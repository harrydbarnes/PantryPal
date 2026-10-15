"""Validate the padded launcher bitmap and adaptive resource wiring (Pillow required)."""
from pathlib import Path
import xml.etree.ElementTree as ET
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
RES = ROOT / 'app/src/main/res'
image = Image.open(RES / 'drawable-nodpi/pantrypal_launcher_foreground.webp')
assert image.size == (432, 432)
assert image.mode == 'RGBA'
alpha = image.getchannel('A')
assert alpha.getextrema() == (0, 255)
centre = (image.width - 1) / 2
radius = 33 * image.width / 108
for y in range(image.height):
    for x in range(image.width):
        if alpha.getpixel((x, y)) > 8:
            assert (x-centre)**2 + (y-centre)**2 <= radius**2, (x, y)
for name in ('ic_launcher', 'ic_launcher_round'):
    root = ET.parse(RES / f'mipmap-anydpi-v26/{name}.xml').getroot()
    assert [child.tag for child in root] == ['background', 'foreground', 'monochrome']
    for child in root:
        reference = child.attrib['{http://schemas.android.com/apk/res/android}drawable']
        assert (RES / (reference[1:] + '.xml')).is_file(), reference
for xml in RES.glob('drawable/ic_launcher*.xml'):
    ET.parse(xml)
print('PASS: RGBA transparency, 432px canvas, 66dp safe circle and adaptive XML references')
