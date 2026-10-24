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
| **TBO1** (default) | Server-side semantic blocks: compact text, headings, lists, links and resized images. No browser engine required. |
| SSR | Structured text blocks |
| FULL | HTML + gzip |
| SSRIMG | Optional screenshot mode for proxy-side clients that explicitly request it (requires Chrome/Chromium). |

## Compact layout inspired by OBML research

1. Client requests page with `X-TGFN-Format: SSRIMG`
2. Proxy opens the page in headless Chrome
3. Chrome renders full HTML/CSS
4. Proxy captures screenshot + extracts links
5. Sends **IMGL** packet (JPEG + links) to the phone
6. Client shows the image and lets you open links via the **Links** menu

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

- Screenshot is static (no live JavaScript after load)
- Interaction is through the link list (not pixel-perfect click map yet)
- Best on text-friendly or mobile sites

Not affiliated with Opera.
