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
| **TBO1** (default for J2ME) | Chromium-free HTML parsing into compact binary semantic blocks: headings, paragraphs, lists, links, images, tables, and native MIDP form controls. The proxy requests mobile HTML when sites provide it. |
| **MOB1** | Legacy Chromium mobile screenshot mode; not used by the default J2ME client. |
| SSR | Structured text blocks |
| FULL | HTML + gzip |
| SSRIMG | Optional screenshot mode for proxy-side clients that explicitly request it (requires Chrome/Chromium). |

## Compact mobile document protocol

1. The J2ME client requests TBO1; the proxy does not start Chromium.
2. The proxy asks the website for its mobile HTML variant, parses returned HTML into bounded semantic records, resolves links and image URLs, and encodes them in the compact TBO1 binary envelope.
3. The MIDP client lays out text and links to its own screen width. Form field labels/types are shown inline in the document flow; selecting a field opens the native Java ME form editor for editing and GET submission. It can navigate without downloading screenshot tiles or decoding large page images.
4. Images are fetched and resized separately by the proxy. CSS and JavaScript are not executed; the renderer reconstructs a readable mobile layout from HTML structure rather than reproducing arbitrary browser CSS.

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
On MOB1 pages, use the directional keys to focus links in place and FIRE/5 to open; no duplicate page-link list is shown. TBO1 fallback pages retain **Menu → Page links**.

## Requirements for SSRIMG

- Chrome or Chromium on the proxy machine (Termux path is supported)

## Limits

- The default TBO1 mode does not use Chromium and does not execute JavaScript. It parses HTML and rebuilds a mobile-friendly semantic layout, so complex CSS grid/flex, arbitrary CSS positioning, and pixel-perfect browser rendering are not reproduced.
- Standard GET forms with named fields and hidden values are supported. Text, password, numeric, URL, email, checkbox, radio and select controls map to native MIDP inputs; select options and checked state are carried in TBO1.
- Radio groups are simplified; POST, file uploads, JavaScript-driven validation and advanced client-side behavior are not browser-equivalent.
- Mobile variants depend on the website honoring a mobile User-Agent. If it does not, the proxy still reflows the returned HTML structure for the narrow handset screen.
- Best on text-friendly or mobile sites

Not affiliated with Opera.
