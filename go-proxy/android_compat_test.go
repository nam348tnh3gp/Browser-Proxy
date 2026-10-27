package main

import (
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"testing"
)

func TestAndroidRegisterAndStats(t *testing.T) {
	req := httptest.NewRequest(http.MethodGet, "/__tgfn/register?client_id=android-main-1", nil)
	req.RemoteAddr = "203.0.113.8:1234"
	rec := httptest.NewRecorder()
	if !serveAndroidCompat(rec, req) {
		t.Fatal("register was not handled")
	}
	if rec.Code != http.StatusOK {
		t.Fatalf("register status = %d", rec.Code)
	}

	req = httptest.NewRequest(http.MethodGet, "/__tgfn/stats?client_id=android-main-1", nil)
	req.RemoteAddr = "203.0.113.8:1234"
	rec = httptest.NewRecorder()
	if !serveAndroidCompat(rec, req) {
		t.Fatal("stats was not handled")
	}
	var stats proxyStatsSnapshot
	if err := json.Unmarshal(rec.Body.Bytes(), &stats); err != nil {
		t.Fatal(err)
	}
	if stats.ClientID != "android-main-1" || stats.RegisteredClients < 1 {
		t.Fatalf("stats = %+v", stats)
	}
	if stats.ClientAttributionStatus != "matched_source_ip" {
		t.Fatalf("attribution = %q", stats.ClientAttributionStatus)
	}
}
