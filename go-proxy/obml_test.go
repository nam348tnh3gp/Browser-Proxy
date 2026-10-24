package main

import (
	"bytes"
	"encoding/binary"
	"io"
	"net/http"
	"net/url"
	"strings"
	"testing"
)

func TestBuildOBMLPageCreatesCompactBinaryBlocks(t *testing.T) {
	source := `<!doctype html><html><head><title>Example &amp; Test</title><script>alert(1)</script><style>.x{display:none}</style></head><body><h1>Welcome</h1><p>Read <a href="/next">the next page</a>.</p><img src="/small.png" alt="Small image"><footer>Footer</footer></body></html>`
	packet, err := buildOBMLPage("http://example.test/start", source)
	if err != nil {
		t.Fatal(err)
	}
	if len(packet) < 8 || string(packet[:4]) != obmlMagic {
		t.Fatalf("invalid packet header: %q", packet[:4])
	}
	for _, value := range []string{"Example & Test", "Welcome", "the next page", "http://example.test/next", "Footer", imageCompressPath + "?url=" + url.QueryEscape("http://example.test/small.png")} {
		if !bytes.Contains(packet, []byte(value)) {
			t.Errorf("packet missing %q", value)
		}
	}
	for _, value := range []string{"alert(1)", "display:none"} {
		if bytes.Contains(packet, []byte(value)) {
			t.Errorf("packet retained removed content %q", value)
		}
	}
}

func TestBuildOBMLPageRendersInlineJavaScriptDocumentWrite(t *testing.T) {
	source := `<!doctype html><html><head><title>JS page</title></head><body><script>document.write('<h1>Rendered heading</h1><p><input name="q" value="hello"></p>')</script><p>After script</p></body></html>`
	packet, err := buildOBMLPage("https://example.test/", source)
	if err != nil {
		t.Fatal(err)
	}
	for _, want := range []string{"Rendered heading", "hello", "After script"} {
		if !bytes.Contains(packet, []byte(want)) {
			t.Errorf("rendered packet missing %q: %q", want, packet)
		}
	}
	if bytes.Contains(packet, []byte("document.write")) {
		t.Fatal("JavaScript source leaked into rendered page")
	}
}

func TestBuildOBMLPageKeepsCSSBannersAndLazyImages(t *testing.T) {
	source := `<!doctype html><html><head><style>.hero { background-image: url('/banner.jpg'); }</style></head><body><div class="hero"><a href="/home"><img data-src="/logo.png" alt="Site logo"></a></div><p>After banner</p></body></html>`
	packet, err := buildOBMLPage("https://example.test/page", source)
	if err != nil {
		t.Fatal(err)
	}
	for _, imageURL := range []string{"https://example.test/banner.jpg", "https://example.test/logo.png"} {
		want := imageCompressPath + "?url=" + url.QueryEscape(imageURL)
		if !bytes.Contains(packet, []byte(want)) {
			t.Errorf("compact page omitted image %q: %q", imageURL, packet)
		}
	}
	if !bytes.Contains(packet, []byte("Site logo")) || !bytes.Contains(packet, []byte("https://example.test/home")) {
		t.Fatalf("linked logo or its target was lost: %q", packet)
	}
	if bytes.Index(packet, []byte("banner.jpg")) > bytes.Index(packet, []byte("After banner")) {
		t.Fatal("banner background should appear before the following page content")
	}
}

func TestBuildOBMLPageCoalescesInlineTextForMobileReflow(t *testing.T) {
	source := `<!doctype html><html><body><p>one <span>two</span> three</p></body></html>`
	packet, err := buildOBMLPage("https://example.test/", source)
	if err != nil {
		t.Fatal(err)
	}
	if !bytes.Contains(packet, []byte("one two three")) {
		t.Fatalf("inline text was split into separate blocks; packet=%q", packet)
	}
}

func TestBuildOBMLPageEncodesSelectAndCheckedControls(t *testing.T) {
	source := `<!doctype html><html><body><form aria-label="Search" action="/search"><input name="safe" type="checkbox" value="1" checked><select name="lang"><option value="en">English</option><option value="vi" selected>Vietnamese</option></select></form></body></html>`
	packet, err := buildOBMLPage("https://example.test/", source)
	if err != nil {
		t.Fatal(err)
	}
	encoded := string(packet)
	if !strings.Contains(encoded, "checkbox_checked") {
		t.Fatal("checked checkbox state was not encoded")
	}
	if !strings.Contains(encoded, "vi\x1eEnglish\x1den\x1eVietnamese\x1dvi") {
		t.Fatalf("select options were not encoded with selected value: %q", packet)
	}
}

func TestRenderInlineJavaScriptDocumentWrite(t *testing.T) {
	source := `<!doctype html><html><body><h1>Before</h1><script>document.write('<p id="generated">Generated <b>mobile</b> content</p>')</script><p>After</p></body></html>`
	rendered := renderInlineJavaScript("https://example.test/page", source)
	if !strings.Contains(rendered, `<p id="generated">Generated <b>mobile</b> content</p>`) {
		t.Fatalf("document.write output missing: %q", rendered)
	}
	if strings.Contains(rendered, "<script") {
		t.Fatalf("inline script tag should not reach the compact renderer: %q", rendered)
	}
	packet, err := buildOBMLPage("https://example.test/page", rendered)
	if err != nil {
		t.Fatal(err)
	}
	for _, want := range []string{"Before", "Generated", "mobile", "After"} {
		if !bytes.Contains(packet, []byte(want)) {
			t.Errorf("TBO1 packet missing rendered JavaScript content %q", want)
		}
	}
}

func TestRenderInlineJavaScriptSkipsExternalScripts(t *testing.T) {
	source := `<html><body><script src="https://example.test/app.js"></script><p>Static</p></body></html>`
	rendered := renderInlineJavaScript("https://example.test/page", source)
	if strings.Contains(rendered, "app.js") || !strings.Contains(rendered, "Static") {
		t.Fatalf("external script should be skipped without damaging static HTML: %q", rendered)
	}
}

func TestOBMLResponseOnlyTransformsOptedInHTML(t *testing.T) {
	body := "<html><head><title>Example</title></head><body><p>Hello</p></body></html>"
	req, _ := http.NewRequest("GET", "http://example.test/page", nil)
	req.Header.Set("X-TGFN-Format", "TBO1")
	resp := &http.Response{
		StatusCode:    200,
		Header:        http.Header{"Content-Type": []string{"text/html; charset=utf-8"}},
		Body:          io.NopCloser(strings.NewReader(body)),
		Request:       req,
		ContentLength: int64(len(body)),
	}
	if err := compactHTMLResponse(resp); err != nil {
		t.Fatal(err)
	}
	packet, err := io.ReadAll(resp.Body)
	if err != nil {
		t.Fatal(err)
	}
	if resp.Header.Get("Content-Type") != "application/x-tgfn-obml; version=1" {
		t.Fatalf("unexpected content type: %s", resp.Header.Get("Content-Type"))
	}
	if len(packet) < 4 || string(packet[:4]) != obmlMagic {
		t.Fatalf("unexpected packet: %q", packet[:4])
	}
	if resp.ContentLength != int64(len(packet)) {
		t.Fatalf("content length mismatch: %d != %d", resp.ContentLength, len(packet))
	}
}

func TestOBMLResponseLeavesOtherClientsUnchanged(t *testing.T) {
	body := "<html><body><p>Hello</p></body></html>"
	req, _ := http.NewRequest("GET", "http://example.test/page", nil)
	resp := &http.Response{
		StatusCode:    200,
		Header:        http.Header{"Content-Type": []string{"text/html"}},
		Body:          io.NopCloser(strings.NewReader(body)),
		Request:       req,
		ContentLength: int64(len(body)),
	}
	if err := compactHTMLResponse(resp); err != nil {
		t.Fatal(err)
	}
	got, err := io.ReadAll(resp.Body)
	if err != nil {
		t.Fatal(err)
	}
	if string(got) != body {
		t.Fatalf("unrequested response was modified: %q", got)
	}
}

func TestBuildOBMLPageCapsBlocksAtMIDPLimit(t *testing.T) {
	pageURL := "http://example.test/"
	source := "<html><body>" + strings.Repeat("<p>block</p>", maxOBMLBlocks*2) + "</body></html>"
	packet, err := buildOBMLPage(pageURL, source)
	if err != nil {
		t.Fatal(err)
	}
	countOffset := 4 + 2 + len(pageURL)
	if len(packet) < countOffset+2 {
		t.Fatal("compact packet is truncated before block count")
	}
	count := int(binary.BigEndian.Uint16(packet[countOffset : countOffset+2]))
	if count > 1100 {
		t.Fatalf("server emitted %d blocks; MIDP client limit is 1100", count)
	}
	if count != maxOBMLBlocks {
		t.Fatalf("expected block cap %d, got %d", maxOBMLBlocks, count)
	}
}
