# TGFN BrowserMini J2ME — Real Render Mode

Client now uses **SSRIMG** by default.

## What SSRIMG does

1. Proxy opens the page in headless Chrome.
2. Chrome renders the real page (full HTML + CSS + layout).
3. Proxy takes a screenshot, compresses it to JPEG, and sends it to the phone.
4. J2ME client displays the JPEG.

This is the closest practical way to show a real web page look on a feature phone.

## Requirements

- **Chrome or Chromium** must be installed on the machine running the Go proxy.
- If Chrome is missing, the request will fail.

## How to run

```bash
cd go-proxy
go mod tidy
PROXY_LISTEN=0.0.0.0:8080 go run .
```

In the J2ME app enable the proxy (LAN IP + 8080), then open a page.

## Notes

- The result is a picture of the page, not an interactive DOM.
- Links are still available via the Links menu in the client.
- Images and layout come from the real rendered page.
- This mode is heavier on the proxy server than the text modes.

Inspired by Opera Mini's server-side rendering idea.
