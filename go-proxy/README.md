# TGFN BrowserMini Go Proxy

Standalone HTTP forward proxy used by the Android browser.

## Run

```sh
cd go-proxy
go run .
```

By default, the proxy listens on `127.0.0.1:8080`. To allow a phone or another machine on a trusted LAN to connect, set `PROXY_LISTEN=0.0.0.0:8080` and configure the client with the host's reachable LAN address.

## HTTP and HTTPS

- Ordinary HTTP requests are forwarded to the upstream server.
- HTTPS uses the standard HTTP `CONNECT` tunnel. The proxy relays encrypted TLS bytes and does not decrypt HTTPS content.
- J2ME-specific compact HTML mode is opt-in through `X-TGFN-Compact: 1`; eligible HTML responses are simplified up to 4 MiB.
- Eligible HTTP text responses may be gzip-compressed when the request advertises `Accept-Encoding: gzip`. Compression applies only to successful GET responses without a Range request, only when the body is at most 8 MiB and gzip makes it smaller. Existing `Content-Encoding`, `Cache-Control: no-transform`, non-text content, and other ineligible responses are left unchanged. `Vary: Accept-Encoding` is added when compression is applied.
- HTTPS `CONNECT` is a byte tunnel: the proxy cannot compress or inspect its encrypted page content.
- Logs include request method, target host, status and duration. CONNECT logs include tunnel establishment/closure, byte counts and errors. Full URLs, query strings and proxy credentials are not logged.
- The local `GET /__tgfn/stats` endpoint returns measured process-lifetime counters for HTTP response-body bytes, gzip-compressed response counts and byte savings, completed CONNECT tunnel byte counts, and registered client count. `GET /__tgfn/register?client_id=<stable-id>` registers the app's persistent client ID; `GET /__tgfn/stats?client_id=<stable-id>` returns that client's counters, while `/__tgfn/stats` without an ID returns network-wide totals. Counters are in memory and reset when the proxy process restarts. The endpoint is subject to configured proxy authentication; the Android analytics screen currently works with authentication disabled. WebView's proxy API cannot attach a custom client header to every HTTP request and CONNECT tunnel, so this version attributes browser traffic by the source IP registered by the client. The proxy canonicalizes IPv4/IPv6 address forms, removes stale IP mappings when a client re-registers from a new address, and falls back to the sole registered ID if only one installation is known. If multiple installations register from the same source IP, that IP is marked ambiguous and its traffic is deliberately not assigned to an arbitrary device; exact per-device attribution for multiple clients behind the same NAT requires per-client credentials or separate proxy listeners. Per-client stats include `client_attribution_status` so the app can explain when attribution is ambiguous or using the single-client fallback. `registered_clients` counts known registered IDs during this proxy process, not guaranteed currently-online physical devices.

## Optional proxy authentication

By default, authentication is disabled for compatibility with existing clients. For a trusted deployment that needs access control, configure **both** environment variables:

```sh
PROXY_USERNAME=browser-client
PROXY_PASSWORD='replace-with-a-long-random-password'
PROXY_LISTEN=0.0.0.0:8080
go run .
```

The proxy then requires HTTP Basic credentials through the standard `Proxy-Authorization` header for both ordinary HTTP requests and CONNECT. Missing or incorrect credentials receive `407 Proxy Authentication Required`. Set both variables or neither; a partial configuration causes startup to fail. Basic authentication does not encrypt credentials, so use it only on a trusted network or behind a secure transport/VPN. The Android WebView settings in this version do not expose proxy credential entry, so leave authentication disabled for that client unless credentials are supplied by a compatible client.

## Data saving behavior

Gzip is negotiated per request and only applied to eligible ordinary HTTP responses. It does not transcode images or modify HTTPS traffic inside CONNECT tunnels, and savings depend on the response content. The Android Data Saver toggle uses WebView cache behavior separately; it does not itself enable image blocking or Chrome-Lite-style rewriting. Open **Menu → Data Saver analytics** in the app to inspect measured counters from this Go proxy. Received bytes combine HTTP response-body bytes with downstream CONNECT tunnel bytes; gzip savings are counted only when the proxy actually emits a smaller gzip body. These counters are proxy-wide, not per-tab or per-device, when clients share a proxy.

## Security

This is a forward proxy and can reach destinations accessible to its host. Do not expose it directly to the public internet without appropriate network controls, authentication and monitoring. The default listen address is loopback-only. HTTPS CONNECT is an encrypted tunnel, not a content-filtering or TLS-inspection feature.
