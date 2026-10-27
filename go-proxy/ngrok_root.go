package main

import (
	"fmt"
	"net/http"
)

// serveNgrokRoot answers the public ngrok URL. An empty or non-HTTP upstream
// response is what browsers report as ERR_EMPTY_RESPONSE.
func serveNgrokRoot(w http.ResponseWriter, r *http.Request) bool {
	if r.Method != http.MethodGet || r.URL == nil || r.URL.IsAbs() {
		return false
	}
	if r.URL.Path != "/" && r.URL.Path != "" {
		return false
	}
	if r.URL.Query().Get("url") != "" {
		r.URL.Path = remoteFetchPath
		return false
	}
	host := r.Host
	if host == "" {
		host = "proxy"
	}
	w.Header().Set("Content-Type", "text/html; charset=utf-8")
	w.Header().Set("Cache-Control", "no-store")
	w.WriteHeader(http.StatusOK)
	fmt.Fprintf(w, `<!doctype html><meta charset="utf-8"><title>TGFN proxy</title>
<body><h1>TGFN proxy is running</h1>
<p>J2ME / fetch test:</p>
<p><a href="/__tgfn/fetch?url=https://example.com">/__tgfn/fetch?url=https://example.com</a></p>
<p>Health: <a href="/health">/health</a></p>
<p>Host: %s</p>
</body>`, host)
	return true
}
