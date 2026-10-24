package main

import (
	"encoding/binary"
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


func TestEncodeMobilePacketPreservesTileAndLinkHitbox(t *testing.T) {
	tiles := [][]byte{[]byte("0123456789jpeg-data")}
	links := []pageLink{{Text: "Open", URL: "https://example.com/", X: 12, Y: 345, W: 67, H: 23}}
	packet, err := encodeMobilePacket(tiles, links)
	if err != nil {
		t.Fatalf("encodeMobilePacket: %v", err)
	}
	in := bytes.NewReader(packet)
	magic := make([]byte, 4)
	if _, err := io.ReadFull(in, magic); err != nil {
		t.Fatal(err)
	}
	if string(magic) != imglMagic {
		t.Fatalf("magic = %q, want %q", magic, imglMagic)
	}
	var tileCount uint16
	if err := binary.Read(in, binary.BigEndian, &tileCount); err != nil {
		t.Fatal(err)
	}
	if tileCount != 1 {
		t.Fatalf("tile count = %d, want 1", tileCount)
	}
	var tileLen uint32
	if err := binary.Read(in, binary.BigEndian, &tileLen); err != nil {
		t.Fatal(err)
	}
	if int(tileLen) != len(tiles[0]) {
		t.Fatalf("tile length = %d, want %d", tileLen, len(tiles[0]))
	}
	tile := make([]byte, tileLen)
	if _, err := io.ReadFull(in, tile); err != nil {
		t.Fatal(err)
	}
	if !bytes.Equal(tile, tiles[0]) {
		t.Fatal("tile bytes changed during encoding")
	}
	var linkCount uint16
	if err := binary.Read(in, binary.BigEndian, &linkCount); err != nil {
		t.Fatal(err)
	}
	if linkCount != 1 {
		t.Fatalf("link count = %d, want 1", linkCount)
	}
	readString := func() string {
		t.Helper()
		var n uint16
		if err := binary.Read(in, binary.BigEndian, &n); err != nil {
			t.Fatal(err)
		}
		b := make([]byte, n)
		if _, err := io.ReadFull(in, b); err != nil {
			t.Fatal(err)
		}
		return string(b)
	}
	if got := readString(); got != links[0].Text {
		t.Fatalf("link text = %q", got)
	}
	if got := readString(); got != links[0].URL {
		t.Fatalf("link URL = %q", got)
	}
	want := []uint16{12, 345, 67, 23}
	for i, expected := range want {
		var got uint16
		if err := binary.Read(in, binary.BigEndian, &got); err != nil {
			t.Fatal(err)
		}
		if got != expected {
			t.Fatalf("hitbox coordinate %d = %d, want %d", i, got, expected)
		}
	}
	if in.Len() != 0 {
		t.Fatalf("unexpected %d trailing bytes", in.Len())
	}
}

func TestEncodeMobilePacketRejectsInvalidTileCount(t *testing.T) {
	if _, err := encodeMobilePacket(nil, nil); err == nil {
		t.Fatal("expected empty tile list to be rejected")
	}
}

func TestEncodeMobilePacketRejectsOversizedPacket(t *testing.T) {
	tile := make([]byte, maxMobilePacketBytes+1)
	if _, err := encodeMobilePacket([][]byte{tile}, nil); err == nil {
		t.Fatal("expected oversized tile packet to be rejected")
	}
}
