package main

import (
	"io"
	"net/http"
	"strings"
	"testing"
)

func TestCompactHTMLRemovesHeavyContentAndKeepsLinks(t *testing.T) {
	input := "<!doctype html><html><head><title>Example</title><script>alert(1)</script><link rel=\"stylesheet\" href=\"/style.css\"></head><body><nav>Menu</nav><h1 class=\"hero\">Hello</h1><p onclick=\"bad()\">Read <a href=\"/next\" onclick=\"bad()\">next page</a></p><img src=\"/a.png\" alt=\"A\"><style>body{display:none}</style></body></html>"
	got := compactHTML(input)
	for _, want := range []string{"<title>Example</title>", "<h1>Hello</h1>", "<a href=\"/next\">next page</a>", "<img src=\"/a.png\" alt=\"A\">"} {
		if !strings.Contains(got, want) {
			t.Errorf("compactHTML() missing %q in %q", want, got)
		}
	}
	for _, unwanted := range []string{"alert(1)", "display:none", "onclick", "stylesheet", "Menu"} {
		if strings.Contains(got, unwanted) {
			t.Errorf("compactHTML() retained unwanted content %q in %q", unwanted, got)
		}
	}
}

func TestCompactHTMLResponseRequiresOptIn(t *testing.T) {
	body := "<html><body><script>x()</script><p>Hello</p></body></html>"
	req, _ := http.NewRequest("GET", "http://example.test/", nil)
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
	got, _ := io.ReadAll(resp.Body)
	if string(got) != body {
		t.Fatalf("response changed without opt-in: %q", got)
	}
}

func TestCompactHTMLResponseTransformsOptInHTML(t *testing.T) {
	body := "<html><body><script>x()</script><p>Hello</p></body></html>"
	req, _ := http.NewRequest("GET", "http://example.test/", nil)
	req.Header.Set("X-TGFN-Compact", "1")
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
	got, _ := io.ReadAll(resp.Body)
	if strings.Contains(string(got), "x()") || !strings.Contains(string(got), "Hello") {
		t.Fatalf("unexpected compact response: %q", got)
	}
	if resp.ContentLength != int64(len(got)) {
		t.Fatalf("content length = %d, body = %d", resp.ContentLength, len(got))
	}
}

func TestTBO1ReturnsCompactBinaryNotHTML(t *testing.T) {
	body := "<!doctype html><html><head><title>Binary example</title><style>p { color: red; }</style></head><body><h1>Hello</h1><p>Read <a href='/next'>next page</a></p></body></html>"
	req, _ := http.NewRequest("GET", "http://example.test/", nil)
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
	got, err := io.ReadAll(resp.Body)
	if err != nil {
		t.Fatal(err)
	}
	if len(got) < 8 || string(got[:4]) != obmlMagic {
		t.Fatalf("TBO1 must return compact binary with %q magic, got content-type=%q body=%q",
			obmlMagic, resp.Header.Get("Content-Type"), got[:min(len(got), 80)])
	}
	if strings.Contains(string(got), "<html") || strings.Contains(string(got), "<style") {
		t.Fatalf("TBO1 response leaked HTML instead of binary: %q", got[:min(len(got), 80)])
	}
	if resp.Header.Get("Content-Type") != "application/x-tgfn-obml; version=1" {
		t.Errorf("content type = %q", resp.Header.Get("Content-Type"))
	}
	if resp.ContentLength != int64(len(got)) {
		t.Errorf("content length = %d, body = %d", resp.ContentLength, len(got))
	}
}
