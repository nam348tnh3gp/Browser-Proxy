# TGFN BrowserMini J2ME

A small Java ME browser companion for feature phones that support **MIDP 2.0 / CLDC 1.1**. This lives separately from the Android app and Go proxy.

## What it does

- Address entry and web search (DuckDuckGo Lite).
- HTTP/HTTPS GET requests using the phone's Java ME networking stack.
- Text-first HTML rendering with a lightweight parser and a list of basic links.
- Back, forward, home, reload and exit commands.
- Small response cap to reduce memory pressure on older devices.
- No mandatory proxy or companion server.

## Important compatibility notes

This is a text-first browser, **not a full HTML/CSS/JavaScript engine**. Modern sites may be too heavy, block old clients, require JavaScript, or use layouts this renderer cannot represent. Try mobile/lightweight pages such as DuckDuckGo Lite or other text-friendly websites.

HTTPS availability depends on the handset's TLS versions, root certificates, network configuration and operator. Some older phones can only connect to older TLS servers; this app does not weaken TLS or bypass certificate validation. If a phone cannot establish HTTPS, use a trusted HTTP endpoint where appropriate or update the device firmware/certificates if supported.

A phone must actually support Java ME MIDP 2.0. Some newer feature phones (including many KaiOS devices) do not support J2ME and cannot install this build. The produced JAR should be tested on target handsets; vendor-specific Java ME implementations differ.

## Build

From this directory with JDK and Maven installed:

```sh
mvn --batch-mode clean package
```

The artifact is `target/TGFN-BrowserMini-J2ME.jar`. The project uses MicroEmu's CLDC API for compilation; it is a compile-time API dependency, not bundled into the application.

For physical deployment, Java ME devices commonly require CLDC-compatible bytecode and preverifying. The GitHub Actions artifact is a development build and must be validated on the intended handset or through a suitable Java ME SDK/preverification toolchain before treating it as a universal legacy-phone installer.
