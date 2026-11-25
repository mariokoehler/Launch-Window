"""Renders the 1.0 launch's poster (2400x3600) and banner (1920x1080) from their HTML pages to PNG.

The pages read their pictures from img/; this copies the picks (picks.json: role -> file, relative to this folder)
there first. Run from anywhere:  python render.py [out-dir]   (needs: pip install playwright; playwright install chromium)
Writes ../launch-window-poster-1.0.png and ../launch-window-banner-1.0.png unless given another folder.
"""
import json
import os
import pathlib
import shutil
import sys

from playwright.sync_api import sync_playwright

HERE = pathlib.Path(__file__).resolve().parent
PAGES = {'poster.html': ('launch-window-poster-1.0.png', 2400, 3600),
         'banner.html': ('launch-window-banner-1.0.png', 1920, 1080)}


def main():
    out = pathlib.Path(sys.argv[1]) if len(sys.argv) > 1 else HERE.parent
    picks = json.loads((HERE / 'picks.json').read_text(encoding='utf-8'))
    img = HERE / 'img'
    img.mkdir(exist_ok=True)
    for role, source in picks.items():
        if not role.startswith('_'):
            shutil.copyfile(HERE / source, img / (role + '.png'))
    with sync_playwright() as p:
        browser = p.chromium.launch(args=['--allow-file-access-from-files'])
        for page_name, (png, width, height) in PAGES.items():
            page = browser.new_page(viewport={'width': width, 'height': height})
            page.goto((HERE / page_name).as_uri())
            page.wait_for_load_state('networkidle')
            # The poster's picture can sit higher or lower (picks.json "_poster_drop", px; negative lifts it).
            page.add_style_tag(content=':root{--drop:%dpx}' % picks.get('_poster_drop', 0))
            page.evaluate('document.fonts.ready')
            page.screenshot(path=str(out / png), full_page=False)
            print('wrote', out / png)
        browser.close()


if __name__ == '__main__':
    main()
