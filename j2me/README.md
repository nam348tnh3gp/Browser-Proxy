# TGFN BrowserMini J2ME

Lightweight Java ME browser with a server-side compression proxy.

## Important: Why you still see mostly text

The J2ME client **does not have a full HTML/CSS engine**.  
Even when the proxy sends mode `FULL` (almost complete HTML + some CSS + links + compressed images), the client extracts the content and draws it as structured text + images on a Canvas.

This is a limitation of MIDP 2.0, not a bug. Real CSS layout (like Opera Mini's Presto server renderer) is not implemented.

## What the current version does

- Proxy can send:
  - **FULL** mode: keeps most HTML structure, links, basic CSS, compresses images, gzips the result.
  - **TBO1** mode: compact binary blocks (lighter).
- Client renders:
  - Title, headings (bold/larger), paragraphs, lists
  - Clickable links
  - Up to 8 compressed images
  - Scrolling Canvas view

## How to get the best result

1. Run the proxy:
   ```bash
   cd go-proxy
   PROXY_LISTEN=0.0.0.0:8080 go run .
   ```
2. In the J2ME app enable the proxy (LAN IP + port 8080).
3. Prefer text-friendly or mobile sites for the best look.

## Current limits

- No real CSS positioning or complex layout
- No JavaScript
- Images are resized and JPEG-compressed on the server
- Best on simple / mobile pages

This is an open project inspired by the Opera Mini idea, not affiliated with Opera.
