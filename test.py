#!/usr/bin/env python3
"""Viewer on port 8081. Fetches via proxy on 8090 and shows result.
Usage: python3 test.py
Open http://127.0.0.1:8081/?url=https://example.com
"""
import sys
from http.server import BaseHTTPRequestHandler, HTTPServer
from urllib.request import Request, urlopen
from urllib.parse import urlparse, parse_qs, quote

PROXY = "http://127.0.0.1:8090"
PORT = 8081

class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        qs = parse_qs(urlparse(self.path).query)
        target = qs.get("url", [None])[0]
        if not target:
            html = """<html><body>
            <h1>test.py :8081</h1>
            <form>
              URL: <input name="url" size="50" value="https://example.com">
              <button>View via proxy</button>
            </form>
            <p>Proxy is at 8090</p>
            </body></html>"""
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(html.encode())
            return
        try:
            proxy_url = PROXY + "/?url=" + quote(target, safe="")
            req = Request(proxy_url)
            with urlopen(req, timeout=30) as resp:
                body = resp.read()
                orig = resp.headers.get("X-Original-Size", "?")
                comp = resp.headers.get("X-Compressed-Size", "?")
            # Wrap with info header
            info = f"<div style='background:#eef;padding:6px'>via proxy 8090 | original {orig} -> compressed {comp} bytes</div>\n"
            out = info.encode() + body
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(out)))
            self.end_headers()
            self.wfile.write(out)
            print(f"[test] {target} via proxy, {len(body)} bytes")
        except Exception as e:
            msg = f"<h1>Error talking to proxy</h1><pre>{e}</pre>".encode()
            self.send_response(502)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(msg)

    def log_message(self, fmt, *args):
        sys.stderr.write("[test] " + (fmt % args) + "\n")

if __name__ == "__main__":
    print(f"test.py listening on 0.0.0.0:{PORT} (proxy at {PROXY})")
    HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
