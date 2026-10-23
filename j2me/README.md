# TGFN BrowserMini J2ME

Lightweight Java ME browser with a server-side compression + rendering proxy.

## SSR mode (Server-Side Rendering)

The client now requests **SSR** by default.

- Proxy fetches the page, parses HTML, extracts structured content (headings, text, lists, links, images).
- Images are resized and JPEG-compressed.
- Everything is packed into a compact binary format (`SSR1`).
- Client paints the blocks with visual hierarchy (larger headings, underlined links, images).

This is a pure-Go server-side layout extraction (no Chrome required). It is closer to Opera Mini's idea than plain HTML dumping, but it is still not a full Presto-style pixel layout engine.

## Modes available

| Mode | Header | What you get |
|------|--------|--------------|
| **SSR** (default) | `X-TGFN-Format: SSR` | Structured rendered blocks + compressed images |
| FULL | `X-TGFN-Format: FULL` | Almost full HTML + gzip (client still extracts text) |
| TBO1 | `X-TGFN-Format: TBO1` | Older compact binary |

## How to use

1. Run the proxy:
   ```bash
   cd go-proxy
   PROXY_LISTEN=0.0.0.0:8080 go run .
   ```
2. In the J2ME app: Menu → Proxy → enter LAN IP + port 8080 → enable.
3. Open a page. The proxy will server-side render it.

## Limits

- No real CSS positioning (no absolute/float layout)
- No JavaScript execution
- Best results on text-friendly or mobile sites
- Up to 8 images per page

Inspired by Opera Mini, not affiliated with Opera.
