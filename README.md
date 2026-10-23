# TGFN.BrowserMini

A lightweight Android browser prototype with a Kotlin/WebView client and a standalone Go HTTP forward proxy.

> This is a standards-based proxy browser, not an implementation of Opera Mini's proprietary OBML protocol. The Go proxy must be reachable from the Android device. WebView proxy override support depends on the installed Android System WebView.

## Project layout

- `android-app/` — Android application (Kotlin, WebView, AndroidX WebKit).
- `go-proxy/` — standalone HTTP forward proxy with timeouts and compression negotiation.
- `.github/workflows/build.yml` — builds the Android debug APK and Go proxy on pushes and pull requests.

## Browser features

- Chrome-inspired address/search bar, reload, back/forward and home controls.
- New-tab page with Google search and quick links to popular sites.
- Multiple WebView tabs with tab switching and closing.
- Bookmarks, browsing history, and page sharing.
- Optional HTTP proxy configuration; direct browsing remains available when the proxy is disabled.
- Settings for the home page and proxy endpoint.

The browser uses Android System WebView; it is not the full Chromium desktop/mobile browser and does not provide Chrome account sync or extensions.

## Build Android APK

GitHub Actions builds `android-app` automatically. Open [Actions](https://github.com/nam348tnh3gp/Browser-Proxy/actions), select the latest successful **Build TGFN.BrowserMini** run, then download the `TGFN-BrowserMini-debug-apk` artifact from the **Artifacts** section. Extract the ZIP to get the debug APK. Failed runs do not upload an APK.

To build locally with Gradle 8.9 and JDK 17:

```sh
gradle -p android-app assembleDebug
```

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
- The app uses a WebView browser engine and does not implement Opera Mini's proprietary OBML protocol.

## App identity

- Application ID: `TGFN.BrowserMini`
- Kotlin namespace: `TGFN.BrowserMini`
