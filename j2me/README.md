# TGFN BrowserMini J2ME (Opera Mini 4.x-style)

Lightweight Java ME browser that uses a server-side proxy to compress pages (text + images) before sending them to the phone — similar in concept to Opera Mini 4.x.

## New in this update

- **Server-side image compression**: Images are automatically resized (max 240px wide) and re-encoded as JPEG quality 60 via `/__tgfn/img`. Pages become much lighter and load faster on feature phones.
- **Fuller content extraction** with `golang.org/x/net/html` (more text, headings, lists, links kept).
- **TBO1 binary protocol**: Compact block format inspired by Opera Mini's OBML. Client receives structured content instead of raw HTML.
- Higher capacity (up to 2500 blocks).

## How it works (like Opera Mini)

1. J2ME client requests a page through the proxy.
2. Proxy fetches the real page, extracts content, compresses images, and packs everything into a small TBO1 binary.
3. Client renders the blocks (title, text, headings, links, compressed images).

Result: lighter pages and a more usable web view on old phones than plain HTML dumping.

## Usage

1. Run the Go proxy (`PROXY_LISTEN=0.0.0.0:8080 go run .` in `go-proxy/`).
2. In the J2ME app: Menu → Proxy → enter LAN IP + port 8080 → enable.
3. Open any HTTP/HTTPS site. The proxy will compress it.

**Important**: Use only a trusted proxy. The proxy can see requested URLs and content.

## Limitations (honest)

- Still not a full layout engine (no real CSS positioning like Opera Mini's Presto server renderer).
- Client currently shows a structured page (text + links + images) rather than pixel-perfect layout.
- JavaScript is not executed on the server.
- Best results on text-friendly or mobile pages.

This is an open re-implementation of the Opera Mini idea, not affiliated with Opera.
