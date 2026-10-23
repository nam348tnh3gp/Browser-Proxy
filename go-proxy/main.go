package main

import (
	"log"
	"net/http"
	"net/http/httputil"
	"net/url"
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
	director := func(req *http.Request) {
		req.Header.Del("Proxy-Connection")
		req.Header.Set("Via", "1.1 TGFN.BrowserMini")
	}
	proxy := &httputil.ReverseProxy{
		Director:       director,
		FlushInterval:  100 * time.Millisecond,
		ErrorLog:       log.New(os.Stderr, "proxy: ", log.LstdFlags),
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
			http.Error(w, "CONNECT is not supported by this simple forward-proxy endpoint; use a dedicated forward proxy implementation", http.StatusNotImplemented)
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
		target, err := url.Parse(r.URL.String())
		if err != nil {
			http.Error(w, "Invalid target URL", http.StatusBadRequest)
			return
		}
		r2 := r.Clone(r.Context())
		r2.URL = target
		r2.Host = target.Host
		proxy.ServeHTTP(w, r2)
	})
}
