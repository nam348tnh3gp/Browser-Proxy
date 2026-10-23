# TGFN BrowserMini Go Proxy

Standalone HTTP forward proxy used by the Android and J2ME clients.

## Run

```sh
cd go-proxy
go run .
```

By default it listens on `127.0.0.1:8080`. To allow a compatible phone on a trusted LAN, set `PROXY_LISTEN=0.0.0.0:8080` and configure the phone to use the host's LAN address. **Do not expose an unauthenticated forward proxy to the public internet.**

## J2ME compact-page protocol (TBO1)

The J2ME client sends `X-TGFN-Format: TBO1` and receives `application/x-tgfn-obml; version=1` for eligible HTML responses up to 4 MiB. The binary packet begins with `TBO1`, followed by a length-prefixed UTF-8 title, a 16-bit block count, then typed blocks with length-prefixed text and URL fields. Current block types represent text, H1/H2 emphasis, list markers, links, paragraph breaks and image references. The server strips scripts/styles and selected heavy regions before tokenizing page content.

This is an original, OBML-inspired protocol for this project, **not** Opera's proprietary OBML protocol and not wire-compatible with Opera Mini. It sends a compact block representation rather than full HTML, but it is not a complete cloud browser engine. Images remain separate resources; server-side image resizing/recompression, JavaScript execution, CSS layout fidelity and complex forms are not implemented. Requests without the format header are unchanged; the legacy `X-TGFN-Compact: 1` HTML-simplification mode remains available.

## J2ME HTTP and HTTPS support

- HTTP pages use the normal absolute-URL forward-proxy request path.
- HTTPS pages and HTTPS images use the server-side `/__tgfn/fetch?url=...` endpoint over the phone-to-proxy HTTP connection. The Go proxy fetches the remote HTTPS resource with normal certificate verification, follows a limited number of redirects, and transforms eligible HTML into TBO1. This avoids needing portable TLS-over-CONNECT support in MIDP.
- The endpoint only permits HTTP(S), rejects URL credentials, blocks loopback/private/link-local/reserved destinations, rechecks redirect targets and validates addresses again when dialing to reduce SSRF/DNS-rebinding risk.
- HTTPS through this endpoint is **server-side fetching**, not an end-to-end CONNECT tunnel: the proxy server can observe the requested URL and response content, and the destination sees the proxy server's network address. Use only a trusted proxy.
- The normal HTTP CONNECT handler remains available for clients such as Android WebView that perform TLS themselves.

The proxy has no authentication by default. Restrict it to trusted interfaces/networks or add authentication before remote deployment. The fetch endpoint does not make an otherwise public unauthenticated proxy safe to expose.
