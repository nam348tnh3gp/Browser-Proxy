# TGFN BrowserMini J2ME

A small Java ME browser companion for feature phones that support **MIDP 2.0 / CLDC 1.1**. This lives separately from the Android app and Go proxy.

## What it does

- Address entry and web search (DuckDuckGo Lite).
- Compact page view with page title/address, clickable HTML links and up to three inline images.
- Back/forward/home/reload, bookmarks and exit commands.
- Persistent proxy settings (host, port and enable toggle) stored in Java ME RMS.
- TBO1 compact binary page protocol for server-generated text/link/image blocks.
- With the proxy enabled, routes both HTTP and HTTPS page/image requests through the configured Go proxy. HTTPS is fetched by the server-side `/__tgfn/fetch` endpoint because standard MIDP does not provide a portable TLS-over-CONNECT API.
- Response/image size caps to reduce memory pressure on older devices.
- Proxy remains optional; direct browsing works when it is disabled.

## Important compatibility notes

This is a lightweight browser, **not a full HTML/CSS/JavaScript engine**. It renders readable text, simple links and a small number of images; it does not implement CSS layout, JavaScript, modern web apps, video or complex forms. Modern sites may be too heavy, block old clients, or require JavaScript. Try mobile/lightweight pages such as DuckDuckGo Lite or other text-friendly websites.

## Proxy and TBO1 protocol

Open **Proxy** from the browser menu and enter the proxy hostname/IP, port (for the bundled Go proxy the default is `127.0.0.1:8080`), and enable the toggle. On a phone, `127.0.0.1` means the phone itself; use the LAN address or reachable host of the machine running the proxy if it is not running on the phone.

Page requests send `X-TGFN-Format: TBO1`. The Go proxy extracts the title, paragraphs, headings, list markers, links and image references, removes scripts/styles and selected heavy page regions, then sends typed binary blocks instead of HTML markup. The client validates and decodes those blocks. This is an original OBML-inspired protocol—not Opera's proprietary OBML format and not wire-compatible with Opera Mini.

When proxying is enabled:
- HTTP pages/images use the normal forward-proxy request.
- HTTPS pages/images use `/__tgfn/fetch?url=...`; the phone connects to the configured proxy over HTTP, while the Go proxy establishes and validates the upstream HTTPS connection and verifies the server certificate. Eligible HTML responses are converted to TBO1; image bytes are returned as image resources.
- This is server-side fetching, not an end-to-end HTTPS tunnel. The proxy can see the requested URL and content, so use a trusted proxy only. The Go proxy blocks private/local target addresses and validates redirect targets to reduce server-side request forgery risk.

If the proxy is disabled, the handset connects directly and its own TLS versions, root certificates, network configuration and operator determine HTTPS availability. This app does not weaken TLS or bypass certificate validation.

A phone must actually support Java ME MIDP 2.0. Some newer feature phones (including many KaiOS devices) do not support J2ME and cannot install this build. The produced JAR should be tested on target handsets; vendor-specific Java ME implementations differ.

## Build

From this directory with JDK and Maven installed:

```sh
mvn --batch-mode clean package
```

The artifact is `target/TGFN-BrowserMini-J2ME.jar`. The project uses MicroEmu's CLDC API for compilation; it is a compile-time API dependency, not bundled into the application.

For physical deployment, Java ME devices commonly require CLDC-compatible bytecode and preverifying. The GitHub Actions artifact is a development build and must be validated on the intended handset or through a suitable Java ME SDK/preverification toolchain before treating it as a universal legacy-phone installer.
