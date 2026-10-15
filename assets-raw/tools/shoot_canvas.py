"""Screenshots a Claude Design canvas's artboards: python assets-raw/tools/shoot_canvas.py <saved canvas> <out dir>

<saved canvas> is the folder the Artifact tool's read saves a canvas to (its project/*.dc.html, the design
system under project/ds/, artifact-type/dc-runtime.js). Serves project/ over HTTP and shoots each artboard
at 1920 x 1080 with headless Chromium. Needs pip install playwright and python -m playwright install chromium.
Canvas assets (_blob/) aren't fetched: icons drawn from them come out blank.
"""
import functools
import http.server
import os
import shutil
import sys
import threading

from playwright.sync_api import sync_playwright

base = sys.argv[1]
out = sys.argv[2]
project = os.path.join(base, "project")
shutil.copy(os.path.join(base, "artifact-type", "dc-runtime.js"), os.path.join(project, "support.js"))
os.makedirs(out, exist_ok=True)


class Handler(http.server.SimpleHTTPRequestHandler):
    extensions_map = {**http.server.SimpleHTTPRequestHandler.extensions_map, ".svg": "image/svg+xml",
                      ".js": "text/javascript"}

    def log_message(self, *args):
        pass


server = http.server.ThreadingHTTPServer(("127.0.0.1", 8765), functools.partial(Handler, directory=project))
threading.Thread(target=server.serve_forever, daemon=True).start()
with sync_playwright() as p:
    browser = p.chromium.launch()
    page = browser.new_page(viewport={"width": 1920, "height": 1080 + 87})
    for name in sorted(os.listdir(project)):
        if name.endswith(".dc.html"):
            page.goto(f"http://127.0.0.1:8765/{name}")
            page.wait_for_timeout(2500)
            page.screenshot(path=os.path.join(out, name.replace(".dc.html", ".png")))
            print(name)
    browser.close()
server.shutdown()
