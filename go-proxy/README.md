# TGFN BrowserMini Go Proxy

Standalone HTTP forward proxy used by the Android and J2ME clients.

## Run

```sh
cd go-proxy
go run .
```

By default it listens on `127.0.0.1:8080`. To allow a compatible phone on a trusted LAN, set `PROXY_LISTEN=0.0.0.0:8080` and configure the phone to use the host's LAN address. Do not expose an unauthenticated forward proxy to the public internet.

## TBO1 OBML-inspired page protocol

The J2ME client sends `X-TGFN-Format: TBO1` and receives `application/x-tgfn-obml; version=1` for eligible HTML responses up to 4 MiB. The binary packet begins with `TBO1`, followed by a length-prefixed UTF-8 title, a 16-bit block count, then typed blocks with length-prefixed text and URL fields. Current block types represent text, H1/H2 emphasis, list markers, links, paragraph breaks and image references. The server strips scripts/styles and selected heavy regions before tokenizing page content. Requests without the format header are unchanged; the legacy `X-TGFN-Compact: 1` HTML-simplification mode remains available.

This is a new, original OBML-inspired format for this project, **not** Opera's proprietary OBML protocol and not wire-compatible with Opera Mini. It avoids sending most HTML markup and creates a server-derived block layout, but it is not a complete cloud browser engine. Images are referenced and fetched separately; image resizing/recompression, CSS layout fidelity, JavaScript execution, forms and HTTPS-over-proxy are not implemented.

## Security and protocol

- HTTPS tunneling uses standard HTTP CONNECT and does not decrypt TLS traffic.
- J2ME client currently uses the proxy for HTTP only; HTTPS uses the handset's direct TLS connection.
- The proxy has no authentication by default. Restrict it to trusted interfaces/networks or add authentication before remote deployment.
