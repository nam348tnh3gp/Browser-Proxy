# TGFN.BrowserMini

A lightweight Android browser prototype with a Kotlin/WebView client and a standalone Go HTTP forward proxy.

> **J2ME Upgrade (branch J2ME)**: Comprehensive enhancement of the Go proxy and J2ME client for better page compression. The proxy now uses proper HTML parsing to extract and send fuller web content in a compact TBO1 binary format (inspired by Opera Mini 4.x OBML), preserving more text, links, headings and images while remaining efficient for feature phones. Still full readable web content, not a full modern browser engine.

## Project layout

- `android-app/` — Android application (Kotlin, WebView, AndroidX WebKit).
- `go-proxy/` — standalone HTTP forward proxy with timeouts, HTTPS CONNECT tunneling, and improved server-side compact/TBO1 mode for J2ME (now with net/html for fuller extraction).
- `j2me/` — standalone Java ME/MIDP 2.0 browser for compatible feature phones, with basic images/links, bookmarks, and optional HTTP proxy settings; separate from Android.
- `.github/workflows/build.yml` — builds the Android debug APK and Go proxy on pushes and pull requests.
- `.github/workflows/build-j2me.yml` — independently builds the J2ME development JAR.

## Browser features

- Chrome-inspired address/search bar, reload, back/forward and home controls.
- New-tab page with Google search and quick links to popular sites.
- Multiple WebView tabs with an animated, bottom-sheet-style tab overview, two-column tab cards, active-tab highlight, one-tap close controls, and a quick new-tab action.
- Bookmarks, browsing history, page sharing, and close-tab controls.
- In-page text search with result counts.
- Page zoom controls and pinch-to-zoom without permanent on-screen zoom buttons.
- Desktop-site mode using a desktop browser user agent and wide viewport settings; websites may still adapt their layout independently.
- In-page translation to Vietnamese, English, Japanese, Korean, French, or Spanish without navigating away; the user can restore the original text. Translation uses Google's online translation endpoint and is not offline.
- Optional HTTP proxy configuration; direct browsing remains available when the proxy is disabled.
- Settings for the home page, proxy endpoint, and desktop-site preference.

The browser uses Android System WebView; it is not the full Chromium desktop/mobile browser and does not provide Chrome account sync or extensions.

## J2ME feature-phone version (upgraded)

See [`j2me/README.md`](j2me/README.md) for details. The proxy compresses pages into TBO1 binary blocks for full content delivery on low-resource devices, similar in concept to Opera Mini 4.x.

## Run the Go proxy

```sh
cd go-proxy
go run .
```

By default it listens on `127.0.0.1:8080`. To allow a phone to connect over a trusted LAN, set `PROXY_LISTEN=0.0.0.0:8080` and configure the phone to use the host's LAN address. **Do not expose an unauthenticated forward proxy to the public internet.** Restrict network access with a firewall or add authentication before remote deployment.

## Important limitations

- The Go proxy offers an improved opt-in TBO1 compact mode for the J2ME client that preserves more of the original page content.
- This is not Opera Mini's proprietary cloud rendering/OBML system.
- Use only trusted proxies.
