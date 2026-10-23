package main

import (
	"bytes"
	"context"
	"errors"
	"fmt"
	"io"
	"log"
	"net"
	"net/http"
	"net/url"
	"strconv"
	"strings"
	"time"
)

const remoteFetchPath = "/__tgfn/fetch"
const maxRemoteFetchBytes = maxCompactHTMLBytes

// serveRemoteFetch provides server-side HTTPS fetching for Java ME clients that
// cannot portably layer TLS over an HTTP CONNECT socket. It is deliberately
// available only on origin-form requests to avoid colliding with normal proxy URLs.
func serveRemoteFetch(w http.ResponseWriter, r *http.Request) bool {
	if r.URL == nil || r.URL.IsAbs() || r.URL.Path != remoteFetchPath {
		return false
	}
	started := time.Now()
	target, err := url.Parse(r.URL.Query().Get("url"))
	if err == nil {
		err = validateRemoteTarget(r.Context(), target)
	}
	if err != nil {
		log.Printf("remote_fetch rejected client=%q reason=%q", safeLogValue(r.RemoteAddr), safeLogValue(err.Error()))
		http.Error(w, "Invalid or disallowed remote URL", http.StatusBadRequest)
		return true
	}

	transport := &http.Transport{
		Proxy:                 nil,
		ForceAttemptHTTP2:     true,
		TLSHandshakeTimeout:   10 * time.Second,
		ResponseHeaderTimeout: 15 * time.Second,
		DisableCompression:    false,
		DialContext:           safeRemoteDial,
	}
	defer transport.CloseIdleConnections()
	client := &http.Client{
		Transport: transport,
		Timeout:   25 * time.Second,
		CheckRedirect: func(req *http.Request, via []*http.Request) error {
			if len(via) >= 5 {
				return errors.New("too many redirects")
			}
			return validateRemoteTarget(req.Context(), req.URL)
		},
	}
	req, err := http.NewRequestWithContext(r.Context(), http.MethodGet, target.String(), nil)
	if err != nil {
		http.Error(w, "Invalid remote URL", http.StatusBadRequest)
		return true
	}
	req.Header.Set("User-Agent", "TGFN-BrowserMini-RemoteFetch/2.0")
	req.Header.Set("Accept", r.Header.Get("Accept"))
	if req.Header.Get("Accept") == "" {
		req.Header.Set("Accept", "text/html,text/plain,image/*,*/*")
	}
	if r.Header.Get("X-TGFN-Format") == "TBO1" {
		req.Header.Set("X-TGFN-Format", "TBO1")
	}

	resp, err := client.Do(req)
	if err != nil {
		log.Printf("remote_fetch failed target=%q error=%q duration=%s", safeLogValue(target.Host), safeLogValue(err.Error()), time.Since(started).Round(time.Millisecond))
		http.Error(w, "Remote fetch failed", http.StatusBadGateway)
		return true
	}
	defer resp.Body.Close()
	body, err := io.ReadAll(io.LimitReader(resp.Body, maxRemoteFetchBytes+1))
	if err != nil {
		http.Error(w, "Unable to read remote response", http.StatusBadGateway)
		return true
	}
	if len(body) > maxRemoteFetchBytes {
		http.Error(w, "Remote response exceeds size limit", http.StatusRequestEntityTooLarge)
		return true
	}

	upstreamReq := resp.Request
	if upstreamReq == nil {
		upstreamReq = req
	}
	upstreamReq.Header = req.Header.Clone()
	resp.Request = upstreamReq
	resp.Body = io.NopCloser(bytes.NewReader(body))
	resp.ContentLength = int64(len(body))
	if err := compactHTMLResponse(resp); err != nil {
		http.Error(w, "Unable to encode compact page", http.StatusBadGateway)
		return true
	}
	encoded, err := io.ReadAll(resp.Body)
	if err != nil {
		http.Error(w, "Unable to prepare remote response", http.StatusBadGateway)
		return true
	}

	if ct := resp.Header.Get("Content-Type"); ct != "" {
		w.Header().Set("Content-Type", ct)
	}
	w.Header().Set("Cache-Control", "no-store")
	w.Header().Set("Content-Length", strconv.Itoa(len(encoded)))
	w.WriteHeader(resp.StatusCode)
	_, _ = w.Write(encoded)
	log.Printf("remote_fetch complete target=%q status=%d bytes=%d duration=%s", safeLogValue(resp.Request.URL.Host), resp.StatusCode, len(encoded), time.Since(started).Round(time.Millisecond))
	return true
}

func validateRemoteTarget(ctx context.Context, target *url.URL) error {
	if target == nil || (target.Scheme != "http" && target.Scheme != "https") ||
		target.Hostname() == "" || target.User != nil || strings.Contains(target.Host, "@") {
		return errors.New("only absolute HTTP(S) URLs without userinfo are allowed")
	}
	if target.Port() != "" {
		port, err := strconv.Atoi(target.Port())
		if err != nil || port < 1 || port > 65535 {
			return errors.New("invalid target port")
		}
	}
	_, err := lookupPublicIPs(ctx, target.Hostname())
	return err
}

func lookupPublicIPs(ctx context.Context, host string) ([]net.IP, error) {
	if strings.Contains(host, "%") {
		return nil, errors.New("IPv6 zone identifiers are not allowed")
	}
	if ip := net.ParseIP(host); ip != nil {
		if !isPublicIP(ip) {
			return nil, fmt.Errorf("non-public target IP is not allowed")
		}
		return []net.IP{ip}, nil
	}
	ips, err := net.DefaultResolver.LookupIPAddr(ctx, host)
	if err != nil || len(ips) == 0 {
		return nil, fmt.Errorf("target DNS lookup failed")
	}
	out := make([]net.IP, 0, len(ips))
	for _, item := range ips {
		if !isPublicIP(item.IP) {
			return nil, errors.New("target resolves to a non-public IP")
		}
		out = append(out, item.IP)
	}
	return out, nil
}

func isPublicIP(ip net.IP) bool {
	if ip == nil || !ip.IsGlobalUnicast() || ip.IsPrivate() || ip.IsLoopback() ||
		ip.IsLinkLocalUnicast() || ip.IsLinkLocalMulticast() || ip.IsMulticast() || ip.IsUnspecified() {
		return false
	}
	// Also reject shared-address, benchmarking and documentation ranges that
	// Go's generic IsGlobalUnicast check alone does not classify as private.
	blocked := []string{
		"100.64.0.0/10", "198.18.0.0/15", "192.0.2.0/24",
		"198.51.100.0/24", "203.0.113.0/24", "2001:db8::/32",
	}
	for _, cidr := range blocked {
		_, network, _ := net.ParseCIDR(cidr)
		if network.Contains(ip) {
			return false
		}
	}
	return true
}

// safeRemoteDial pins the socket to an IP that was checked immediately before
// dialing, preventing the HTTP transport from resolving a hostname a second time
// to a private address after validation.
func safeRemoteDial(ctx context.Context, network, address string) (net.Conn, error) {
	host, port, err := net.SplitHostPort(address)
	if err != nil {
		return nil, err
	}
	ips, err := lookupPublicIPs(ctx, host)
	if err != nil {
		return nil, err
	}
	dialer := &net.Dialer{Timeout: 10 * time.Second, KeepAlive: 30 * time.Second}
	var lastErr error
	for _, ip := range ips {
		conn, dialErr := dialer.DialContext(ctx, network, net.JoinHostPort(ip.String(), port))
		if dialErr == nil {
			return conn, nil
		}
		lastErr = dialErr
	}
	if lastErr == nil {
		lastErr = errors.New("no public target addresses")
	}
	return nil, lastErr
}
