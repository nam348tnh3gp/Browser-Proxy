#!/usr/bin/env python3
"""Proxy on 8090. Returns lightly minified HTML (structure kept).
Endpoint /__tgfn/fetch?url=  — HTML minify beats binary for display.
"""
import re
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.request import Request, urlopen
from urllib.parse import urlparse, parse_qs, unquote

PORT = 8090
UA = "Mozilla/5.0 (Linux; Android 10; Mobile) AppleWebKit/537.36 Chrome/124.0.0.0 Mobile Safari/537.36"

def minify_html(html: bytes) -> bytes:
    text = html.decode("utf-8", errors="ignore")
    # Keep scripts/styles/content. Only collapse whitespace.
    text = re.sub(r">\s+<", "><", text)
    text = re.sub(r"[ \t]{2,}", " ", text)
    return text.encode("utf-8")

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        parsed = urlparse(self.path)
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
                body = minify_html(raw)
                self.send_response(200)
                self.send_header("Content-Type", "text/html; charset=utf-8")
                self.send_header("X-Original-Size", str(len(raw)))
                self.send_header("X-Compressed-Size", str(len(body)))
                self.send_header("Content-Length", str(len(body)))
                self.end_headers()
                self.wfile.write(body)
                print(f"[proxy] {target} html-minify {len(raw)} -> {len(body)}")
            except Exception as e:
                self.send_response(502)
                self.send_header("Content-Type", "text/plain")
                self.end_headers()
                self.wfile.write(str(e).encode())
            return
        self.send_response(200)
        self.send_header("Content-Type", "text/html; charset=utf-8")
        self.end_headers()
        self.wfile.write(b"<h1>proxy.py :8090</h1><p>HTML minify (structure kept)</p>")

    def log_message(self, fmt, *args):
        sys.stderr.write("[proxy] " + (fmt % args) + "\n")

if __name__ == "__main__":
    print(f"proxy.py listening on 0.0.0.0:{PORT} (HTML minify)")
    HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
