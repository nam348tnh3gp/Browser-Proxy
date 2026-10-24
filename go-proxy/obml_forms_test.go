package main

import (
	"bytes"
	"io"
	"net/http"
	"strings"
	"testing"
)

func TestBuildOBMLPagePreservesFormsAndLayout(t *testing.T) {
	source := "<html><head><title>Search</title></head><body>" +
		"<header>Site Header</header><nav><a href=\"/home\">Home</a></nav>" +
		"<div style=\"display:none\">CSS-HIDDEN-SENTINEL</div>" +
		"<table><tr><td>Cell A</td><td>Cell B</td></tr></table>" +
		"<form action=\"/search\" method=\"get\" aria-label=\"Site search\">" +
		"<input type=\"hidden\" name=\"token\" value=\"abc123\">" +
		"<input type=\"search\" name=\"q\" placeholder=\"Search the web\">" +
		"<input type=\"password\" name=\"pass\">" +
		"<button type=\"submit\">Search</button></form></body></html>"
	packet, err := buildOBMLPage("https://example.test/", source)
	if err != nil {
		t.Fatal(err)
	}
	if bytes.Contains(packet, []byte("CSS-HIDDEN-SENTINEL")) {
		t.Fatal("hidden CSS content leaked into page")
	}
	for _, want := range []string{"Site Header", "Home", "Cell A", "Cell B", "|", "Site search", "token", "abc123", "Search the web", "https://example.test/search"} {
		if !bytes.Contains(packet, []byte(want)) {
			t.Errorf("packet missing %q", want)
		}
	}
	if !bytes.Contains(packet, []byte{7}) {
		t.Fatal("form block kind missing")
	}
}

func TestMobileResponseFallsBackToCompactSemanticPage(t *testing.T) {
	body := "<html><head><title>Mobile fallback</title></head><body><h1>Hello mobile</h1><a href='/next'>Next page</a></body></html>"
	req, err := http.NewRequest(http.MethodGet, "https://example.test/start", nil)
	if err != nil {
		t.Fatal(err)
	}
	req.Header.Set("X-TGFN-Format", "MOB1")
	resp := &http.Response{
		StatusCode:    http.StatusOK,
		Header:        http.Header{"Content-Type": []string{"text/html; charset=utf-8"}},
		Body:          io.NopCloser(strings.NewReader(body)),
		Request:       req,
		ContentLength: int64(len(body)),
	}
	if err := mobileResponseWithChrome(resp, ""); err != nil {
		t.Fatal(err)
	}
	if got := resp.Header.Get("Content-Type"); got != "application/x-tgfn-obml; version=1" {
		t.Fatalf("fallback content type = %q, want compact TBO1", got)
	}
	packet, err := io.ReadAll(resp.Body)
	if err != nil {
		t.Fatal(err)
	}
	for _, want := range []string{"Mobile fallback", "Hello mobile", "https://example.test/next"} {
		if !bytes.Contains(packet, []byte(want)) {
			t.Errorf("fallback packet missing %q", want)
		}
	}
	if got := req.Header.Get("X-TGFN-Format"); got != "MOB1" {
		t.Errorf("request format header mutated to %q", got)
	}
}
