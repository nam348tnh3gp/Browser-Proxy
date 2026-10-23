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
| **SSRIMG** (default) | Chrome renders the real page → JPEG + link list (IMGL). Closest to Opera Mini visual look. |
| SSR | Structured text blocks |
| FULL | HTML + gzip |
| TBO1 | Compact binary blocks |

## How SSRIMG works (Opera Mini style)

1. Client requests page with `X-TGFN-Format: SSRIMG`
2. Proxy opens the page in headless Chrome
3. Chrome renders full HTML/CSS
4. Proxy captures screenshot + extracts links
5. Sends **IMGL** packet (JPEG + links) to the phone
6. Client shows the image and lets you open links via the **Links** menu

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
