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
