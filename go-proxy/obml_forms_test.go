package main

import (
	"bytes"
	"testing"
)

func TestBuildOBMLPagePreservesFormsAndLayout(t *testing.T) {
	source := "<html><head><title>Search</title></head><body>" +
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
	for _, want := range []string{"Cell A", "Cell B", "Site search", "token", "abc123", "Search the web", "https://example.test/search"} {
		if !bytes.Contains(packet, []byte(want)) {
			t.Errorf("packet missing %q", want)
		}
	}
	if !bytes.Contains(packet, []byte{7}) {
		t.Fatal("form block kind missing")
	}
}
