#!/usr/bin/env python3
"""Simple compression proxy on port 8090.
Receives web pages, lightly compresses, returns to client.
Usage: python3 proxy.py
"""
import gzip
import re
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.request import Request, urlopen
from urllib.parse import urlparse, parse_qs

PORT = 8090
UA = "Mozilla/5.0 (Linux; Android 4.4; Mobile) AppleWebKit/537.36"

def compress_html(html: bytes) -> bytes:
    text = html.decode("utf-8", errors="ignore")
    # Strip scripts and styles for lighter payload
    text = re.sub(r"(?is)<script[^>]*>.*?</script>", "", text)
    text = re.sub(r"(?is)<style[^>]*>.*?</style>", "", text)
    text = re.sub(r"(?is)<noscript[^>]*>.*?</noscript>", "", text)
    # Collapse whitespace
    text = re.sub(r">\s+<", "><", text)
    text = re.sub(r"\s{2,}", " ", text)
    return text.encode("utf-8")

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        qs = parse_qs(urlparse(self.path).query)
        target = qs.get("url", [None])[0]
        if not target:
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(b"<h1>proxy.py :8090</h1><p>GET /?url=https://example.com</p>")
            return
        if not target.startswith("http"):
            target = "https://" + target
        try:
            req = Request(target, headers={"User-Agent": UA})
            with urlopen(req, timeout=20) as resp:
                raw = resp.read()
            compressed = compress_html(raw)
            # Optional gzip
            if "gzip" in self.headers.get("Accept-Encoding", ""):
                body = gzip.compress(compressed)
                encoding = "gzip"
            else:
                body = compressed
                encoding = None
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("X-Original-Size", str(len(raw)))
            self.send_header("X-Compressed-Size", str(len(compressed)))
            if encoding:
                self.send_header("Content-Encoding", encoding)
            self.send_header("Content-Length", str(len(body)))
            self.end_headers()
            self.wfile.write(body)
            print(f"[proxy] {target} {len(raw)} -> {len(compressed)}")
        except Exception as e:
            msg = f"<h1>Error</h1><pre>{e}</pre>".encode()
            self.send_response(502)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(msg)

    def log_message(self, fmt, *args):
        sys.stderr.write("[proxy] " + (fmt % args) + "\n")

if __name__ == "__main__":
    print(f"proxy.py listening on 0.0.0.0:{PORT}")
    HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
