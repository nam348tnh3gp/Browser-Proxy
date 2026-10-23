# TGFN BrowserMini J2ME (Upgraded Opera Mini 4.x-style)

Enhanced lightweight Java ME browser with improved server-side compression proxy.

## Key upgrades (v2.1)

- **Fuller content extraction**: Proxy now uses proper HTML parsing (golang.org/x/net/html) instead of regex, preserving more text, headings, lists, links and images from modern pages.
- **Higher capacity**: Up to 2500 content blocks, more images supported on client (increased from 3).
- **TBO1 protocol**: Binary compact format inspired by Opera Mini's OBML — compresses full page content into small blocks for slow/low-memory J2ME devices.
- **HTTPS via proxy**: Server-side fetch endpoint handles TLS for devices without portable CONNECT+TLS.
- Still lightweight: No full CSS/JS engine (like original Opera Mini 4.x server-side model), but much closer to delivering complete readable web content.

## Usage

Enable proxy in the app, point to your Go proxy (LAN IP:8080). Pages are fetched, compressed to TBO1, and rendered with text, clickable links, and images.

**Note**: This is an open re-implementation of the Opera Mini 4.x proxy+client idea for J2ME. Not affiliated with Opera. Use only trusted proxies.

## Original features retained

- Address entry and web search (DuckDuckGo Lite).
- Compact page view with page title/address, clickable HTML links and images.
- Back/forward/home/reload, bookmarks and exit commands.
- Persistent proxy settings (host, port and enable toggle) stored in Java ME RMS.
- Response/image size caps to reduce memory pressure on older devices.
