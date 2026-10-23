# TGFN.BrowserMini

A lightweight Android browser prototype with a Kotlin/WebView client and a standalone Go HTTP forward proxy.

> This is a standards-based proxy browser, not an implementation of Opera Mini's proprietary OBML protocol. The Go proxy must be reachable from the Android device. WebView proxy override support depends on the installed Android System WebView.

## Project layout

- `android-app/` — Android application (Kotlin, WebView, AndroidX WebKit).
- `go-proxy/` — standalone HTTP forward proxy with timeouts and compression negotiation.
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

## Build Android APK

GitHub Actions builds `android-app` automatically. Open [Actions](https://github.com/nam348tnh3gp/Browser-Proxy/actions), select the latest successful **Build TGFN.BrowserMini** run, then download the `TGFN-BrowserMini-debug-apk` artifact from the **Artifacts** section. Extract the ZIP to get the debug APK. Failed runs do not upload an APK.

To build locally with Gradle 8.9 and JDK 17:

```sh
gradle -p android-app assembleDebug
```

## J2ME feature-phone version

A separate Java ME client is available in [`j2me/`](j2me/). It targets the MIDP 2.0 / CLDC 1.1 API and provides address/search entry, basic HTML text, clickable links, up to three small images, bookmarks, back/forward/home/reload controls, and persistent optional HTTP proxy settings. It remains separate from the Android app; when configured, HTTP requests can use the Go forward proxy.

GitHub Actions builds a development JAR in the **Build TGFN BrowserMini J2ME** workflow. Open [Actions](https://github.com/nam348tnh3gp/Browser-Proxy/actions), select that workflow's successful run, and download the `TGFN-BrowserMini-J2ME` artifact.

**Compatibility note:** J2ME runs only on phones with a compatible Java ME runtime. Many newer KaiOS and other feature phones do not include J2ME. This is a lightweight browser, not a modern JavaScript/CSS engine. The configured proxy currently handles HTTP only; HTTPS uses the phone's direct TLS connection because portable TLS-over-CONNECT is not available in standard MIDP. Read [`j2me/README.md`](j2me/README.md) for build and handset limitations.

## Run the Go proxy

```sh
cd go-proxy
go run .
```

By default it listens on `127.0.0.1:8080`. To allow a phone to connect over a trusted LAN, set `PROXY_LISTEN=0.0.0.0:8080` and configure the phone to use the host's LAN address. **Do not expose an unauthenticated forward proxy to the public internet.** Restrict network access with a firewall or add authentication before remote deployment.

## Important limitations

- WebView proxy override is a WebView feature and may not be available on every device/WebView version.
- Open **Settings** in the app to enable/disable a proxy and enter its URL, e.g. `http://192.168.1.10:8080`; the setting is saved across launches. Use a reachable HTTP proxy endpoint; `localhost` refers to the Android device itself. Proxy URLs with paths, credentials, queries, or fragments are not accepted.
- The Go proxy supports HTTPS tunneling with standard HTTP CONNECT and does not decrypt HTTPS traffic. WebView proxy override must be supported by the device's WebView provider.
- Compression is negotiated with upstream servers; the proxy does not transcode pages or images like Opera Mini's cloud compression system.
- In-page translation currently translates up to 36 visible text nodes per action. Dynamic content, complex pages, protected text, and some site layouts may not translate perfectly; network access to Google's translation endpoint is required.
- The app uses a WebView browser engine and does not implement Opera Mini's proprietary OBML protocol.

## App identity

- Application ID: `TGFN.BrowserMini`
- Kotlin namespace: `TGFN.BrowserMini`
