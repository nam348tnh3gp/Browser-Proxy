# TGFN BrowserMini J2ME

Lightweight Java ME browser inspired by **Opera Mini** and open-source MIDP browsers.

## Inspiration

- **Opera Mini / OBML** — server-side rendering with Presto, compact binary delivery, link regions.  
  Reference: [grawity/obml-parser](https://github.com/grawity/obml-parser)
- **PicoBrowser / picomidp** — simple cHTML MIDP browser with good key navigation.  
  Reference: [karthikeyann/picomidp](https://github.com/karthikeyann/picomidp)
- Other J2ME clients (GitHub-j2me, J2METube) for proxy patterns and UI ideas.

## Current modes

| Mode | Description |
|------|-------------|
| **MOB1** (default for J2ME) | Mobile viewport render using Chromium when available (240×500, mobile user-agent, responsive CSS/media queries, JPEG compression, and extracted links). Falls back to TBO1 when Chromium is unavailable or rendering fails. |
| **TBO1** | Server-side semantic blocks: compact text, headings, lists, links, images, and native MIDP forms for common GET searches. No browser engine required. |
| SSR | Structured text blocks |
| FULL | HTML + gzip |
| SSRIMG | Optional screenshot mode for proxy-side clients that explicitly request it (requires Chrome/Chromium). |

## Compact layout inspired by OBML research

1. Client requests page with `X-TGFN-Format: MOB1`, including HTTPS pages through `/__tgfn/fetch`.
2. When Chromium is available, the Go proxy opens the page at a 240×500 mobile viewport with mobile device emulation, so responsive CSS/media queries and mobile layout are applied before capture.
3. The viewport is JPEG-compressed and sent in the compact `IMGL` envelope with extracted links; the MIDP client displays the rendered page image and exposes links through keypad navigation and the Links menu.
4. If Chromium is unavailable or rendering fails, the proxy falls back to TBO1 semantic blocks and native MIDP GET forms. TBO1 image references use `/__tgfn/img` for resizing and JPEG compression.
5. This approach prioritizes faithful mobile appearance on devices that cannot run a modern HTML/CSS engine; the rendered image itself is static, while navigation is handled separately through the link list.

## OBML references and compatibility

The implementation borrows design ideas from both reference projects:

- [grawity/obml-parser](https://github.com/grawity/obml-parser): its format notes explain that OBML separates text from link hit regions and that pixel positions depend on the original device's font metrics. TBO1 therefore keeps links as separate semantic records rather than pretending browser text is inherently clickable.
- [MacMailler/obml-renderer](https://github.com/MacMailler/obml-renderer): its OBMLv6 entity model informs the separation of text, images, links, and visual styles in the client/server pipeline.

**Compatibility note:** TBO1 is TGFN's own protocol, not Opera's OBMLv6. Neither reference project supplies an HTML-to-Opera-OBML encoder that can be safely dropped into this Go/MIDP stack. This change does not copy their source code or claim binary OBML compatibility.

## Usage

```bash
cd go-proxy
go mod tidy
PROXY_LISTEN=0.0.0.0:8080 go run .
```

In the app: Menu → Proxy → enable, then open a page.  
Use **Menu → Links** to navigate.

## Requirements for SSRIMG

- Chrome or Chromium on the proxy machine (Termux path is supported)

## Limits

- MOB1 screenshot is static after capture; interaction is through the extracted link list, not pixel-position click mapping.
- MOB1 uses Chromium when available; without it, TBO1 fallback retains readable content and native GET forms.
- Layout preservation is semantic and mobile-first: headings, paragraphs, lists, navigation, footer content, links, and table rows/cells are retained and reflowed for narrow screens. Inline `display:none` / `visibility:hidden` is filtered. External CSS, complex grid/flex positioning, exact colors, and pixel-perfect desktop CSS are not reproduced.
- Common GET forms are supported; POST forms, JavaScript-driven forms, file uploads, and full checkbox/radio/select semantics are not yet browser-equivalent.
- Best on text-friendly or mobile sites

Not affiliated with Opera.
