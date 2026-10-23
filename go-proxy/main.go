package main

import (
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
	addr := os.Getenv("PROXY_LISTEN")
	if addr == "" {
		addr = "127.0.0.1:8080"
	}
	server := &http.Server{
		Addr:              addr,
		Handler:           proxyHandler(),
		ReadHeaderTimeout: 10 * time.Second,
		IdleTimeout:       90 * time.Second,
	}
	log.Printf("TGFN.BrowserMini Go proxy listening on %s", addr)
	log.Fatal(server.ListenAndServe())
}

func proxyHandler() http.Handler {
	proxy := &httputil.ReverseProxy{
		Director: func(req *http.Request) {
			req.Header.Del("Proxy-Connection")
			req.Header.Set("Via", "1.1 TGFN.BrowserMini")
		},
		FlushInterval: 100 * time.Millisecond,
		ErrorLog:      log.New(os.Stderr, "proxy: ", log.LstdFlags),
		Transport: &http.Transport{
			Proxy:                 http.ProxyFromEnvironment,
			ForceAttemptHTTP2:     true,
			MaxIdleConns:          100,
			IdleConnTimeout:       90 * time.Second,
			TLSHandshakeTimeout:   10 * time.Second,
			ExpectContinueTimeout: 1 * time.Second,
			DisableCompression:    false,
		},
	}

	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		if r.Method == http.MethodConnect {
			handleConnect(w, r)
			return
		}
		if r.URL == nil || (r.URL.Scheme != "http" && r.URL.Scheme != "https") || r.URL.Host == "" {
			http.Error(w, "Send an absolute HTTP(S) URL to the proxy", http.StatusBadRequest)
			return
		}
		if strings.Contains(r.URL.Host, "@") {
			http.Error(w, "userinfo in URL is not allowed", http.StatusBadRequest)
			return
		}
		r2 := r.Clone(r.Context())
		r2.URL = r.URL
		r2.Host = r.URL.Host
		proxy.ServeHTTP(w, r2)
	})
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
	ctx, cancel := context.WithTimeout(r.Context(), 10*time.Second)
	defer cancel()
	dialer := &net.Dialer{Timeout: 10 * time.Second}
	upstream, err := dialer.DialContext(ctx, "tcp", host)
	if err != nil {
		http.Error(w, "Unable to connect to upstream", http.StatusBadGateway)
		return
	}
	hijacker, ok := w.(http.Hijacker)
	if !ok {
		upstream.Close()
		http.Error(w, "HTTP hijacking is not supported", http.StatusInternalServerError)
		return
	}
	client, rw, err := hijacker.Hijack()
	if err != nil {
		upstream.Close()
		return
	}
	if _, err = rw.WriteString("HTTP/1.1 200 Connection Established\r\n\r\n"); err != nil {
		client.Close()
		upstream.Close()
		return
	}
	if err = rw.Flush(); err != nil {
		client.Close()
		upstream.Close()
		return
	}
	go tunnel(upstream, client)
	go func() {
		if rw.Reader.Buffered() > 0 {
			if _, copyErr := io.Copy(upstream, rw.Reader); copyErr != nil && !errors.Is(copyErr, net.ErrClosed) {
				log.Printf("CONNECT client-to-upstream: %v", copyErr)
			}
		}
		_, _ = io.Copy(upstream, client)
		_ = upstream.Close()
	}()
}

func tunnel(upstream net.Conn, client net.Conn) {
	defer upstream.Close()
	defer client.Close()
	if _, err := io.Copy(client, upstream); err != nil && !errors.Is(err, net.ErrClosed) {
		log.Printf("CONNECT upstream-to-client: %v", err)
	}
}
