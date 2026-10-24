package main

import (
	"bufio"
	"bytes"
	"compress/gzip"
	"context"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"encoding/json"
	"errors"
	"io"
	"log"
	"mime"
	"net"
	"net/http"
	"net/http/httputil"
	"os"
	"strconv"
	"strings"
	"sync"
	"sync/atomic"
	"time"
)

type clientCounterSet struct {
	httpRequests            atomic.Int64
	httpResponseBytes       atomic.Int64
	gzipResponses           atomic.Int64
	gzipOriginalBytes       atomic.Int64
	gzipCompressedBytes     atomic.Int64
	connectTunnels          atomic.Int64
	connectClientToUpstream atomic.Int64
	connectUpstreamToClient atomic.Int64
}

type clientContextKey struct{}

var (
	proxyProcessStartedAt = time.Now().UTC().Format(time.RFC3339Nano)
	clientTracking        struct {
		sync.RWMutex
		idByIP map[string]string
		stats  map[string]*clientCounterSet
	}
	proxyStats            clientCounterSet
)

type proxyStatsSnapshot struct {
	ProcessStartedAt        string `json:"process_started_at"`
	HTTPRequests            int64  `json:"http_requests"`
	HTTPResponseBytes       int64  `json:"http_response_bytes"`
	GzipResponses           int64  `json:"gzip_responses"`
	GzipOriginalBytes       int64  `json:"gzip_original_bytes"`
	GzipCompressedBytes     int64  `json:"gzip_compressed_bytes"`
	GzipSavedBytes          int64  `json:"gzip_saved_bytes"`
	ConnectTunnels          int64  `json:"connect_tunnels"`
	ConnectClientToUpstream int64  `json:"connect_client_to_upstream_bytes"`
	ConnectUpstreamToClient int64  `json:"connect_upstream_to_client_bytes"`
	RegisteredClients       int    `json:"registered_clients"`
	ClientID                string `json:"client_id,omitempty"`
}

func currentProxyStats() proxyStatsSnapshot {
	return snapshotCounters(&proxyStats)
}

func snapshotCounters(c *clientCounterSet) proxyStatsSnapshot {
	original := c.gzipOriginalBytes.Load()
	clientTracking.RLock()
	registeredClients := len(clientTracking.stats)
	clientTracking.RUnlock()
	compressed := proxyStats.gzipCompressedBytes.Load()
	return proxyStatsSnapshot{
		ProcessStartedAt:        proxyProcessStartedAt,
		HTTPRequests:            c.httpRequests.Load(),
		HTTPResponseBytes:       c.httpResponseBytes.Load(),
		GzipResponses:           c.gzipResponses.Load(),
		GzipOriginalBytes:       original,
		GzipCompressedBytes:     compressed,
		GzipSavedBytes:          maxInt64(original-compressed, 0),
		ConnectTunnels:          c.connectTunnels.Load(),
		ConnectClientToUpstream: c.connectClientToUpstream.Load(),
		ConnectUpstreamToClient: c.connectUpstreamToClient.Load(),
		RegisteredClients:       registeredClients,
	}
}

func clientCounters(id string) *clientCounterSet {
	if id == "" {
		return nil
	}
	clientTracking.Lock()
	defer clientTracking.Unlock()
	if clientTracking.stats == nil {
		clientTracking.stats = make(map[string]*clientCounterSet)
	}
	if clientTracking.stats[id] == nil {
		clientTracking.stats[id] = &clientCounterSet{}
	}
	return clientTracking.stats[id]
}

func clientIDFromRemoteAddr(remoteAddr string) string {
	host, _, err := net.SplitHostPort(remoteAddr)
	if err != nil {
		return ""
	}
	clientTracking.RLock()
	id := clientTracking.idByIP[host]
	clientTracking.RUnlock()
	return id
}

func registerClient(remoteAddr, id string) bool {
	if len(id) < 8 || len(id) > 80 {
		return false
	}
	for _, r := range id {
		if !(r >= 'a' && r <= 'z' || r >= 'A' && r <= 'Z' || r >= '0' && r <= '9' || r == '-' || r == '_') {
			return false
		}
	}
	host, _, err := net.SplitHostPort(remoteAddr)
	if err != nil || host == "" {
		return false
	}
	clientTracking.Lock()
	defer clientTracking.Unlock()
	if clientTracking.idByIP == nil {
		clientTracking.idByIP = make(map[string]string)
	}
	if clientTracking.stats == nil {
		clientTracking.stats = make(map[string]*clientCounterSet)
	}
	clientTracking.idByIP[host] = id
	if clientTracking.stats[id] == nil {
		clientTracking.stats[id] = &clientCounterSet{}
	}
	return true
}

func clientIDFromRequest(r *http.Request) string {
	if r == nil {
		return ""
	}
	if id := r.Header.Get("X-TGFN-Client-ID"); id != "" {
		return id
	}
	if id, ok := r.Context().Value(clientContextKey{}).(string); ok {
		return id
	}
	return clientIDFromRemoteAddr(r.RemoteAddr)
}

func addClientCounter(id string, update func(*clientCounterSet)) {
	if counters := clientCounters(id); counters != nil {
		update(counters)
	}
}

func maxInt64(a, b int64) int64 {
	if a > b {
		return a
	}
	return b
}

func main() {
	username := os.Getenv("PROXY_USERNAME")
	password := os.Getenv("PROXY_PASSWORD")
	if (username == "") != (password == "") {
		log.Fatal("PROXY_USERNAME and PROXY_PASSWORD must both be set, or both be empty")
	}
	addr := os.Getenv("PROXY_LISTEN")
	if addr == "" {
		addr = "127.0.0.1:8080"
	}
	server := &http.Server{
		Addr:              addr,
		Handler:           proxyHandlerWithAuth(username, password),
		ReadHeaderTimeout: 10 * time.Second,
		IdleTimeout:       90 * time.Second,
	}
	log.Printf("TGFN.BrowserMini Go proxy listening on %s", addr)
	log.Fatal(server.ListenAndServe())
}

type requestTrackingTransport struct {
	base http.RoundTripper
}

func (t *requestTrackingTransport) RoundTrip(req *http.Request) (*http.Response, error) {
	resp, err := t.base.RoundTrip(req)
	if resp != nil {
		resp.Request = req
	}
	return resp, err
}

func proxyHandler() http.Handler {
	return proxyHandlerWithAuth(os.Getenv("PROXY_USERNAME"), os.Getenv("PROXY_PASSWORD"))
}

func proxyHandlerWithAuth(username, password string) http.Handler {
	proxy := &httputil.ReverseProxy{
		Director: func(req *http.Request) {
			req.Header.Del("Proxy-Authorization")
			req.Header.Del("Proxy-Connection")
			req.Header.Set("Via", "1.1 TGFN.BrowserMini")
		},
		FlushInterval: 100 * time.Millisecond,
		ModifyResponse: func(resp *http.Response) error {
			if err := compactHTMLResponse(resp); err != nil {
				return err
			}
			return gzipTextResponse(resp)
		},
		ErrorLog: log.New(os.Stderr, "proxy: ", log.LstdFlags),
		Transport: &requestTrackingTransport{base: &http.Transport{
			Proxy:                 http.ProxyFromEnvironment,
			ForceAttemptHTTP2:     true,
			MaxIdleConns:          100,
			IdleConnTimeout:       90 * time.Second,
			TLSHandshakeTimeout:   10 * time.Second,
			ExpectContinueTimeout: 1 * time.Second,
			DisableCompression:    false,
		}},
	}

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		started := time.Now()
		if username != "" && !validProxyAuthorization(r.Header.Get("Proxy-Authorization"), username, password) {
			w.Header().Set("Proxy-Authenticate", `Basic realm="TGFN.BrowserMini"`)
			http.Error(w, "Proxy authentication required", http.StatusProxyAuthRequired)
			log.Printf("request rejected method=%s client=%q reason=proxy_auth status=%d", r.Method, safeLogValue(r.RemoteAddr), http.StatusProxyAuthRequired)
			return
		}
		// Native app registers one stable ID per installation. Since WebView's
		// proxy API cannot attach a custom header to every subresource/CONNECT,
		// attribution falls back to the source IP registered by this endpoint.
		if r.Method == http.MethodGet && r.URL != nil && !r.URL.IsAbs() && r.URL.Path == "/__tgfn/register" {
			id := r.URL.Query().Get("client_id")
			if !registerClient(r.RemoteAddr, id) {
				http.Error(w, "Invalid client ID or remote address", http.StatusBadRequest)
				return
			}
			w.Header().Set("Content-Type", "application/json; charset=utf-8")
			w.Header().Set("Cache-Control", "no-store")
			_ = json.NewEncoder(w).Encode(map[string]any{"registered": true, "client_id": id})
			return
		}
		// Native app analytics requests use origin-form directly against the proxy
		// listener. Normal forward-proxy requests use absolute-form URLs.
		if r.Method == http.MethodGet && r.URL != nil && !r.URL.IsAbs() && r.URL.Path == "/__tgfn/stats" {
			w.Header().Set("Content-Type", "application/json; charset=utf-8")
			w.Header().Set("Cache-Control", "no-store")
			id := r.URL.Query().Get("client_id")
			stats := currentProxyStats()
			if id != "" {
				if counters := clientCounters(id); counters != nil {
					stats = snapshotCounters(counters)
				} else {
					stats = snapshotCounters(&clientCounterSet{})
				}
				stats.ClientID = id
			}
			if err := json.NewEncoder(w).Encode(stats); err != nil {
				log.Printf("stats response failed: %v", err)
			}
			return
		}
		clientID := clientIDFromRequest(r)
		r = r.WithContext(context.WithValue(r.Context(), clientContextKey{}, clientID))
		if r.Method == http.MethodConnect {
			log.Printf("request method=CONNECT client=%q client_id=%q target=%q", r.RemoteAddr, clientID, safeLogValue(r.Host))
			handleConnect(w, r, clientID)
			return
		}

		proxyStats.httpRequests.Add(1)
		addClientCounter(clientID, func(c *clientCounterSet) { c.httpRequests.Add(1) })
		target := ""
		if r.URL != nil {
			target = r.URL.Host
		}
		logWriter := &statusLoggingWriter{ResponseWriter: w, status: http.StatusOK, clientID: clientID}
		defer func() {
			log.Printf("request method=%s client=%q target=%q status=%d duration=%s",
				r.Method, r.RemoteAddr, safeLogValue(target), logWriter.status, time.Since(started).Round(time.Millisecond))
		}()

		if r.URL == nil || (r.URL.Scheme != "http" && r.URL.Scheme != "https") || r.URL.Host == "" {
			http.Error(logWriter, "Send an absolute HTTP(S) URL to the proxy", http.StatusBadRequest)
			return
		}
		if strings.Contains(r.URL.Host, "@") {
			http.Error(logWriter, "userinfo in URL is not allowed", http.StatusBadRequest)
			return
		}
		r2 := r.Clone(r.Context())
		r2.URL = r.URL
		r2.Host = r.URL.Host
		proxy.ServeHTTP(logWriter, r2)
	})
}

// multiReadCloser preserves the unread upstream body after a bounded read.
type multiReadCloser struct {
	io.Reader
	closer io.Closer
}

func (m *multiReadCloser) Close() error { return m.closer.Close() }

// gzipTextResponse compresses eligible uncompressed text responses only when the
// client explicitly advertises gzip support. HTTPS CONNECT tunnels remain opaque.
func gzipTextResponse(resp *http.Response) error {
	if resp == nil || resp.Request == nil || resp.Body == nil ||
		resp.StatusCode != http.StatusOK || resp.Request.Method != http.MethodGet ||
		resp.Request.Header.Get("Range") != "" ||
		!acceptsGzip(resp.Request.Header.Get("Accept-Encoding")) ||
		resp.Header.Get("Content-Encoding") != "" {
		return nil
	}
	if strings.Contains(strings.ToLower(resp.Header.Get("Cache-Control")), "no-transform") {
		return nil
	}
	mediaType, _, err := mime.ParseMediaType(resp.Header.Get("Content-Type"))
	if err != nil || !isCompressibleText(mediaType) {
		return nil
	}
	const maxBytes = 8 << 20
	originalBody := resp.Body
	data, err := io.ReadAll(io.LimitReader(originalBody, maxBytes+1))
	if err != nil {
		return err
	}
	if len(data) > maxBytes {
		resp.Body = &multiReadCloser{Reader: io.MultiReader(bytes.NewReader(data), originalBody), closer: originalBody}
		return nil
	}
	var compressed bytes.Buffer
	zw := gzip.NewWriter(&compressed)
	if _, err := zw.Write(data); err != nil {
		_ = zw.Close()
		_ = originalBody.Close()
		return err
	}
	if err := zw.Close(); err != nil {
		_ = originalBody.Close()
		return err
	}
	if compressed.Len() >= len(data) {
		_ = originalBody.Close()
		resp.Body = io.NopCloser(bytes.NewReader(data))
		return nil
	}
	_ = originalBody.Close()
	resp.Body = io.NopCloser(bytes.NewReader(compressed.Bytes()))
	resp.ContentLength = int64(compressed.Len())
	resp.Header.Set("Content-Encoding", "gzip")
	resp.Header.Set("Content-Length", strconv.Itoa(compressed.Len()))
	resp.Header.Del("Content-MD5")
	resp.Header.Del("ETag")
	resp.Header.Del("Content-Range")
	addVary(resp.Header, "Accept-Encoding")
	proxyStats.gzipResponses.Add(1)
	proxyStats.gzipOriginalBytes.Add(int64(len(data)))
	proxyStats.gzipCompressedBytes.Add(int64(compressed.Len()))
	clientID := clientIDFromRequest(resp.Request)
	addClientCounter(clientID, func(c *clientCounterSet) {
		c.gzipResponses.Add(1)
		c.gzipOriginalBytes.Add(int64(len(data)))
		c.gzipCompressedBytes.Add(int64(compressed.Len()))
	})
	return nil
}

func acceptsGzip(header string) bool {
	for _, item := range strings.Split(header, ",") {
		parts := strings.Split(strings.TrimSpace(item), ";")
		if !strings.EqualFold(strings.TrimSpace(parts[0]), "gzip") {
			continue
		}
		allowed := true
		for _, parameter := range parts[1:] {
			pair := strings.SplitN(strings.TrimSpace(parameter), "=", 2)
			if len(pair) == 2 && strings.EqualFold(strings.TrimSpace(pair[0]), "q") {
				if q, err := strconv.ParseFloat(strings.TrimSpace(pair[1]), 64); err == nil && q <= 0 {
					allowed = false
				}
			}
		}
		if allowed {
			return true
		}
	}
	return false
}

func isCompressibleText(mediaType string) bool {
	mediaType = strings.ToLower(mediaType)
	return strings.HasPrefix(mediaType, "text/") ||
		mediaType == "application/javascript" ||
		mediaType == "application/x-javascript" ||
		mediaType == "application/json" ||
		mediaType == "application/manifest+json" ||
		mediaType == "application/xml" ||
		mediaType == "application/xhtml+xml" ||
		mediaType == "image/svg+xml"
}

func addVary(header http.Header, value string) {
	for _, existing := range header.Values("Vary") {
		for _, token := range strings.Split(existing, ",") {
			if strings.EqualFold(strings.TrimSpace(token), value) {
				return
			}
		}
	}
	header.Add("Vary", value)
}

func validProxyAuthorization(header, username, password string) bool {
	const prefix = "Basic "
	if !strings.HasPrefix(header, prefix) {
		return false
	}
	decoded, err := base64.StdEncoding.DecodeString(strings.TrimSpace(strings.TrimPrefix(header, prefix)))
	if err != nil {
		return false
	}
	separator := strings.IndexByte(string(decoded), ':')
	if separator < 0 {
		return false
	}
	gotUser := string(decoded[:separator])
	gotPassword := string(decoded[separator+1:])
	wantUser := sha256.Sum256([]byte(username))
	wantPassword := sha256.Sum256([]byte(password))
	actualUser := sha256.Sum256([]byte(gotUser))
	actualPassword := sha256.Sum256([]byte(gotPassword))
	return subtle.ConstantTimeCompare(wantUser[:], actualUser[:]) == 1 &&
		subtle.ConstantTimeCompare(wantPassword[:], actualPassword[:]) == 1
}

type statusLoggingWriter struct {
	http.ResponseWriter
	status   int
	clientID string
}

func (w *statusLoggingWriter) WriteHeader(status int) {
	w.status = status
	w.ResponseWriter.WriteHeader(status)
}

func (w *statusLoggingWriter) Write(p []byte) (int, error) {
	n, err := w.ResponseWriter.Write(p)
	proxyStats.httpResponseBytes.Add(int64(n))
	addClientCounter(w.clientID, func(c *clientCounterSet) { c.httpResponseBytes.Add(int64(n)) })
	return n, err
}

func (w *statusLoggingWriter) Flush() {
	if flusher, ok := w.ResponseWriter.(http.Flusher); ok {
		flusher.Flush()
	}
}

func (w *statusLoggingWriter) Hijack() (net.Conn, *bufio.ReadWriter, error) {
	hijacker, ok := w.ResponseWriter.(http.Hijacker)
	if !ok {
		return nil, nil, errors.New("HTTP hijacking is not supported")
	}
	return hijacker.Hijack()
}

func safeLogValue(value string) string {
	// Log host/target metadata only, never full URLs, query strings, or credentials.
	value = strings.ReplaceAll(value, "\r", "")
	value = strings.ReplaceAll(value, "\n", "")
	return value
}

func handleConnect(w http.ResponseWriter, r *http.Request, clientID string) {
	host := r.Host
	if host == "" {
		host = r.URL.Host
	}
	if host == "" || strings.Contains(host, "@") {
		http.Error(w, "Invalid CONNECT target", http.StatusBadRequest)
		return
	}
	if _, _, err := net.SplitHostPort(host); err != nil {
		if strings.Contains(err.Error(), "missing port in address") && !strings.Contains(host, "]") {
			host = net.JoinHostPort(host, "443")
		} else {
			http.Error(w, "CONNECT target must be host:port", http.StatusBadRequest)
			return
		}
	}

	dialStarted := time.Now()
	ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
	defer cancel()
	dialer := &net.Dialer{Timeout: 10 * time.Second}
	upstream, err := dialer.DialContext(ctx, "tcp", host)
	dialDuration := time.Since(dialStarted)
	if err != nil {
		log.Printf("CONNECT failed target=%q dial_duration=%s error=%q",
			safeLogValue(host), dialDuration.Round(time.Millisecond), err)
		http.Error(w, "Unable to connect to upstream", http.StatusBadGateway)
		return
	}

	hijacker, ok := w.(http.Hijacker)
	if !ok {
		_ = upstream.Close()
		http.Error(w, "HTTP hijacking is not supported", http.StatusInternalServerError)
		return
	}
	client, rw, err := hijacker.Hijack()
	if err != nil {
		_ = upstream.Close()
		log.Printf("CONNECT hijack failed target=%q error=%q", safeLogValue(host), err)
		return
	}
	if _, err = rw.WriteString("HTTP/1.1 200 Connection Established\r\n\r\n"); err != nil {
		_ = client.Close()
		_ = upstream.Close()
		log.Printf("CONNECT handshake failed target=%q error=%q", safeLogValue(host), err)
		return
	}
	if err = rw.Flush(); err != nil {
		_ = client.Close()
		_ = upstream.Close()
		log.Printf("CONNECT handshake failed target=%q error=%q", safeLogValue(host), err)
		return
	}

	log.Printf("CONNECT established target=%q dial_duration=%s",
		safeLogValue(host), dialDuration.Round(time.Millisecond))
	go tunnel(upstream, client, rw.Reader, host, clientID, time.Now())
}

type tunnelCopyResult struct {
	direction string
	bytes     int64
	err       error
}

func tunnel(upstream, client net.Conn, clientReader io.Reader, host, clientID string, started time.Time) {
	results := make(chan tunnelCopyResult, 2)

	go func() {
		n, err := io.Copy(upstream, clientReader)
		if tcp, ok := upstream.(*net.TCPConn); ok {
			_ = tcp.CloseWrite()
		}
		results <- tunnelCopyResult{direction: "client_to_upstream", bytes: n, err: err}
	}()

	go func() {
		n, err := io.Copy(client, upstream)
		if tcp, ok := client.(*net.TCPConn); ok {
			_ = tcp.CloseWrite()
		}
		results <- tunnelCopyResult{direction: "upstream_to_client", bytes: n, err: err}
	}()

	first := <-results
	if first.err != nil {
		_ = client.Close()
		_ = upstream.Close()
	}
	second := <-results
	_ = client.Close()
	_ = upstream.Close()

	clientToUpstream, upstreamToClient := int64(0), int64(0)
	for _, result := range []tunnelCopyResult{first, second} {
		switch result.direction {
		case "client_to_upstream":
			clientToUpstream = result.bytes
		case "upstream_to_client":
			upstreamToClient = result.bytes
		}
	}

	reason := "clean_eof"
	for _, result := range []tunnelCopyResult{first, second} {
		if result.err != nil && !errors.Is(result.err, net.ErrClosed) {
			reason = result.direction + "_error"
			log.Printf("CONNECT tunnel copy error target=%q direction=%s error=%q",
				safeLogValue(host), result.direction, result.err)
		}
	}
	proxyStats.connectTunnels.Add(1)
	proxyStats.connectClientToUpstream.Add(clientToUpstream)
	proxyStats.connectUpstreamToClient.Add(upstreamToClient)
	addClientCounter(clientID, func(c *clientCounterSet) {
		c.connectTunnels.Add(1)
		c.connectClientToUpstream.Add(clientToUpstream)
		c.connectUpstreamToClient.Add(upstreamToClient)
	})
	log.Printf("CONNECT closed target=%q duration=%s bytes_client_to_upstream=%d bytes_upstream_to_client=%d reason=%s",
		safeLogValue(host), time.Since(started).Round(time.Millisecond),
		clientToUpstream, upstreamToClient, reason)
}
