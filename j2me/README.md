# TGFN BrowserMini J2ME

A small Java ME browser companion for feature phones that support **MIDP 2.0 / CLDC 1.1**. This lives separately from the Android app and Go proxy.

## What it does

- Address entry and web search (DuckDuckGo Lite).
- Compact page view with page title/address, clickable HTML links and up to three small inline images.
- Address entry and search, back/forward/home/reload, bookmarks and exit commands.
- Persistent proxy settings (host, port and enable toggle) stored in Java ME RMS.
- HTTP forward-proxy requests compatible with the Go proxy, with an opt-in server-side compact-page mode for HTML.
- Response/image size caps to reduce memory pressure on older devices.
- Proxy remains optional; direct browsing works when it is disabled.

## Important compatibility notes

This is a lightweight browser, **not a full HTML/CSS/JavaScript engine**. It renders readable text, simple links and a small number of images; it does not implement CSS layout, JavaScript, modern web apps, video or complex forms. Modern sites may be too heavy, block old clients, or require JavaScript. Try mobile/lightweight pages such as DuckDuckGo Lite or other text-friendly websites.

## Proxy

Open **Proxy** from the browser menu and enter the proxy hostname/IP, port (for the bundled Go proxy the default is `127.0.0.1:8080`), and enable the toggle. Settings are saved in Java ME RMS. On a phone, `127.0.0.1` means the phone itself; use the LAN address or reachable host of the machine running the proxy if it is not running on the phone.

**Server-side compact mode:** HTTP page requests through the bundled Go proxy send `X-TGFN-Compact: 1`. The proxy strips scripts/styles, embedded media, navigation/footer regions and unsupported attributes, then returns simplified HTML for the small-screen renderer. Basic links and image URLs are preserved. This is payload reduction and HTML simplification, not Opera Mini's proprietary cloud renderer/OBML protocol; it does not resize or recompress image bytes. It is opt-in and other proxy clients are unaffected.

**Protocol limitation:** configured proxying currently applies to HTTP page and image requests. HTTPS pages and HTTPS images use the handset's direct secure connection. Standard MIDP does not provide a portable way to layer TLS over an arbitrary HTTP CONNECT socket, so this build does not claim that HTTPS is tunneled through the proxy. The Go proxy supports CONNECT, but this J2ME client does not yet have a portable TLS-over-CONNECT implementation. Do not enter proxy credentials or sensitive data into an untrusted proxy.

HTTPS availability depends on the handset's TLS versions, root certificates, network configuration and operator. Some older phones can only connect to older TLS servers; this app does not weaken TLS or bypass certificate validation. If a phone cannot establish HTTPS, use a trusted HTTP endpoint where appropriate or update the device firmware/certificates if supported.

A phone must actually support Java ME MIDP 2.0. Some newer feature phones (including many KaiOS devices) do not support J2ME and cannot install this build. The produced JAR should be tested on target handsets; vendor-specific Java ME implementations differ.

## Build

From this directory with JDK and Maven installed:

```sh
mvn --batch-mode clean package
```

The artifact is `target/TGFN-BrowserMini-J2ME.jar`. The project uses MicroEmu's CLDC API for compilation; it is a compile-time API dependency, not bundled into the application.

For physical deployment, Java ME devices commonly require CLDC-compatible bytecode and preverifying. The GitHub Actions artifact is a development build and must be validated on the intended handset or through a suitable Java ME SDK/preverification toolchain before treating it as a universal legacy-phone installer.
