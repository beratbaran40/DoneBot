#!/usr/bin/env python3
"""Generate 8-Bit-kit pixel variants of every `ic_*.xml` vector drawable.

Android's `android:pathData` IS SVG path syntax, so each vector can be re-wrapped as an SVG,
rasterised at a low resolution, alpha-thresholded into a pixel grid, and re-emitted as a vector
drawable made of axis-aligned cells. That gives all ~144 icons a consistent pixel-art variant without
hand-drawing any of them.

Most icons (120 of 131) are single-colour and get tinted at draw time via `Icon(tint = …)`, so for
those only COVERAGE matters: they render as opaque black and emit one tintable path. The 11
multi-colour icons (flags, the Google mark, the selected-tab art) carry meaning in their colours — a
black silhouette would be strictly worse than the original — so those are rasterised in colour and
emitted as one path per colour, matching how the app already draws them (`tint = Color.Unspecified`).

Usage:
    genpixelicons.py <res-dir> [<res-dir> ...] [--grid 16] [--preview ic_home]

Each <res-dir> is a `.../res/drawable*` directory. Outputs `ic_pixel_<name>.xml` beside the sources
in the plain `drawable/` dir of the same module, and prints a summary. `--preview` dumps an ASCII
grid for one icon instead of writing anything, for eyeballing a conversion.

Requires: cairosvg (`pip install cairosvg pillow`).
"""
from __future__ import annotations

import argparse
import pathlib
import re
import sys
import xml.etree.ElementTree as ET

ANDROID = "{http://schemas.android.com/apk/res/android}"
# Grid cells are emitted into a 24x24 viewport so the output matches every other ic_* in the project.
VIEWPORT = 24.0
# Alpha coverage above which a cell counts as "on". 0.5 keeps thin strokes without smearing them.
ALPHA_THRESHOLD = 128
# Launcher/adaptive-icon assets are never drawn through the in-app icon path, and the polaroid art
# describes a physical object that stays kit-invariant (same rule as PolaroidColors).
SKIP = ("ic_launcher_background", "ic_launcher_foreground", "ic_app_logo_background",
        "ic_app_logo_foreground", "ic_polaroid")


def a(el: ET.Element, name: str, default: str | None = None) -> str | None:
    return el.get(ANDROID + name, default)


def opaque(colour: str | None) -> bool:
    """A vector colour is drawable unless it is absent or fully transparent (#00xxxxxx)."""
    if not colour:
        return False
    c = colour.strip()
    if c.startswith("@"):  # theme/resource reference — assume it paints something
        return True
    if re.fullmatch(r"#[0-9a-fA-F]{8}", c):
        return int(c[1:3], 16) != 0
    return True


def norm_colour(c: str | None) -> str | None:
    """`#AARRGGBB` / `#RRGGBB` -> `#rrggbb`; None when absent, a reference, or fully transparent."""
    if not c or c.startswith("@"):
        return None
    c = c.strip().lower()
    if re.fullmatch(r"#[0-9a-f]{8}", c):
        return None if int(c[1:3], 16) == 0 else "#" + c[3:]
    if re.fullmatch(r"#[0-9a-f]{6}", c):
        return c
    return None


def source_colours(root: ET.Element) -> set[str]:
    out = set()
    for el in root.iter():
        for attr in ("fillColor", "strokeColor"):
            c = norm_colour(el.get(ANDROID + attr))
            if c:
                out.add(c)
    return out


def path_to_svg(el: ET.Element, keep_colour: bool) -> str:
    d = a(el, "pathData")
    if not d:
        return ""
    bits = [f'd="{d}"']
    # `opaque` decides WHETHER it paints (it accepts `@color/...` references, which several
    # Material-derived icons use); `norm_colour` supplies the literal, and only matters when the
    # source is multi-colour and we are preserving colours.
    fill = norm_colour(a(el, "fillColor"))
    if not opaque(a(el, "fillColor")):
        bits.append('fill="none"')
    else:
        bits.append(f'fill="{fill}"' if (fill and keep_colour) else 'fill="#000000"')
    if a(el, "fillType", "").lower() == "evenodd":
        bits.append('fill-rule="evenodd"')
    stroke = norm_colour(a(el, "strokeColor"))
    if opaque(a(el, "strokeColor")):
        width = a(el, "strokeWidth", "1")
        if float(width) > 0:
            paint = stroke if (stroke and keep_colour) else "#000000"
            bits.append(f'stroke="{paint}" stroke-width="{width}"')
            bits.append(f'stroke-linecap="{a(el, "strokeLineCap", "butt")}"')
            bits.append(f'stroke-linejoin="{a(el, "strokeLineJoin", "miter")}"')
    return "<path " + " ".join(bits) + " />"


def group_transform(el: ET.Element) -> str:
    """Android <group> transform attributes, in the order the platform applies them."""
    parts = []
    px, py = a(el, "pivotX", "0"), a(el, "pivotY", "0")
    tx, ty = a(el, "translateX", "0"), a(el, "translateY", "0")
    rot = a(el, "rotation", "0")
    sx, sy = a(el, "scaleX", "1"), a(el, "scaleY", "1")
    if float(tx) or float(ty):
        parts.append(f"translate({tx},{ty})")
    if float(rot):
        parts.append(f"rotate({rot},{px},{py})")
    if float(sx) != 1 or float(sy) != 1:
        parts.append(f"translate({px},{py}) scale({sx},{sy}) translate(-{px},-{py})")
    return " ".join(parts)


def vector_to_svg(root: ET.Element, keep_colour: bool = False) -> tuple[str, bool]:
    vw = a(root, "viewportWidth", "24")
    vh = a(root, "viewportHeight", "24")
    body: list[str] = []
    review = False

    def walk(node: ET.Element) -> None:
        nonlocal review
        for child in node:
            tag = child.tag.split("}")[-1]
            if tag == "path":
                body.append(path_to_svg(child, keep_colour))
            elif tag == "group":
                review = True
                t = group_transform(child)
                body.append(f'<g transform="{t}">' if t else "<g>")
                walk(child)
                body.append("</g>")
            elif tag == "clip-path":
                # A clip-path would need <clipPath>+clip-path refs; flag instead of silently dropping.
                review = True

    walk(root)
    svg = (
        f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 {vw} {vh}" '
        f'width="{vw}" height="{vh}">{"".join(body)}</svg>'
    )
    return svg, review


def rasterise(svg: str, grid: int) -> list[list[tuple[int, int, int, int]]]:
    import cairosvg
    from PIL import Image
    import io

    png = cairosvg.svg2png(bytestring=svg.encode(), output_width=grid, output_height=grid)
    img = Image.open(io.BytesIO(png)).convert("RGBA")
    px = img.load()
    return [[px[x, y] for x in range(grid)] for y in range(grid)]


def mask(cells) -> list[list[bool]]:
    return [[c[3] >= ALPHA_THRESHOLD for c in row] for row in cells]


def snap(rgb: tuple[int, int, int], palette: list[str]) -> str:
    """Nearest source colour — kills the antialiasing fringe the rasteriser leaves between shapes."""
    r, g, b = rgb
    best, bd = palette[0], 1 << 30
    for hexc in palette:
        pr, pg, pb = int(hexc[1:3], 16), int(hexc[3:5], 16), int(hexc[5:7], 16)
        d = (r - pr) ** 2 + (g - pg) ** 2 + (b - pb) ** 2
        if d < bd:
            best, bd = hexc, d
    return best


def cells_to_path(cells: list[list[bool]], grid: int) -> str:
    """Merge horizontal runs into rectangles so the emitted pathData stays compact."""
    cell = VIEWPORT / grid
    out = []

    def f(v: float) -> str:
        return f"{v:g}"

    for r, row in enumerate(cells):
        c = 0
        while c < grid:
            if not row[c]:
                c += 1
                continue
            start = c
            while c < grid and row[c]:
                c += 1
            x, y, w = start * cell, r * cell, (c - start) * cell
            out.append(f"M{f(x)},{f(y)}h{f(w)}v{f(cell)}h-{f(w)}z")
    return "".join(out)


def ascii_preview(cells: list[list[bool]]) -> str:
    return "\n".join("".join("#" if v else "." for v in row) for row in cells)


def colour_paths(cells, grid: int, palette: list[str]) -> list[tuple[str, str]]:
    """One (pathData, #rrggbb) per distinct colour, so a flag stays a flag."""
    buckets: dict[str, list[list[bool]]] = {}
    for r, row in enumerate(cells):
        for c, px in enumerate(row):
            if px[3] < ALPHA_THRESHOLD:
                continue
            key = snap(px[:3], palette)
            grid_for = buckets.setdefault(key, [[False] * grid for _ in range(grid)])
            grid_for[r][c] = True
    return [(cells_to_path(g, grid), col) for col, g in buckets.items()]


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("dirs", nargs="+")
    ap.add_argument("--grid", type=int, default=16)
    ap.add_argument("--preview", default=None, help="print an ASCII grid for this icon and exit")
    ap.add_argument("--kotlin-out", default=None, help="write the Int->Int resource map here")
    ap.add_argument("--kotlin-package", default=None)
    ap.add_argument("--kotlin-r", default=None, help="fully-qualified R class for the map entries")
    ap.add_argument("--kotlin-name", default="PixelIcons")
    args = ap.parse_args()

    sources: dict[str, pathlib.Path] = {}
    out_dir_for: dict[str, pathlib.Path] = {}
    for d in args.dirs:
        p = pathlib.Path(d)
        plain = p.parent / "drawable"
        for f in sorted(p.glob("ic_*.xml")):
            if f.stem.startswith("ic_pixel_") or f.stem in SKIP:
                continue
            sources.setdefault(f.stem, f)
            out_dir_for.setdefault(f.stem, plain)

    if args.preview:
        f = sources.get(args.preview)
        if not f:
            print(f"no such icon: {args.preview}")
            return 1
        svg, _ = vector_to_svg(ET.parse(f).getroot())
        print(ascii_preview(mask(rasterise(svg, args.grid))))
        return 0

    written, multi, empty, review = 0, [], [], []
    for name, src in sources.items():
        root = ET.parse(src).getroot()
        palette = sorted(source_colours(root))
        keep_colour = len(palette) > 1

        svg, needs_review = vector_to_svg(root, keep_colour=keep_colour)
        cells = rasterise(svg, args.grid)
        if not any(c[3] >= ALPHA_THRESHOLD for row in cells for c in row):
            empty.append(name)
            continue
        if needs_review:
            review.append(name)

        if keep_colour:
            paths = colour_paths(cells, args.grid, palette)
            multi.append(name)
        else:
            paths = [(cells_to_path(mask(cells), args.grid), "#000000")]

        body = "\n".join(
            f'  <path\n      android:pathData="{d}"\n      android:fillColor="#FF{col[1:]}" />'
            for d, col in paths
        )
        xml = (
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            '    android:width="24dp"\n'
            '    android:height="24dp"\n'
            '    android:viewportWidth="24"\n'
            '    android:viewportHeight="24">\n'
            f"  <!-- Generated by tools/genpixelicons.py from {src.name}. Do not hand-edit. -->\n"
            f"{body}\n"
            "</vector>\n"
        )
        (out_dir_for[name] / f"ic_pixel_{name[3:]}.xml").write_text(xml)
        written += 1

    if args.kotlin_out:
        entries = "\n".join(
            f"    {args.kotlin_r}.drawable.{n} to {args.kotlin_r}.drawable.ic_pixel_{n[3:]},"
            for n in sorted(sources)
        )
        kt = (
            f"package {args.kotlin_package}\n\n"
            f"import {args.kotlin_r}\n\n"
            "/**\n"
            " * Source icon -> 8-Bit pixel variant. GENERATED by tools/genpixelicons.py — do not hand-edit;\n"
            " * re-run the tool after adding or removing an `ic_*` drawable.\n"
            " *\n"
            " * Library R fields are not compile-time constants, so this is a runtime map rather than a\n"
            " * `when` — which is also why it can be merged across modules.\n"
            " */\n"
            f"val {args.kotlin_name}: Map<Int, Int> = mapOf(\n{entries}\n)\n"
        )
        pathlib.Path(args.kotlin_out).write_text(kt)
        print(f"wrote {args.kotlin_out} ({len(sources)} entries)")

    print(f"generated {written} pixel icons at {args.grid}x{args.grid}")
    print(f"  colour-preserving (multi-colour source): {len(multi)} -> {', '.join(sorted(multi))}")
    if review:
        print(f"  NEEDS EYEBALL ({len(review)}, uses group/clip-path): {', '.join(sorted(review))}")
    if empty:
        print(f"FAILED - empty grid ({len(empty)}): {', '.join(sorted(empty))}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
