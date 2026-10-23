package main

import (
	"encoding/base64"
	"net/http"
	"net/http/httptest"
	"testing"
)

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
