# TGFN BrowserMini Go Proxy

Standalone HTTP forward proxy used by the Android and J2ME clients.

## Run

```sh
cd go-proxy
go run .
```

By default it listens on `127.0.0.1:8080`. To allow a compatible phone on a trusted LAN, set `PROXY_LISTEN=0.0.0.0:8080` and configure the phone to use the host's LAN address. **Do not expose an unauthenticated forward proxy to the public internet.**

## J2ME mobile-page protocols (MOB1 and TBO1)

The J2ME client requests `X-TGFN-Format: MOB1`. When Chromium is available, the proxy renders the target at a 240×500 mobile viewport and returns an `IML2` packet containing compressed JPEG page tiles plus link text, URL, and rendered hitbox coordinates. The client displays tiles edge-to-edge and uses directional keys to focus links in place; FIRE/5 opens the selected link without a duplicate page-link list. This is a visual capture, so page JavaScript does not execute on the handset.

If Chromium is unavailable or rendering fails, the proxy falls back to `X-TGFN-Format: TBO1` semantic blocks and responds with `application/x-tgfn-obml; version=1` for eligible HTML responses up to 4 MiB. The TBO1 fallback packet begins with `TBO1`, followed by a length-prefixed UTF-8 title, a 16-bit block count, then typed blocks with length-prefixed text and URL fields. Current block types represent text, H1/H2 emphasis, list markers, links, paragraph breaks and image references. The server strips scripts/styles and selected heavy regions before tokenizing page content.

This is an original, OBML-inspired protocol for this project, **not** Opera's proprietary OBML protocol and not wire-compatible with Opera Mini. TBO1 sends a compact block representation rather than full HTML. MOB1 preserves rendered mobile appearance through JPEG tiles, but is static after capture; JavaScript interactions and form submission are not supported in MOB1. TBO1 supports common GET forms and separately resized/recompressed image resources. Requests without the format header are unchanged; the legacy `X-TGFN-Compact: 1` HTML-simplification mode remains available.

## J2ME HTTP and HTTPS support

- HTTP pages use the normal absolute-URL forward-proxy request path.
- HTTPS pages and HTTPS images use the server-side `/__tgfn/fetch?url=...` endpoint over the phone-to-proxy HTTP connection. The Go proxy fetches the remote HTTPS resource with normal certificate verification, follows a limited number of redirects, and transforms eligible HTML into TBO1. This avoids needing portable TLS-over-CONNECT support in MIDP.
- The endpoint only permits HTTP(S), rejects URL credentials, blocks loopback/private/link-local/reserved destinations, rechecks redirect targets and validates addresses again when dialing to reduce SSRF/DNS-rebinding risk.
- HTTPS through this endpoint is **server-side fetching**, not an end-to-end CONNECT tunnel: the proxy server can observe the requested URL and response content, and the destination sees the proxy server's network address. Use only a trusted proxy.
- The normal HTTP CONNECT handler remains available for clients such as Android WebView that perform TLS themselves.

The proxy has no authentication by default. Restrict it to trusted interfaces/networks or add authentication before remote deployment. The fetch endpoint does not make an otherwise public unauthenticated proxy safe to expose.
