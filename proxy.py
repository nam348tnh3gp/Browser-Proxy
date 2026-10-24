#!/usr/bin/env python3
"""Compression proxy aligned with Go proxy.
Port 8090. Endpoint /__tgfn/fetch?url=  Header X-TGFN-Format: TBO1
Returns TBO1 binary (same magic and content type as Go).
"""
import re
import struct
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.request import Request, urlopen
from urllib.parse import urlparse, parse_qs, unquote

PORT = 8090
UA = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"

def strip_tags(html: str) -> str:
    text = re.sub(r"(?is)<script[^>]*>.*?</script>", " ", html)
    text = re.sub(r"(?is)<style[^>]*>.*?</style>", " ", text)
    text = re.sub(r"<[^>]+>", " ", text)
    text = re.sub(r"\s+", " ", text)
    return text.strip()

def extract_links(html: str, base: str):
    links = []
    for m in re.finditer(r'(?is)<a\s[^>]*href=["\']([^"\']+)["\'][^>]*>(.*?)</a>', html):
        href, label = m.group(1), re.sub(r"<[^>]+>", "", m.group(2)).strip()
        if label and href.startswith("http"):
            links.append((label[:80], href[:200]))
        if len(links) >= 40:
            break
    return links

def build_tbo1(title: str, text: str, links):
    """Minimal TBO1 packet: magic + count + blocks (kind, text, url, dx, dy, w, h)."""
    blocks = []
    # kind 0 = text
    for chunk in text.split(". ")[:30]:
        chunk = chunk.strip()
        if chunk:
            blocks.append((0, chunk[:200], ""))
    for label, href in links:
        blocks.append((4, label, href))  # kind 4 = link
    out = bytearray()
    out += b"TBO1"
    out += struct.pack(">H", len(blocks))
    for kind, t, u in blocks:
        out.append(kind & 0xFF)
        tb, ub = t.encode("utf-8"), u.encode("utf-8")
        out += struct.pack(">H", len(tb)) + tb
        out += struct.pack(">H", len(ub)) + ub
        out += struct.pack(">hhhh", 0, 0, 0, 0)  # delta pos
    return bytes(out)

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        fmt = self.headers.get("X-TGFN-Format", "")
        if parsed.path == "/__tgfn/fetch":
            qs = parse_qs(parsed.query)
            target = qs.get("url", [None])[0]
            if not target:
                self.send_error(400, "missing url")
                return
            target = unquote(target)
            if not target.startswith("http"):
                target = "https://" + target
            try:
                req = Request(target, headers={"User-Agent": UA})
                with urlopen(req, timeout=20) as resp:
                    raw = resp.read()
                html = raw.decode("utf-8", errors="ignore")
                if fmt == "TBO1":
                    text = strip_tags(html)
                    links = extract_links(html, target)
                    body = build_tbo1(target, text, links)
                    ctype = "application/x-tgfn-obml; version=1"
                else:
                    body = raw
                    ctype = "text/html; charset=utf-8"
                self.send_response(200)
                self.send_header("Content-Type", ctype)
                self.send_header("X-TGFN-Format", fmt or "raw")
                self.send_header("X-Original-Size", str(len(raw)))
                self.send_header("X-Compressed-Size", str(len(body)))
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)
                print(f"[proxy] {target} format={fmt or 'raw'} {len(raw)} -> {len(body)}")
            except Exception as e:
                msg = f"error: {e}".encode()
                self.send_response(502)
                self.send_header("Content-Type", "text/plain")
                self.end_headers()
                self.wfile.write(msg)
            return
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(b"<h1>proxy.py :8090</h1><p>/__tgfn/fetch?url=  X-TGFN-Format: TBO1</p>")

    def log_message(self, fmt, *args):
        sys.stderr.write("[proxy] " + (fmt % args) + "\n")

if __name__ == "__main__":
    print(f"proxy.py listening on 0.0.0.0:{PORT}")
    print("Aligned with Go: /__tgfn/fetch + X-TGFN-Format: TBO1 -> application/x-tgfn-obml")
    HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
