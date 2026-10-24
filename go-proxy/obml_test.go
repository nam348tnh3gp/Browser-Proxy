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
	for _, value := range []string{"Example & Test", "Welcome", "the next page", "http://example.test/next", imageCompressPath + "?url=" + url.QueryEscape("http://example.test/small.png")} {
		if !bytes.Contains(packet, []byte(value)) {
			t.Errorf("packet missing %q", value)
		}
	}
	for _, value := range []string{"alert(1)", "display:none", "Footer"} {
		if bytes.Contains(packet, []byte(value)) {
			t.Errorf("packet retained removed content %q", value)
		}
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
