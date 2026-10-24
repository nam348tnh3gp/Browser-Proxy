# TGFN BrowserMini Go Proxy

Standalone HTTP forward proxy and compact-page encoder for the Android and J2ME clients.

## Run

```sh
cd go-proxy
go test ./...
go run .
```

By default it listens on `127.0.0.1:8080`. To allow a compatible phone on a trusted LAN, set `PROXY_LISTEN=0.0.0.0:8080` and configure the phone to use the host's LAN address. **Do not expose an unauthenticated forward proxy to the public internet.**

## J2ME compact mobile document (TBO1)

The J2ME client requests `X-TGFN-Format: TBO1` by default. This path **does not launch Chromium and does not create screenshots**. The proxy requests mobile HTML using an Android Mobile User-Agent, parses the HTML structure, resolves safe HTTP(S) links and image references, and emits `application/x-tgfn-obml; version=1`.

TBO1 packet layout:

1. Four-byte magic `TBO1`.
2. A UTF-8 title with a 16-bit big-endian byte length.
3. A 16-bit big-endian block count.
4. Typed blocks; each contains a one-byte kind followed by length-prefixed UTF-8 text and URL fields.

The encoder bounds response size, block count and string length. Before encoding TBO1, a sandboxed Goja runtime executes a limited subset of inline JavaScript: `document.write()` and `document.writeln()` output is inserted at the script's location. Execution is bounded to 12 inline scripts, 128 KiB per script and about 100 ms per script; external scripts are skipped and there is no network, filesystem or browser API. It then removes scripts/styles and selected non-readable regions, coalesces adjacent inline text fragments to avoid one-word-per-line rendering, and represents headings, paragraphs, lists, links, images, table separators, form controls and form payloads as compact records. The MIDP client wraps text to the actual device width; selecting a displayed form control opens the native form editor for editing/submission. Images are fetched separately through the proxy's image endpoint and resized/recompressed for small screens.

The representation is semantic rather than pixel-perfect: the client reconstructs a readable mobile layout from HTML structure. This is not a full JavaScript browser engine: DOM updates such as `innerHTML`, event handlers, fetch/XHR, timers, external JS bundles, canvas, SPA hydration and arbitrary CSS layout are not implemented. JavaScript that depends on those browser APIs will not render correctly. Animations and complex client-side applications are unsupported. CSS grid/flex and exact desktop layout are not emulated. Standard GET forms with named fields and hidden values are supported. Text, password, numeric, URL, email, checkbox, radio and select controls use native MIDP inputs; select options and checked state are carried in TBO1. Radio grouping, POST, file uploads and JavaScript-driven validation are still not equivalent to a full browser.

TBO1 is an original, OBML-inspired format for this project. It is **not** Opera's proprietary OBML format and is not wire-compatible with Opera Mini.

## Legacy image-rendering modes

The repository retains optional legacy `MOB1`/SSRIMG Chromium screenshot rendering for clients that explicitly request it and for supported deployments with Chrome/Chromium installed. That path captures a static mobile view into JPEG tiles with link hitboxes; it is **not used by the default J2ME TBO1 request**. JavaScript does not run on the handset in screenshot mode either. The legacy `X-TGFN-Compact: 1` HTML-simplification mode remains available. Requests without a format header are not converted to TBO1.

## HTTP and HTTPS

- HTTP pages use the normal absolute-URL forward-proxy request path.
- The J2ME client fetches HTTPS pages and HTTPS images through the server-side `/__tgfn/fetch?url=...` endpoint over its phone-to-proxy HTTP socket. The proxy fetches the remote HTTPS resource with certificate verification, follows a limited number of redirects, and applies the requested compact transform.
- The fetch endpoint accepts only HTTP(S), rejects URL credentials, blocks private/loopback/link-local/reserved destinations, validates redirect targets and pins the socket to a validated public IP to reduce SSRF and DNS-rebinding risk.
- This HTTPS endpoint is **server-side fetching, not an end-to-end CONNECT tunnel**. The proxy server can observe the requested URL and response content; use only a trusted proxy. The ordinary HTTP CONNECT handler remains available to clients that can perform TLS themselves.

The proxy has no authentication by default. Restrict it to trusted interfaces/networks or add authentication before remote deployment. The fetch endpoint does not make an otherwise public unauthenticated proxy safe to expose.
