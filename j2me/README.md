# TGFN BrowserMini J2ME

## J2ME rendering features

The TBO1 renderer preserves semantic typography from the proxy: heading levels, bold text, italic/emphasis, monospace code, blockquotes, horizontal rules, strike-through, highlighted text, underline, and small/large text. On-device rendering uses MIDP fonts and drawing primitives, with keypad link focus, inline images and compact forms. This is a constrained mobile renderer, not a complete CSS engine or a direct execution of the decompiled Opera Mini source.


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
| **TBO1** (default for J2ME) | Chromium-free HTML parsing into compact binary semantic blocks: headings, bold/italic text, code/pre blocks, blockquotes, horizontal rules, lists, links, images, tables, and native MIDP form controls. The proxy requests mobile HTML when sites provide it. |
| **MOB1** | Legacy Chromium mobile screenshot mode; not used by the default J2ME client. |
| SSR | Structured text blocks |
| FULL | HTML + gzip |
| SSRIMG | Optional screenshot mode for proxy-side clients that explicitly request it (requires Chrome/Chromium). |

## Compact mobile document protocol

1. The J2ME client requests TBO1; the proxy does not start Chromium.
2. The proxy asks the website for its mobile HTML variant, runs a bounded sandboxed subset of inline JavaScript (`document.write()` / `document.writeln()`), parses the resulting HTML into bounded semantic records, resolves links and image URLs, and encodes them in the compact TBO1 binary envelope.
3. The MIDP client lays out text and links to its own screen width. Form field labels/types are shown inline in the document flow; selecting a field opens the native Java ME form editor for editing and GET submission. It can navigate without downloading screenshot tiles or decoding large page images. The JS sandbox has no network, filesystem, browser DOM, timers or event loop; external scripts are skipped and inline execution is bounded by script count, script size and runtime.
4. Images are fetched and resized separately by the proxy, then painted inline at their original positions in the page flow. TBO1 supports regular images, common lazy-load attributes (`data-src`, `data-original`, `srcset`), linked logo images, and simple CSS background images using tag/class/ID selectors. The renderer keeps a readable mobile layout, including semantic italic/code/quote/rule styles and basic centered/right-aligned text inspired by the recovered Opera Mini J2ME source, rather than reproducing every browser CSS rule; external stylesheets, CSS grid/flex and arbitrary positioning are not fully emulated. Only a small inline-JavaScript subset is executed for static HTML generation.

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

- The default TBO1 mode does not use Chromium. It executes only sandboxed inline `document.write()` / `document.writeln()` scripts before parsing HTML; external scripts, DOM mutation APIs, fetch/XHR, timers, event handlers and SPA hydration are not implemented. Complex CSS grid/flex, arbitrary CSS positioning and pixel-perfect browser rendering are not reproduced.
- Standard GET forms with named fields and hidden values are supported. Text, password, numeric, URL, email, checkbox, radio and select controls map to native MIDP inputs; select options and checked state are carried in TBO1.
- Radio groups are simplified; POST, file uploads, JavaScript-driven validation and advanced client-side behavior are not browser-equivalent.
- Mobile variants depend on the website honoring a mobile User-Agent. If it does not, the proxy still reflows the returned HTML structure for the narrow handset screen.
- Best on text-friendly or mobile sites

Not affiliated with Opera.


## FULL mode (structure-preserving mobile HTML)

Client now requests `X-TGFN-Format: FULL` by default.
- Proxy fetches with mobile User-Agent.
- Removes only scripts, external styles, iframes, video, etc.
- Keeps original HTML structure, links, forms, semantic tags and images.
- Makes URLs absolute and routes images through compression.
- Returns minified plain HTML that the J2ME parser can process directly.
