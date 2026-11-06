# Lumina brand exports

- `lumina-brand-mark-4k.png` is a 4096 × 4096 Lanczos export of the existing 1024 × 1024 transparent source in `app/src/main/res/drawable-nodpi/lumina_brand_mark_color.png`.
- The 4K file keeps the original artwork and alpha; upscaling does not invent vector detail. The Android splash uses the 1024 px source because it is already comfortably above the device-pixel size at the displayed 252 dp.
- Launcher foreground, monochrome mask, density-specific fallback icons, and `assets/lumina-icon-final.png` now use this same multicolour Lumina mark rather than the former terracotta-and-white symbol.
