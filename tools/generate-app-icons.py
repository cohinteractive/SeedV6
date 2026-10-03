"""Regenerate checked-in application icons from crowned_seed_emblem.png.

Requires Pillow (as does extract_chess_piece_sheet.py). Run from any directory.
macOS packaging generates its own 16-1024 pixel ICNS using sips and iconutil.
"""

from pathlib import Path

from PIL import Image


ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT / "crowned_seed_emblem.png"
PNG_DIRECTORY = ROOT / "app/src/main/resources/com/ohinteractive/seedv6/gui/icons"
ICO_PATH = ROOT / "tools/icons/seedv6.ico"
SIZES = (16, 20, 24, 32, 40, 48, 64, 96, 128, 256)


def main() -> None:
    with Image.open(SOURCE) as source:
        if source.width != source.height or source.width < max(SIZES):
            raise ValueError("Application artwork must be square and at least 256 pixels.")
        # Keep the full approved artwork, including its background and any alpha.
        artwork = source.convert("RGBA")
    images = [artwork.resize((size, size), Image.Resampling.LANCZOS) for size in SIZES]
    for size, image in zip(SIZES, images):
        image.save(PNG_DIRECTORY / f"seedv6-{size}.png")
    # Embed the same directly resized pixels for every Windows representation.
    images[-1].save(
        ICO_PATH,
        format="ICO",
        sizes=[(size, size) for size in SIZES],
        append_images=images[:-1],
    )
    print(f"Generated {len(images)} PNGs and {ICO_PATH.relative_to(ROOT)} from {SOURCE.name}")


if __name__ == "__main__":
    main()
