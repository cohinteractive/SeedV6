"""Extract the original SeedV6 chess-piece sheet into transparent PNG assets.

The pale sheet background is removed with an edge flood-fill. The dark piece
outlines act as barriers, so enclosed white fills and contrasting internal
details remain part of the artwork. All pieces use one scale and baseline.
"""

from collections import deque
import argparse
from pathlib import Path

from PIL import Image


SOURCE = Path(__file__).resolve().parents[1] / "chess-pieces-01.png"
OUTPUT = (
    Path(__file__).resolve().parents[1]
    / "app/src/main/resources/com/ohinteractive/seedv6/gui/pieces/original"
)
FILES = ("k", "q", "r", "b", "n", "p")
X_EDGES = (0, 310, 543, 777, 1000, 1219, 1491)
Y_EDGES = (0, 527, 1055)
BACKGROUND_THRESHOLD = 220
CANVAS_SIZE = 256
ARTWORK_HEIGHT = 224
BOTTOM_PADDING = 16


def is_background(pixel: tuple[int, int, int]) -> bool:
    red, green, blue = pixel
    luminance = (299 * red + 587 * green + 114 * blue) // 1000
    return luminance >= BACKGROUND_THRESHOLD


def artwork_mask(tile: Image.Image) -> Image.Image:
    width, height = tile.size
    pixels = tile.load()
    exterior = bytearray(width * height)
    pending: deque[tuple[int, int]] = deque()

    def enqueue(x: int, y: int) -> None:
        offset = y * width + x
        if not exterior[offset] and is_background(pixels[x, y]):
            exterior[offset] = 1
            pending.append((x, y))

    for x in range(width):
        enqueue(x, 0)
        enqueue(x, height - 1)
    for y in range(height):
        enqueue(0, y)
        enqueue(width - 1, y)

    while pending:
        x, y = pending.popleft()
        if x:
            enqueue(x - 1, y)
        if x + 1 < width:
            enqueue(x + 1, y)
        if y:
            enqueue(x, y - 1)
        if y + 1 < height:
            enqueue(x, y + 1)

    mask = Image.new("L", tile.size)
    mask.putdata([0 if outside else 255 for outside in exterior])
    return mask


def extract_artwork(tile: Image.Image) -> Image.Image:
    mask = artwork_mask(tile)
    bounds = mask.getbbox()
    if bounds is None:
        raise ValueError("piece artwork is blank")
    artwork = tile.crop(bounds).convert("RGBA")
    artwork.putalpha(mask.crop(bounds))
    return artwork


def validate_asset(path: Path) -> tuple[tuple[int, int, int, int], int, int]:
    with Image.open(path) as asset:
        if asset.mode != "RGBA" or asset.size != (CANVAS_SIZE, CANVAS_SIZE):
            raise ValueError(f"invalid output format for {path}: {asset.mode} {asset.size}")
        alpha = asset.getchannel("A")
        bounds = alpha.getbbox()
        if bounds is None:
            raise ValueError(f"blank output asset: {path}")
        histogram = alpha.histogram()
        if alpha.getextrema() != (0, 255) or not sum(histogram[1:255]):
            raise ValueError(f"output lacks clean alpha coverage: {path}")
        border = list(alpha.crop((0, 0, CANVAS_SIZE, 1)).getdata())
        border += list(alpha.crop((0, CANVAS_SIZE - 1, CANVAS_SIZE, CANVAS_SIZE)).getdata())
        border += list(alpha.crop((0, 0, 1, CANVAS_SIZE)).getdata())
        border += list(alpha.crop((CANVAS_SIZE - 1, 0, CANVAS_SIZE, CANVAS_SIZE)).getdata())
        if any(border):
            raise ValueError(f"non-transparent border in {path}")
        light_pixels = 0
        dark_pixels = 0
        for red, green, blue, opacity in asset.getdata():
            if opacity < 200:
                continue
            luminance = (299 * red + 587 * green + 114 * blue) // 1000
            light_pixels += luminance >= 220
            dark_pixels += luminance <= 80
        retained_fill = light_pixels if path.stem.startswith("w") else dark_pixels
        if retained_fill <= 100:
            raise ValueError(f"expected piece fill is missing: {path}")
        return bounds, histogram[255], sum(histogram[1:255])


def create_preview(path: Path) -> None:
    square_size = 80
    preview = Image.new("RGBA", (square_size * 6, square_size * 2))
    colours = ((238, 216, 180, 255), (126, 164, 118, 255))
    for row, colour in enumerate(("w", "b")):
        for column, piece in enumerate(FILES):
            left = column * square_size
            top = row * square_size
            tile = Image.new(
                "RGBA", (square_size, square_size), colours[(row + column) & 1]
            )
            with Image.open(OUTPUT / f"{colour}{piece}.png") as asset:
                tile.alpha_composite(
                    asset.resize((square_size, square_size), Image.Resampling.LANCZOS)
                )
            preview.alpha_composite(tile, (left, top))
    path.parent.mkdir(parents=True, exist_ok=True)
    preview.convert("RGB").save(path, format="PNG", optimize=True)


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview", type=Path)
    args = parser.parse_args()
    source = Image.open(SOURCE).convert("RGB")
    if source.size != (X_EDGES[-1], Y_EDGES[-1]):
        raise ValueError(f"unexpected source dimensions: {source.size}")

    pieces: list[tuple[str, Image.Image]] = []
    for row, colour in enumerate(("w", "b")):
        for column, piece in enumerate(FILES):
            tile = source.crop(
                (X_EDGES[column], Y_EDGES[row], X_EDGES[column + 1], Y_EDGES[row + 1])
            )
            pieces.append((colour + piece, extract_artwork(tile)))

    tallest = max(artwork.height for _, artwork in pieces)
    scale = ARTWORK_HEIGHT / tallest
    OUTPUT.mkdir(parents=True, exist_ok=True)
    for name, artwork in pieces:
        size = (
            max(1, round(artwork.width * scale)),
            max(1, round(artwork.height * scale)),
        )
        resized = artwork.resize(size, Image.Resampling.LANCZOS)
        canvas = Image.new("RGBA", (CANVAS_SIZE, CANVAS_SIZE), (0, 0, 0, 0))
        left = (CANVAS_SIZE - resized.width) // 2
        top = CANVAS_SIZE - BOTTOM_PADDING - resized.height
        canvas.alpha_composite(resized, (left, top))
        path = OUTPUT / f"{name}.png"
        canvas.save(path, format="PNG", optimize=True)
        bounds, opaque, partial = validate_asset(path)
        print(
            f"{path.relative_to(SOURCE.parent)} {size[0]}x{size[1]} artwork "
            f"alpha_bbox={bounds} opaque={opaque} partial={partial}"
        )
    if args.preview:
        create_preview(args.preview)
        print(f"preview {args.preview}")


if __name__ == "__main__":
    main()
