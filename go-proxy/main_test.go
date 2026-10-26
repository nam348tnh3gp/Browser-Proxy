package main

import (
	"compress/gzip"
	"encoding/base64"
	"encoding/json"
	"io"
	"net/http"
	"net/http/httptest"
	"os"
	"path/filepath"
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
		if !strings.Contains(rec.Body.String(), `"`+key+`"`) {
			t.Errorf("stats response missing key %q: %s", key, rec.Body.String())
		}
	}
}

func TestRegisterClientAndReadPerClientStats(t *testing.T) {
	const id = "test-client-1234"
	if !registerClient("192.0.2.10:54321", id) {
		t.Fatal("registerClient rejected a valid ID and remote address")
	}
	if got := clientIDFromRemoteAddr("192.0.2.10:60000"); got != id {
		t.Fatalf("clientIDFromRemoteAddr() = %q, want %q", got, id)
	}
	counters := clientCounters(id)
	counters.httpRequests.Add(3)
	counters.httpResponseBytes.Add(1024)
	counters.gzipOriginalBytes.Add(900)
	counters.gzipCompressedBytes.Add(300)
	counters.gzipResponses.Add(1)
	counters.connectTunnels.Add(2)
	counters.connectUpstreamToClient.Add(2048)

	handler := proxyHandlerWithAuth("", "")
	req := httptest.NewRequest(http.MethodGet, "/__tgfn/stats?client_id="+id, nil)
	req.RemoteAddr = "192.0.2.10:54321"
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("status = %d, want %d", rec.Code, http.StatusOK)
	}
	body := rec.Body.String()
	for _, expected := range []string{
		`"client_id":"test-client-1234"`,
		`"http_requests":3`,
		`"http_response_bytes":1024`,
		`"gzip_saved_bytes":600`,
		`"connect_tunnels":2`,
		`"registered_clients":1`,
		`"client_attribution_status":"matched_source_ip"`,
	} {
		if !strings.Contains(body, expected) {
			t.Errorf("per-client stats missing %s: %s", expected, body)
		}
	}
}

func TestRegisterClientRejectsInvalidIDs(t *testing.T) {
	for _, id := range []string{"", "short", "bad/id/with/slash", strings.Repeat("a", 81)} {
		if registerClient("192.0.2.20:1234", id) {
			t.Errorf("registerClient unexpectedly accepted ID %q", id)
		}
	}
	if registerClient("invalid-remote-address", "valid-client-123") {
		t.Fatal("registerClient accepted an invalid remote address")
	}
}

// resetClientTrackingForTest keeps global counter tests independent.
func resetClientTrackingForTest() {
	clientTracking.Lock()
	clientTracking.idByIP = make(map[string]string)
	clientTracking.ambiguousIPs = make(map[string]bool)
	clientTracking.registeredIDs = make(map[string]struct{})
	clientTracking.stats = make(map[string]*clientCounterSet)
	clientTracking.Unlock()
}

func TestPerClientCountersFollowSingleRegisteredDeviceAcrossAddressChanges(t *testing.T) {
	resetClientTrackingForTest()
	const id = "main-device-1234"
	if !registerClient("192.0.2.10:4321", id) {
		t.Fatal("registerClient failed")
	}

	// Registration and WebView traffic can arrive over different local/interface
	// addresses; mapped IPv4/IPv6 forms should also resolve to the same client.
	if got := clientIDFromRemoteAddr("[::ffff:192.0.2.10]:5000"); got != id {
		t.Fatalf("mapped IPv4 attribution = %q, want %q", got, id)
	}
	if got := clientIDFromRemoteAddr("198.51.100.9:5000"); got != id {
		t.Fatalf("single-client fallback = %q, want %q", got, id)
	}

	body := strings.Repeat("device counter body ", 20)
	upstream := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "text/plain; charset=utf-8")
		_, _ = io.WriteString(w, body)
	}))
	defer upstream.Close()

	handler := proxyHandlerWithAuth("", "")
	beforeNetworkRequests := proxyStats.httpRequests.Load()
	beforeNetworkBytes := proxyStats.httpResponseBytes.Load()
	beforeClient := lookupClientCounters(id)
	beforeClientRequests := beforeClient.httpRequests.Load()
	beforeClientBytes := beforeClient.httpResponseBytes.Load()

	req := httptest.NewRequest(http.MethodGet, upstream.URL, nil)
	req.RemoteAddr = "198.51.100.9:6000" // differs from registration IP
	req.Header.Set("Accept-Encoding", "identity")
	rec := httptest.NewRecorder()
	handler.ServeHTTP(rec, req)
	if rec.Code != http.StatusOK {
		t.Fatalf("proxy response status = %d, body=%s", rec.Code, rec.Body.String())
	}
	if rec.Body.String() != body {
		t.Fatal("proxy response body differs from upstream")
	}
	if got := proxyStats.httpRequests.Load() - beforeNetworkRequests; got != 1 {
		t.Fatalf("network HTTP request delta = %d, want 1", got)
	}
	if got := proxyStats.httpResponseBytes.Load() - beforeNetworkBytes; got != int64(len(body)) {
		t.Fatalf("network response-byte delta = %d, want %d", got, len(body))
	}
	if got := beforeClient.httpRequests.Load() - beforeClientRequests; got != 1 {
		t.Fatalf("device HTTP request delta = %d, want 1", got)
	}
	if got := beforeClient.httpResponseBytes.Load() - beforeClientBytes; got != int64(len(body)) {
		t.Fatalf("device response-byte delta = %d, want %d", got, len(body))
	}
}

func TestSharedSourceIPIsMarkedAmbiguousInsteadOfMisattributed(t *testing.T) {
	resetClientTrackingForTest()
	if !registerClient("203.0.113.7:1001", "device-client-1111") {
		t.Fatal("first registration failed")
	}
	if !registerClient("203.0.113.7:1002", "device-client-2222") {
		t.Fatal("second registration failed")
	}
	if got := clientIDFromRemoteAddr("203.0.113.7:6000"); got != "" {
		t.Fatalf("shared-IP attribution = %q, want ambiguous/empty", got)
	}
	if got := snapshotCounters(&proxyStats).RegisteredClients; got != 2 {
		t.Fatalf("registered client count = %d, want 2", got)
	}

	// A caller must not be able to inflate device counters by inventing an ID
	// in X-TGFN-Client-ID; an unregistered ID falls back to the ambiguous source.
	req := httptest.NewRequest(http.MethodGet, "http://example.test/", nil)
	req.RemoteAddr = "203.0.113.7:6000"
	req.Header.Set("X-TGFN-Client-ID", "spoofed-device-9999")
	if got := clientIDFromRequest(req); got != "" {
		t.Fatalf("unregistered header attribution = %q, want empty", got)
	}
	if got := snapshotCounters(&proxyStats).RegisteredClients; got != 2 {
		t.Fatalf("spoofed header changed registered client count to %d", got)
	}
}

func TestReRegisteringClientRemovesStaleIPMapping(t *testing.T) {
	resetClientTrackingForTest()
	if !registerClient("192.0.2.21:1000", "moving-device-1111") {
		t.Fatal("initial registration failed")
	}
	if !registerClient("192.0.2.22:1000", "other-device-2222") {
		t.Fatal("second registration failed")
	}
	if !registerClient("192.0.2.23:1000", "moving-device-1111") {
		t.Fatal("updated registration failed")
	}
	if got := clientIDFromRemoteAddr("192.0.2.21:5000"); got != "" {
		t.Fatalf("old IP still maps to moved client %q; want no attribution", got)
	}
	if got := clientIDFromRemoteAddr("192.0.2.23:5000"); got != "moving-device-1111" {
		t.Fatalf("new IP maps to %q; want moving-device-1111", got)
	}
}


func TestSavePersistedStatsWritesJSON(t *testing.T) {
	path := filepath.Join(t.TempDir(), "proxy-stats.json")
	t.Setenv("PROXY_STATS_FILE", path)

	if err := savePersistedStats(); err != nil {
		t.Fatalf("savePersistedStats() error = %v", err)
	}
	data, err := os.ReadFile(path)
	if err != nil {
		t.Fatalf("read saved stats: %v", err)
	}
	var saved persistedStatsFile
	if err := json.Unmarshal(data, &saved); err != nil {
		t.Fatalf("saved stats is not valid JSON: %v", err)
	}
	if saved.SavedAt == "" || saved.Clients == nil || saved.IDByIP == nil {
		t.Fatalf("saved stats missing required fields: %+v", saved)
	}
}

func TestPersistStatsRetriesAfterWriteFailure(t *testing.T) {
	path := filepath.Join(t.TempDir(), "missing-directory", "proxy-stats.json")
	t.Setenv("PROXY_STATS_FILE", path)
	statsDirty.Store(true)
	t.Cleanup(func() { statsDirty.Store(false) })

	persistStatsIfDirty()
	if !statsDirty.Load() {
		t.Fatal("statsDirty was cleared after a failed save; next retry would be skipped")
	}
}

func TestSavePersistedStatsReportsRenameFailure(t *testing.T) {
	dir := t.TempDir()
	path := filepath.Join(dir, "stats-target")
	if err := os.Mkdir(path, 0o700); err != nil {
		t.Fatal(err)
	}
	t.Setenv("PROXY_STATS_FILE", path)

	if err := savePersistedStats(); err == nil {
		t.Fatal("savePersistedStats() succeeded despite rename target being a directory")
	}
}
