"""Package an approved transparent icon onto Android's 108dp canvas.

Usage: python3 scripts/export-launcher-icon.py transparent-source.png
Requires Pillow; only sizing/encoding is performed, no artwork editing.
"""
from pathlib import Path
import math
import sys
from PIL import Image

root = Path(__file__).resolve().parents[1]
source = Image.open(sys.argv[1]).convert('RGBA')
alpha = source.getchannel('A')
# Ignore imperceptible generation noise when measuring the visible artwork.
bounds = alpha.point(lambda value: 255 if value > 8 else 0).getbbox()
assert bounds, 'Empty source'
source = source.crop(bounds)
cx, cy = (source.width - 1) / 2, (source.height - 1) / 2
alpha = source.getchannel('A')
extent = max(math.hypot(x-cx, y-cy) for y in range(source.height)
             for x in range(source.width) if alpha.getpixel((x,y)) > 8)
# 32dp radius provides 1dp clearance inside the 33dp safe-circle radius.
scale = 128 / extent
source = source.resize((round(source.width*scale), round(source.height*scale)), Image.Resampling.LANCZOS)
canvas = Image.new('RGBA', (432,432))
canvas.alpha_composite(source, ((432-source.width)//2, (432-source.height)//2))
destination = root / 'app/src/main/res/drawable-nodpi/pantrypal_launcher_foreground.webp'
canvas.save(destination, lossless=True, method=6)
print(f'{destination}: {destination.stat().st_size} bytes; foreground {source.size}')
