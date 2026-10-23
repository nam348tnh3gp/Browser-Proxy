# TGFN BrowserMini J2ME

## Why you only see text

The J2ME client has **no HTML/CSS engine**. It can only draw text, links and images.  
That is a hard limit of MIDP 2.0. No amount of proxy changes can make the current client render real CSS layout.

## Closest solution: Screenshot SSR (SSRIMG)

A new optional mode renders the page on the server with headless Chrome and sends a JPEG of the real page (with CSS applied).

**Requirements**
- Chrome or Chromium must be installed on the machine running the proxy.
- The client must request `X-TGFN-Format: SSRIMG`.

**How to try**
1. Install Chrome/Chromium on the proxy machine.
2. In `BrowserMidlet.java` change the header to:
   ```java
   writer.write("X-TGFN-Format: SSRIMG\r\n");
   ```
3. Rebuild the JAR and open a page. You should receive a JPEG screenshot of the rendered page.

## Current default modes

| Mode | Result |
|------|--------|
| SSR (default) | Structured text + links + images |
| FULL | HTML (client still extracts text) |
| SSRIMG | Real rendered JPEG (needs Chrome) |

## Honest limits

- True interactive CSS layout on the phone is not possible with the current client.
- Screenshot mode gives the visual look of the real page but links become a separate list (or you tap the image and use the Links menu).
- Best long-term path for "full web" on old phones is the Opera Mini model: heavy server renderer + very thin client painter.

This project is an open experiment, not affiliated with Opera.
