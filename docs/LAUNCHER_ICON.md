# PantryPal launcher icon

The illustrated cream grocery tote, green leaves and orange use separate adaptive
layers. The repaired left handle continues behind the leaves to the bag rim.

- Canvas: 108 × 108dp; lossless RGBA WebP at 432 × 432px (4×).
- Foreground: `drawable-nodpi/pantrypal_launcher_foreground.webp`, with transparent padding.
- Background: opaque, full-bleed warm oat `#F4F0E8`, not baked into the foreground.
- Artwork including its soft edge is fitted inside a 64dp-diameter circle,
  leaving clearance within Android's 66dp safe circle. Do not enlarge blindly.
- Both standard and round launcher resources use identical adaptive layers.
- Monochrome: a separate 108dp vector silhouette for Android 13+ themed icons.
- No baked-in circle, squircle mask, border, or text.
- No legacy density icons are needed because the app's minimum SDK is 26.

The foreground was extracted from the approved light-background illustration using
the built-in image editor with this brief: remove only the oat background; retain
the tote, orange, leaves, soft texture and repaired left handle; use real alpha.
`scripts/export-launcher-icon.py` performs deterministic sizing and lossless encoding
of the transparent source. `scripts/check-launcher-icon.py` checks alpha, dimensions,
the safe circle and resource wiring (both scripts require Pillow).

Android guidance: https://developer.android.com/develop/ui/compose/system/icon_design_adaptive

After installing a new build, some launchers may need restarting to refresh cached
icons. Enabling themed icons intentionally shows the simplified monochrome design.
