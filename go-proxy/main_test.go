package main

import (
	"compress/gzip"
	"encoding/base64"
	"io"
	"net/http"
	"net/http/httptest"
	"strings"
	"testing"
)

func TestGzipTextResponse(t *testing.T) {
	body := strings.Repeat("html content repeated for compression ", 100)
	req := httptest.NewRequest(http.MethodGet, "http://example.test/", nil)
	req.Header.Set("Accept-Encoding", "gzip")
	resp := &http.Response{
		StatusCode:    http.StatusOK,
		Header:        http.Header{"Content-Type": []string{"text/html; charset=utf-8"}},
		Body:          io.NopCloser(strings.NewReader(body)),
		Request:       req,
		ContentLength: int64(len(body)),
	}
	if err := gzipTextResponse(resp); err != nil {
		t.Fatal(err)
	}
	if resp.Header.Get("Content-Encoding") != "gzip" {
		t.Fatalf("Content-Encoding = %q, want gzip", resp.Header.Get("Content-Encoding"))
	}
	if !strings.Contains(strings.Join(resp.Header.Values("Vary"), ","), "Accept-Encoding") {
		t.Fatal("Vary must include Accept-Encoding")
	}
	reader, err := gzip.NewReader(resp.Body)
	if err != nil {
		t.Fatal(err)
	}
	decoded, err := io.ReadAll(reader)
	if err != nil {
		t.Fatal(err)
	}
	if string(decoded) != body {
		t.Fatal("decompressed response differs from original")
	}
}

func TestGzipTextResponseSkipsUnsafeResponses(t *testing.T) {
	cases := []struct {
		name        string
		accept      string
		contentType string
		encoding    string
		status      int
		rangeHeader string
		wantGzip    bool
	}{
		{name: "client does not accept gzip", accept: "identity", contentType: "text/html", status: http.StatusOK},
		{name: "already compressed", accept: "gzip", contentType: "text/html", encoding: "br", status: http.StatusOK},
		{name: "binary image", accept: "gzip", contentType: "image/png", status: http.StatusOK},
		{name: "partial response", accept: "gzip", contentType: "text/html", status: http.StatusPartialContent, rangeHeader: "bytes=0-10"},
		{name: "gzip refused by quality", accept: "gzip;q=0", contentType: "text/html", status: http.StatusOK},
		{name: "compress text", accept: "gzip", contentType: "text/html", status: http.StatusOK, wantGzip: true},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			req := httptest.NewRequest(http.MethodGet, "http://example.test/", nil)
			req.Header.Set("Accept-Encoding", tc.accept)
			if tc.rangeHeader != "" {
				req.Header.Set("Range", tc.rangeHeader)
			}
			header := http.Header{"Content-Type": []string{tc.contentType}}
			if tc.encoding != "" {
				header.Set("Content-Encoding", tc.encoding)
			}
			resp := &http.Response{
				StatusCode: tc.status,
				Header:     header,
				Body:       io.NopCloser(strings.NewReader(strings.Repeat("x", 200))),
				Request:    req,
			}
			if err := gzipTextResponse(resp); err != nil {
				t.Fatal(err)
			}
			got := resp.Header.Get("Content-Encoding") == "gzip"
			if got != tc.wantGzip {
				t.Fatalf("gzip = %v, want %v", got, tc.wantGzip)
			}
		})
	}
}

func TestValidProxyAuthorization(t *testing.T) {
	good := "Basic " + base64.StdEncoding.EncodeToString([]byte("user:secret"))
	cases := []struct {
		name   string
		header string
		want   bool
	}{
		{name: "correct credentials", header: good, want: true},
		{name: "missing header", header: "", want: false},
		{name: "wrong password", header: "Basic " + base64.StdEncoding.EncodeToString([]byte("user:wrong")), want: false},
		{name: "wrong username", header: "Basic " + base64.StdEncoding.EncodeToString([]byte("other:secret")), want: false},
		{name: "malformed base64", header: "Basic !!!", want: false},
		{name: "wrong scheme", header: "Bearer secret", want: false},
		{name: "missing separator", header: "Basic " + base64.StdEncoding.EncodeToString([]byte("usersecret")), want: false},
	}
	for _, tc := range cases {
		t.Run(tc.name, func(t *testing.T) {
			if got := validProxyAuthorization(tc.header, "user", "secret"); got != tc.want {
				t.Fatalf("validProxyAuthorization() = %v, want %v", got, tc.want)
			}
		})
	}
}

func TestProxyAuthRequiredForHTTPAndCONNECT(t *testing.T) {
	handler := proxyHandlerWithAuth("user", "secret")
	for _, target := range []struct {
		name   string
		method string
		url    string
		host   string
	}{
		{name: "HTTP", method: http.MethodGet, url: "http://example.test/"},
		{name: "CONNECT", method: http.MethodConnect, url: "http://example.test:443", host: "example.test:443"},
	} {
		t.Run(target.name, func(t *testing.T) {
			req := httptest.NewRequest(target.method, target.url, nil)
			if target.host != "" {
				req.Host = target.host
			}
			rec := httptest.NewRecorder()
			handler.ServeHTTP(rec, req)
			if rec.Code != http.StatusProxyAuthRequired {
				t.Fatalf("status = %d, want %d", rec.Code, http.StatusProxyAuthRequired)
			}
			if got := rec.Header().Get("Proxy-Authenticate"); got == "" {
				t.Fatal("missing Proxy-Authenticate challenge")
			}
		})
	}
}

func TestProxyAcceptsCorrectCredentialsBeforeTargetValidation(t *testing.T) {
	handler := proxyHandlerWithAuth("user", "secret")
	req := httptest.NewRequest(http.MethodGet, "/not-an-absolute-proxy-url", nil)
	req.Header.Set("Proxy-Authorization", "Basic "+base64.StdEncoding.EncodeToString([]byte("user:secret")))
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("status = %d, want %d; correct credentials should pass authentication", rec.Code, http.StatusBadRequest)
	}
}


func TestStatsEndpointReturnsMeasuredCounters(t *testing.T) {
	handler := proxyHandlerWithAuth("", "")
	req := httptest.NewRequest(http.MethodGet, "/__tgfn/stats", nil)
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)

	if rec.Code != http.StatusOK {
		t.Fatalf("stats endpoint status = %d, want %d; body=%s", rec.Code, http.StatusOK, rec.Body.String())
	}
	if got := rec.Header().Get("Cache-Control"); got != "no-store" {
		t.Fatalf("Cache-Control = %q, want no-store", got)
	}
	for _, key := range []string{
		"process_started_at",
		"http_requests",
		"http_response_bytes",
		"gzip_responses",
		"gzip_original_bytes",
		"gzip_compressed_bytes",
		"gzip_saved_bytes",
		"connect_tunnels",
		"connect_client_to_upstream_bytes",
		"connect_upstream_to_client_bytes",
	} {
		if !strings.Contains(rec.Body.String(), `"` + key + `"`) {
			t.Errorf("stats response missing key %q: %s", key, rec.Body.String())
		}
	}
}
