#!/usr/bin/env python3
"""Test client matching J2ME browser request style.
Listens on 8081. Sends X-TGFN-Format: TBO1 to proxy on 8090.
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
            <p>Sends requests like the J2ME client (X-TGFN-Format: TBO1)</p>
            <form>
              URL: <input name="url" size="50" value="https://example.com">
              <button>Open via proxy</button>
            </form>
            <p>Proxy: 127.0.0.1:8090</p>
            </body></html>"""
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(html.encode())
            return
        try:
            # Same path style as J2ME client remote fetch
            proxy_url = PROXY + "/__tgfn/fetch?url=" + quote(target, safe="")
            req = Request(proxy_url, headers={
                "User-Agent": "TGFN-BrowserMini-J2ME/2.1",
                "X-TGFN-Format": "TBO1",
                "Accept": "application/x-tgfn-obml,text/html,*/*",
            })
            with urlopen(req, timeout=30) as resp:
                body = resp.read()
                orig = resp.headers.get("X-Original-Size", "?")
                comp = resp.headers.get("X-Compressed-Size", "?")
                fmt = resp.headers.get("X-TGFN-Format", "?")
            info = (
                f"<div style='background:#eef;padding:8px;font-family:sans-serif'>"
                f"test.py → proxy:8090 | HTML minify | {orig} → {comp} bytes</div>\n"
            )
            out = info.encode() + body
            self.send_response(200)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.send_header("Content-Length", str(len(out)))
            self.end_headers()
            self.wfile.write(out)
            print(f"[test] {target} format=TBO1 via proxy, {len(body)} bytes")
        except Exception as e:
            msg = f"<h1>Error</h1><pre>{e}</pre>".encode()
            self.send_response(502)
            self.send_header("Content-Type", "text/html; charset=utf-8")
            self.end_headers()
            self.wfile.write(msg)

    def log_message(self, fmt, *args):
        sys.stderr.write("[test] " + (fmt % args) + "\n")

if __name__ == "__main__":
    print(f"test.py listening on 0.0.0.0:{PORT}")
    print(f"Talks to proxy at {PROXY} with X-TGFN-Format: TBO1")
    HTTPServer(("0.0.0.0", PORT), Handler).serve_forever()
