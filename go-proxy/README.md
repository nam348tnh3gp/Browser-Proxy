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

Gzip is negotiated per request and only applied to eligible ordinary HTTP responses. It does not transcode images or modify HTTPS traffic inside CONNECT tunnels, and savings depend on the response content. The Android Data Saver toggle uses WebView cache behavior separately; it does not itself enable image blocking or Chrome-Lite-style rewriting.

## Security

This is a forward proxy and can reach destinations accessible to its host. Do not expose it directly to the public internet without appropriate network controls, authentication and monitoring. The default listen address is loopback-only. HTTPS CONNECT is an encrypted tunnel, not a content-filtering or TLS-inspection feature.
