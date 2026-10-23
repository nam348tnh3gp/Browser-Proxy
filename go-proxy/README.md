# TGFN BrowserMini Go Proxy

Standalone HTTP forward proxy used by the Android and J2ME clients.

## Run

```sh
cd go-proxy
go run .
```

By default it listens on `127.0.0.1:8080`. To allow a compatible phone on a trusted LAN, set `PROXY_LISTEN=0.0.0.0:8080` and configure the phone to use the host's LAN address. Do not expose an unauthenticated forward proxy to the public internet.

## J2ME compact HTML mode

Requests with `X-TGFN-Compact: 1` receive server-side compact HTML when the upstream response is HTML and no larger than 4 MiB. The proxy removes scripts, stylesheets/style blocks, embedded media and common navigation/footer regions; keeps a small set of semantic HTML tags and safe display/link attributes; and collapses whitespace. The J2ME client opts into this mode automatically for HTTP requests sent through this proxy. Requests without the header are passed through unchanged.

This can reduce markup and avoid sending much of the content a MIDP browser cannot render. It is not Opera Mini's proprietary OBML protocol or a full cloud browser engine, and it does not transcode or resize image bytes. JavaScript-heavy sites, forms and modern CSS layouts may still be unusable.

## Security and protocol

- HTTPS tunneling uses standard HTTP CONNECT and does not decrypt TLS traffic.
- J2ME client currently uses the proxy for HTTP only; HTTPS uses the handset's direct TLS connection.
- The proxy has no authentication by default. Restrict it to trusted interfaces/networks or add authentication before remote deployment.
