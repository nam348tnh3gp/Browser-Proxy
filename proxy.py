#!/usr/bin/env python3
"""Compression proxy matching Go proxy structure.
Listens on 8090. Supports /__tgfn/fetch?url= and X-TGFN-Format: TBO1.
"""
import re
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.request import Request, urlopen
from urllib.parse import urlparse, parse_qs, unquote

PORT = 8090
UA = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"

def compress_html(html: bytes) -> bytes:
    text = html.decode("utf-8", errors="ignore")
    text = re.sub(r"(?is)<script[^>]*>.*?</script>", "", text)
    text = re.sub(r"(?is)<style[^>]*>.*?</style>", "", text)
    text = re.sub(r"(?is)<noscript[^>]*>.*?</noscript>", "", text)
    text = re.sub(r">\s+<", "><", text)
    text = re.sub(r"\s{2,}", " ", text)
    return text.encode("utf-8")

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
        fmt = self.headers.get("X-TGFN-Format", "")
        # Match Go proxy remote fetch
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
                body = compress_html(raw) if fmt == "TBO1" else raw
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("X-TGFN-Format", fmt or "raw")
                self.send_header("X-Original-Size", str(len(raw)))
                self.send_header("X-Compressed-Size", str(len(body)))
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)
                print(f"[proxy] fetch {target} format={fmt or 'raw'} {len(raw)} -> {len(body)}")
            except Exception as e:
                msg = f"<h1>Fetch error</h1><pre>{e}</pre>".encode()
                self.send_response(502)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.end_headers()
                self.wfile.write(msg)
            return
        # Simple status
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(b"<h1>proxy.py :8090</h1><p>GET /__tgfn/fetch?url=...</p><p>Header X-TGFN-Format: TBO1</p>")

    def log_message(self, fmt, *args):
        sys.stderr.write("[proxy] " + (fmt % args) + "\n")

if __name__ == "__main__":
    print(f"proxy.py listening on 0.0.0.0:{PORT}")
    print("Endpoint: /__tgfn/fetch?url=  Header: X-TGFN-Format: TBO1")
    HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
