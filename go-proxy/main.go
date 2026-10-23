package main

import (
	"bufio"
	"crypto/sha256"
	"crypto/subtle"
	"encoding/base64"
	"context"
	"errors"
	"io"
	"log"
	"net"
	"net/http"
	"net/http/httputil"
	"os"
	"strings"
	"time"
)

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
		FlushInterval:  100 * time.Millisecond,
		ModifyResponse: compactHTMLResponse,
		ErrorLog:       log.New(os.Stderr, "proxy: ", log.LstdFlags),
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
		if r.Method == http.MethodConnect {
			log.Printf("request method=CONNECT client=%q target=%q", r.RemoteAddr, safeLogValue(r.Host))
			handleConnect(w, r)
			return
		}

		target := ""
		if r.URL != nil {
			target = r.URL.Host
		}
		logWriter := &statusLoggingWriter{ResponseWriter: w, status: http.StatusOK}
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
	status int
}

func (w *statusLoggingWriter) WriteHeader(status int) {
	w.status = status
	w.ResponseWriter.WriteHeader(status)
}

func (w *statusLoggingWriter) Write(p []byte) (int, error) {
	return w.ResponseWriter.Write(p)
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

func handleConnect(w http.ResponseWriter, r *http.Request) {
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
	go tunnel(upstream, client, rw.Reader, host, time.Now())
}

type tunnelCopyResult struct {
	direction string
	bytes     int64
	err       error
}

func tunnel(upstream, client net.Conn, clientReader io.Reader, host string, started time.Time) {
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
	log.Printf("CONNECT closed target=%q duration=%s bytes_client_to_upstream=%d bytes_upstream_to_client=%d reason=%s",
		safeLogValue(host), time.Since(started).Round(time.Millisecond),
		clientToUpstream, upstreamToClient, reason)
}
