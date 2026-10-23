package main

import (
	"net/http"
	"net/http/httptest"
	"net/url"
	"testing"
)

func TestValidateRemoteTargetRejectsUnsafeTargets(t *testing.T) {
	cases := []string{
		"http://127.0.0.1/",
		"http://localhost/",
		"http://10.0.0.1/",
		"http://169.254.169.254/latest/meta-data/",
		"http://user:pass@example.com/",
		"file:///etc/passwd",
		"ftp://example.com/",
		"http://192.0.2.1/",
	}
	for _, raw := range cases {
		t.Run(raw, func(t *testing.T) {
			u, err := url.Parse(raw)
			if err != nil {
				t.Fatal(err)
			}
			if err := validateRemoteTarget(t.Context(), u); err == nil {
				t.Fatalf("expected target to be rejected: %s", raw)
			}
		})
	}
}

func TestServeRemoteFetchRejectsPrivateTargets(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, remoteFetchPath+"?url=http%3A%2F%2F127.0.0.1%2F", nil)
	rec := httptest.NewRecorder()
	if !serveRemoteFetch(rec, req) {
		t.Fatal("remote fetch route was not handled")
	}
	if rec.Code != http.StatusBadRequest {
		t.Fatalf("expected 400, got %d", rec.Code)
	}
}

func TestServeRemoteFetchDoesNotCaptureForwardProxyURLs(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "http://example.com/__tgfn/fetch?url=http%3A%2F%2Fexample.org", nil)
	rec := httptest.NewRecorder()
	if serveRemoteFetch(rec, req) {
		t.Fatal("absolute forward-proxy request was incorrectly captured as remote fetch")
	}
}
