package main

import (
	"encoding/json"
	"net"
	"net/http"
	"sync"
	"sync/atomic"
	"time"
)

// Android BrowserMini on main expects these origin-form endpoints and CONNECT.
// J2ME still uses /__tgfn/fetch and /__tgfn/img.

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

var (
	proxyProcessStartedAt = time.Now().UTC().Format(time.RFC3339Nano)
	proxyStats            clientCounterSet
	clientTracking        struct {
		sync.RWMutex
		idByIP        map[string]string
		ambiguousIPs  map[string]bool
		registeredIDs map[string]struct{}
		stats         map[string]*clientCounterSet
	}
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
	ClientAttributionStatus string `json:"client_attribution_status,omitempty"`
}

func serveAndroidCompat(w http.ResponseWriter, r *http.Request) bool {
	if r.Method != http.MethodGet || r.URL == nil || r.URL.IsAbs() {
		return false
	}
	switch r.URL.Path {
	case "/health", "/__tgfn/health":
		w.Header().Set("Content-Type", "text/plain; charset=utf-8")
		w.Header().Set("Cache-Control", "no-store")
		w.WriteHeader(http.StatusOK)
		_, _ = w.Write([]byte("ok"))
		return true
	case "/__tgfn/register":
		id := r.URL.Query().Get("client_id")
		if !registerClient(r.RemoteAddr, id) {
			http.Error(w, "Invalid client ID or remote address", http.StatusBadRequest)
			return true
		}
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		w.Header().Set("Cache-Control", "no-store")
		_ = json.NewEncoder(w).Encode(map[string]any{"registered": true, "client_id": id})
		return true
	case "/__tgfn/stats":
		id := r.URL.Query().Get("client_id")
		stats := snapshotCounters(&proxyStats)
		if id != "" {
			if counters := lookupClientCounters(id); counters != nil {
				stats = snapshotCounters(counters)
			} else {
				stats = snapshotCounters(&clientCounterSet{})
			}
			stats.ClientID = id
			stats.ClientAttributionStatus = clientAttributionStatus(r.RemoteAddr, id)
		}
		w.Header().Set("Content-Type", "application/json; charset=utf-8")
		w.Header().Set("Cache-Control", "no-store")
		_ = json.NewEncoder(w).Encode(stats)
		return true
	default:
		return false
	}
}

func snapshotCounters(c *clientCounterSet) proxyStatsSnapshot {
	original := c.gzipOriginalBytes.Load()
	compressed := c.gzipCompressedBytes.Load()
	clientTracking.RLock()
	registered := len(clientTracking.registeredIDs)
	clientTracking.RUnlock()
	saved := original - compressed
	if saved < 0 {
		saved = 0
	}
	return proxyStatsSnapshot{
		ProcessStartedAt:        proxyProcessStartedAt,
		HTTPRequests:            c.httpRequests.Load(),
		HTTPResponseBytes:       c.httpResponseBytes.Load(),
		GzipResponses:           c.gzipResponses.Load(),
		GzipOriginalBytes:       original,
		GzipCompressedBytes:     compressed,
		GzipSavedBytes:          saved,
		ConnectTunnels:          c.connectTunnels.Load(),
		ConnectClientToUpstream: c.connectClientToUpstream.Load(),
		ConnectUpstreamToClient: c.connectUpstreamToClient.Load(),
		RegisteredClients:       registered,
	}
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
		host = remoteAddr
	}
	clientTracking.Lock()
	defer clientTracking.Unlock()
	if clientTracking.idByIP == nil {
		clientTracking.idByIP = make(map[string]string)
	}
	if clientTracking.ambiguousIPs == nil {
		clientTracking.ambiguousIPs = make(map[string]bool)
	}
	if clientTracking.registeredIDs == nil {
		clientTracking.registeredIDs = make(map[string]struct{})
	}
	if clientTracking.stats == nil {
		clientTracking.stats = make(map[string]*clientCounterSet)
	}
	if existing := clientTracking.idByIP[host]; existing != "" && existing != id {
		delete(clientTracking.idByIP, host)
		clientTracking.ambiguousIPs[host] = true
	} else if !clientTracking.ambiguousIPs[host] {
		clientTracking.idByIP[host] = id
	}
	clientTracking.registeredIDs[id] = struct{}{}
	if clientTracking.stats[id] == nil {
		clientTracking.stats[id] = &clientCounterSet{}
	}
	return true
}

func lookupClientCounters(id string) *clientCounterSet {
	clientTracking.RLock()
	defer clientTracking.RUnlock()
	return clientTracking.stats[id]
}

func clientIDFromRemoteAddr(remoteAddr string) string {
	host, _, err := net.SplitHostPort(remoteAddr)
	if err != nil || host == "" {
		host = remoteAddr
	}
	clientTracking.RLock()
	defer clientTracking.RUnlock()
	if clientTracking.ambiguousIPs[host] {
		return ""
	}
	return clientTracking.idByIP[host]
}

func clientAttributionStatus(remoteAddr, id string) string {
	host, _, err := net.SplitHostPort(remoteAddr)
	if err != nil || host == "" {
		host = remoteAddr
	}
	clientTracking.RLock()
	defer clientTracking.RUnlock()
	if clientTracking.ambiguousIPs[host] {
		return "ambiguous_source_ip"
	}
	if mapped := clientTracking.idByIP[host]; mapped != "" {
		if mapped == id {
			return "matched_source_ip"
		}
		return "source_ip_mapped_to_other_client"
	}
	if _, ok := clientTracking.registeredIDs[id]; ok && len(clientTracking.registeredIDs) == 1 {
		return "single_client_fallback"
	}
	return "source_ip_unmapped"
}

func noteHTTP(remoteAddr string, bytes int64) {
	proxyStats.httpRequests.Add(1)
	proxyStats.httpResponseBytes.Add(bytes)
	if id := clientIDFromRemoteAddr(remoteAddr); id != "" {
		if c := lookupClientCounters(id); c != nil {
			c.httpRequests.Add(1)
			c.httpResponseBytes.Add(bytes)
		}
	}
}

func noteConnect(remoteAddr string, clientToUpstream, upstreamToClient int64) {
	proxyStats.connectTunnels.Add(1)
	proxyStats.connectClientToUpstream.Add(clientToUpstream)
	proxyStats.connectUpstreamToClient.Add(upstreamToClient)
	if id := clientIDFromRemoteAddr(remoteAddr); id != "" {
		if c := lookupClientCounters(id); c != nil {
			c.connectTunnels.Add(1)
			c.connectClientToUpstream.Add(clientToUpstream)
			c.connectUpstreamToClient.Add(upstreamToClient)
		}
	}
}
