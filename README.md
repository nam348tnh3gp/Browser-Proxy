# TGFN.BrowserMini

A lightweight Android browser prototype with a Kotlin/WebView client and a standalone Go HTTP forward proxy.

> This is a standards-based proxy browser, not an implementation of Opera Mini's proprietary OBML protocol. The Go proxy must be reachable from the Android device. WebView proxy override support depends on the installed Android System WebView.

## Project layout

- `android-app/` — Android application (Kotlin, WebView, AndroidX WebKit).
- `go-proxy/` — standalone HTTP forward proxy with timeouts and compression negotiation.
- `.github/workflows/build.yml` — builds the Android debug APK and Go proxy on pushes and pull requests.

## Build Android APK

GitHub Actions builds `android-app` automatically. Download the `TGFN-BrowserMini-debug-apk` artifact from the workflow run.

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
- The app lets you enter an HTTP proxy host and port. Use a reachable proxy endpoint; `localhost` refers to the Android device itself.
- HTTPS destinations are forwarded using the standard HTTP CONNECT method. The proxy does not decrypt HTTPS traffic.
- Compression is negotiated with upstream servers; the proxy does not transcode pages or images like Opera Mini's cloud compression system.
- The app uses a WebView browser engine and does not implement Opera Mini's proprietary OBML protocol.

## App identity

- Application ID: `TGFN.BrowserMini`
- Kotlin namespace: `TGFN.BrowserMini`
