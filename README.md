# TGFN.BrowserMini

A lightweight Android browser prototype with a Kotlin/WebView client and a standalone Go HTTP forward proxy.

> This is a standards-based proxy browser, not an implementation of Opera Mini's proprietary OBML protocol. The Go proxy must be reachable from the Android device. WebView proxy override support depends on the installed Android System WebView.

## Project layout

- `android-app/` — Android application (Kotlin, WebView, AndroidX WebKit).
- `go-proxy/` — standalone HTTP forward proxy with timeouts and HTTPS CONNECT tunneling.
- `.github/workflows/build.yml` — builds the Android debug APK and Go proxy on Android/proxy changes.
- [J2ME branch](https://github.com/nam348tnh3gp/Browser-Proxy/tree/J2ME) — separate Java ME/MIDP 2.0 feature-phone browser and compact-page proxy integration.

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
- Optional Data Saver toggle that keeps images and network access enabled, uses WebView's normal cache validation to avoid stale-page failures, and persists across launches.
- Settings for the home page, proxy endpoint, and desktop-site preference.

The browser uses Android System WebView; it is not the full Chromium desktop/mobile browser and does not provide Chrome account sync or extensions.

## Build Android APK

GitHub Actions builds `android-app` automatically. Open [Actions](https://github.com/nam348tnh3gp/Browser-Proxy/actions), select the latest successful **Build TGFN.BrowserMini** run, then download the `TGFN-BrowserMini-debug-apk` artifact from the **Artifacts** section. Extract the ZIP to get the debug APK. Failed runs do not upload an APK.

To build locally with Gradle 8.9 and JDK 17:

```sh
gradle -p android-app assembleDebug
```

## J2ME feature-phone version

The Java ME client has been separated into the dedicated [`J2ME` branch](https://github.com/nam348tnh3gp/Browser-Proxy/tree/J2ME), including its own build workflow and server-side compact HTML integration. Switch to that branch to build the JAR/JAD and read its handset limitations.

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
- Data Saver keeps images and network access enabled and uses WebView's normal cache policy (it does not force expired cached HTML/JavaScript). This safety mode does not provide Chrome-Lite-style server-side page compression, image transcoding, or guaranteed data-savings percentages; real transfer reduction requires optimization support on the proxy/server.
- In-page translation currently translates up to 36 visible text nodes per action. Dynamic content, complex pages, protected text, and some site layouts may not translate perfectly; network access to Google's translation endpoint is required.
- The app uses a WebView browser engine and does not implement Opera Mini's proprietary OBML protocol.

## App identity

- Application ID: `TGFN.BrowserMini`
- Kotlin namespace: `TGFN.BrowserMini`
