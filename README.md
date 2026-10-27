# TGFN.BrowserMini

[![Build and Release](https://github.com/nam348tnh3gp/Browser-Proxy/actions/workflows/build.yml/badge.svg)](https://github.com/nam348tnh3gp/Browser-Proxy/actions/workflows/build.yml)
[![Go Proxy Tests](https://github.com/nam348tnh3gp/Browser-Proxy/actions/workflows/proxy-test.yml/badge.svg)](https://github.com/nam348tnh3gp/Browser-Proxy/actions/workflows/proxy-test.yml)
[![Latest Release](https://img.shields.io/github/v/release/nam348tnh3gp/Browser-Proxy?display_name=tag&label=latest%20release)](https://github.com/nam348tnh3gp/Browser-Proxy/releases/latest)
[![Release Downloads](https://img.shields.io/github/downloads/nam348tnh3gp/Browser-Proxy/total?label=downloads)](https://github.com/nam348tnh3gp/Browser-Proxy/releases)
[![Repo Size](https://img.shields.io/github/repo-size/nam348tnh3gp/Browser-Proxy?label=repo%20size)](https://github.com/nam348tnh3gp/Browser-Proxy)
[![Last Commit](https://img.shields.io/github/last-commit/nam348tnh3gp/Browser-Proxy?label=last%20commit)](https://github.com/nam348tnh3gp/Browser-Proxy/commits/main)
[![Open Issues](https://img.shields.io/github/issues/nam348tnh3gp/Browser-Proxy?label=issues)](https://github.com/nam348tnh3gp/Browser-Proxy/issues)


A lightweight Android browser prototype with a Kotlin/WebView client and a standalone Go HTTP forward proxy.

> This is a standards-based proxy browser, not an implementation of Opera Mini's proprietary OBML protocol. The Go proxy must be reachable from the Android device. WebView proxy override support depends on the installed Android System WebView.

## Project layout

- `android-app/` — Android application (Kotlin, WebView, AndroidX WebKit).
- `go-proxy/` — standalone HTTP forward proxy with timeouts and HTTPS CONNECT tunneling.
- `.github/workflows/build.yml` — manually triggered signed Android APK release workflow; publishes a GitHub Release and then prepares the next version.
- [J2ME branch](https://github.com/nam348tnh3gp/Browser-Proxy/tree/J2ME) — separate Java ME/MIDP 2.0 feature-phone browser and compact-page proxy integration.

## Browser features

- Chrome-inspired address/search bar, reload, back/forward and home controls.
- Lightweight WebView optimization layer that centralizes rendering settings, avoids off-screen pre-raster work for hidden tabs when supported, and deprioritizes hidden-tab renderer work on Android 8+; it keeps Android System WebView/Chromium as the actual browser engine.
- New-tab page with Google search and quick links to popular sites.
- Multiple WebView tabs with an animated, bottom-sheet-style tab overview, two-column tab cards, active-tab highlight, one-tap close controls, and a quick new-tab action.
- Managed popup links (`target="_blank"` and `window.open()`), tab groups, close-other-tabs actions, and recently closed-tab restore.
- Built-in download manager with progress/status, opening completed files, retrying failed downloads, and a shortcut to Android's system download manager.
- Bookmarks, browsing history, page sharing, and close-tab controls.
- In-page text search with result counts.
- Page zoom controls and pinch-to-zoom without permanent on-screen zoom buttons.
- Desktop-site mode using a desktop browser user agent and wide viewport settings; websites may still adapt their layout independently.
- In-page translation to Vietnamese, English, Japanese, Korean, French, or Spanish without navigating away; the user can restore the original text. Translation uses Google's online translation endpoint and is not offline.
- Optional HTTP proxy configuration; direct browsing remains available when the proxy is disabled.
- Optional Data Saver toggle that keeps images and network access enabled, uses WebView cache validation, and persists across launches. The Go proxy can gzip eligible small text responses when the client advertises gzip. The **Menu → Data Saver analytics** screen shows this installation's measured counters and whole-proxy totals for response bytes, HTTPS tunnel bytes, gzip savings, and registered client IDs. The Go proxy periodically persists counters to `proxy-stats.json` by default (or `PROXY_STATS_FILE` if set), so counters can survive a restart when that file remains available and writable. The client ID is generated once and persisted, not generated per request. Per-device attribution now normalizes IP formats and handles a single registered client across address changes; if multiple clients share one source IP, the UI reports that attribution is ambiguous rather than silently assigning traffic to the wrong device.
- Privacy and site-data controls for clearing WebView cache, cookies/sign-in sessions, website storage, browsing history, or all browsing data. Third-party cookies can be allowed or blocked and the choice is applied to existing and newly created tabs.
- Settings for the home page, proxy endpoint, and desktop-site preference.

The browser uses Android System WebView; it is not the full Chromium desktop/mobile browser and does not provide Chrome account sync or extensions.

## Build and release Android APK

Release builds are **manual only**; pushes and pull requests do not start these workflows. To publish a signed release:

1. Configure all four signing secrets described below.
2. Open [Actions → Build and Release TGFN.BrowserMini](https://github.com/nam348tnh3gp/Browser-Proxy/actions/workflows/build.yml).
3. Select **Run workflow** on `main`.
4. Wait for the build, APK signature verification, and GitHub Release publication to finish.
5. Download the APK from [GitHub Releases](https://github.com/nam348tnh3gp/Browser-Proxy/releases) or from the run's **Artifacts** section.

The workflow reads `versioncode.txt` and `versionname.txt` as the source of truth and checks that `android-app/app/build.gradle.kts` matches them. The Release tag is `v<versionName>`. Only after publishing the Release successfully does the workflow increment the version code and patch version, update all three version files, and commit the next development version to `main`. A failed build or failed Release does not run the version-bump step. Start a new workflow run rather than rerunning a completed run.

To build locally with Gradle 8.9 and JDK 17:

```sh
gradle -p android-app assembleDebug
```

### Signed APK and automatic versioning

The release workflow increments Android `versionCode` and the patch part of `versionName` only after a successful GitHub Release. It commits the updated `versioncode.txt`, `versionname.txt`, and `android-app/app/build.gradle.kts` to `main` together. The current version files must already match before a release can begin.

To create the signing key in Termux (OpenJDK 21 is fine for generating the key; CI builds with JDK 17):

```sh
pkg install openjdk-21 coreutils
mkdir -p ~/tgfn-signing
cd ~/tgfn-signing
keytool -genkeypair -v \
  -keystore tgfn-browser-release.jks \
  -alias tgfn-browser \
  -keyalg RSA -keysize 4096 -validity 10000
base64 tgfn-browser-release.jks | tr -d '\\n' > tgfn-browser-release.base64
```

Keep the `.jks` file and passwords private and back them up securely. **Never commit the keystore or its passwords to Git.** If you lose the keystore, you generally cannot publish an update over an app installed with that signing key.

In GitHub, open **Settings → Secrets and variables → Actions → New repository secret** and add these four secrets:

- `ANDROID_KEYSTORE_BASE64`: contents of `~/tgfn-signing/tgfn-browser-release.base64`
- `ANDROID_KEYSTORE_PASSWORD`: the keystore password chosen in `keytool`
- `ANDROID_KEY_ALIAS`: `tgfn-browser`
- `ANDROID_KEY_PASSWORD`: the key password chosen in `keytool` (it can match the keystore password)

After all four secrets are set, the workflow builds and publishes a signed release APK. If any signing secret is missing or the keystore cannot be validated, the release workflow fails; it does not fall back to an unsigned APK. Use the same keystore and alias for every update of this app.

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
- The Go proxy supports HTTPS tunneling with standard HTTP CONNECT and does not decrypt HTTPS traffic. WebView proxy override must be supported by the device's WebView provider. Client traffic attribution currently uses the source IP registered by the app because WebView's proxy override cannot attach a custom ID header to every request/tunnel; devices sharing one public IP may be conflated, so registered IDs are not always equal to physical devices.
- Data Saver keeps images and network access enabled and relies on WebView cache validation; it does not force expired cached HTML/JavaScript. Separately, the Go proxy compresses eligible HTTP text responses with gzip only when the client advertises gzip, the response is a successful GET without a Range request, the body is at most 8 MiB, and compression reduces the size. Already encoded content, non-text media, and `Cache-Control: no-transform` responses are skipped. HTTPS CONNECT traffic remains encrypted and untouched. This is not Chrome Lite: it does not transcode images, rewrite arbitrary pages, or guarantee a particular savings percentage.
- In-page translation currently translates up to 36 visible text nodes per action. Dynamic content, complex pages, protected text, and some site layouts may not translate perfectly; network access to Google's translation endpoint is required.
- The app uses a WebView browser engine and does not implement Opera Mini's proprietary OBML protocol.

## App identity

- Application ID: `TGFN.BrowserMini`
- Kotlin namespace: `TGFN.BrowserMini`
