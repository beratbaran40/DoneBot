#!/usr/bin/env python3
"""Generate 8-Bit-kit pixel variants of every `ic_*.xml` vector drawable.

Android's `android:pathData` IS SVG path syntax, so each vector can be re-wrapped as an SVG,
rasterised at a low resolution, alpha-thresholded into a pixel grid, and re-emitted as a vector
drawable made of axis-aligned cells. That gives all ~144 icons a consistent pixel-art variant without
hand-drawing any of them.

Most icons are single-colour and get tinted at draw time via `Icon(tint = …)`, so for those only
COVERAGE matters: they render as opaque black and emit one tintable path. The multi-colour icons
(the selected-tab art) carry meaning in their colours — a black silhouette would be strictly worse
than the original — so those are rasterised in colour and emitted as one path per colour, matching
how the app already draws them (`tint = Color.Unspecified`).

Auto-conversion is lossy for some SHAPES, and no grid size rescues them (measured: 16/121 icons are
degenerate at 16x16, and going to 24x24 fixes one while breaking two others). Three layers handle
that, in priority order:

1. `HAND` — an ASCII grid under `tools/pixelart/<name>.txt` (`#` on, `.` off) wins outright and is
   NEVER overwritten. This is the fix for the regression where a generator run silently replaced
   hand-tuned tab icons with worse auto-derived ones.
2. `SKIP` — icons that should never be pixelated at all (brand marks, geometric primitives).
3. The QUALITY GATE — anything that rasterises to a blob, a near-empty grid, or a shattered mess is
   dropped with a reason. A dropped icon simply gets no map entry, and `tdIconRes` then returns the
   original resource id, so the 8-Bit kit falls back to the smooth vector for that one icon.

Usage:
    genpixelicons.py <res-dir> [<res-dir> ...] [--grid 16] [--preview ic_home]
                     [--hand-dir tools/pixelart] [--contact-sheet out.png]

Each <res-dir> is a `.../res/drawable*` directory. Outputs `ic_pixel_<name>.xml` beside the sources
in the plain `drawable/` dir of the same module, and prints a summary. `--preview` dumps an ASCII
grid for one icon instead of writing anything, for eyeballing a conversion. `--contact-sheet` writes
a single PNG pairing every source with its pixel variant, for reviewing the whole set at once.

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
#
# The second group is a deliberate design exemption rather than a technical one: brand marks carry
# identity in their exact colours and a blocky approximation reads as a broken logo, and the
# geometric primitives are used as plain fills where "pixelated" has no meaning. Keeping them out of
# SKIP would just leave them in the quality-gate report forever as noise.
SKIP = ("ic_launcher_background", "ic_launcher_foreground", "ic_app_logo_background",
        "ic_app_logo_foreground", "ic_polaroid",
        "ic_american_flag", "ic_turkish_flag", "ic_google_logo",
        "ic_ellipse", "ic_rectangle_sharp", "ic_tasks_done")

# Conversions that pass the structural gate below but were REJECTED on sight from the contact
# sheet: a gear that loses its teeth, an eye that turns into a ring of noise, a fingerprint that
# becomes static. They keep their smooth vector in the 8-Bit kit, which reads far better than a
# blocky smear. The ones worth the effort were hand-drawn instead (see tools/pixelart/); these were
# judged either too detailed to survive 16x16 or too rarely seen to be worth the art time.
#
# Re-run with --contact-sheet after changing anything here and look at the result. There is no
# metric for this: silhouette overlap rewards blobs and punishes faithful thin strokes, so the
# structural gate catches only the unambiguous failures and the rest is a judgement call.
REJECTED = ("ic_avatar_new_group", "ic_daily_label", "ic_fast_forward", "ic_focus", "ic_fullscreen",
            "ic_globe", "ic_health_label", "ic_lock_reset", "ic_long_break", "ic_no_group_task",
            "ic_outline_expand_circle_right_24", "ic_palette", "ic_password", "ic_pending_invite",
            "ic_pin", "ic_pomodoro", "ic_refresh", "ic_resume", "ic_settings_code",
            "ic_settings_diagnostics", "ic_settings_fingerprint", "ic_settings_motion",
            "ic_settings_shield", "ic_settings_text_size", "ic_short_break", "ic_sun_cloud")

# --- Quality gate -----------------------------------------------------------------------------
# Thresholds come from measuring all 121 sources at 16x16, 20x20 and 24x24. They are intentionally
# loose: the goal is to catch conversions that are unrecognisable, not to enforce taste.
BLOB_FILL = 0.62      # a silhouette this solid has lost the detail that made it readable
SPARSE_FILL = 0.08    # a stroke this thin survives as a few disconnected dots
MAX_COMPONENTS = 5    # 8-connectivity: diagonal pixel-art lines stay one piece, real debris does not


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


def components(cells: list[list[bool]]) -> int:
    """Count 8-connected blobs. 8- rather than 4-connectivity because pixel art draws diagonals as
    corner-touching cells — under 4-connectivity a perfectly readable house roof counts as debris."""
    on = {(r, c) for r, row in enumerate(cells) for c, v in enumerate(row) if v}
    seen: set[tuple[int, int]] = set()
    found = 0
    for start in on:
        if start in seen:
            continue
        found += 1
        stack = [start]
        while stack:
            r, c = stack.pop()
            if (r, c) in seen:
                continue
            seen.add((r, c))
            for dr in (-1, 0, 1):
                for dc in (-1, 0, 1):
                    nb = (r + dr, c + dc)
                    if nb in on and nb not in seen:
                        stack.append(nb)
    return found


def degenerate(cells: list[list[bool]]) -> str | None:
    """Reason this conversion is unusable, or None when it passes."""
    total = len(cells) * len(cells)
    fill = sum(1 for row in cells for v in row if v) / total
    if fill > BLOB_FILL:
        return f"blob ({fill:.0%} filled)"
    if fill < SPARSE_FILL:
        return f"too sparse ({fill:.1%} filled)"
    pieces = components(cells)
    if pieces >= MAX_COMPONENTS:
        return f"shattered ({pieces} disconnected pieces)"
    return None


def load_hand_icons(hand_dir: pathlib.Path) -> dict[str, list[list[bool]]]:
    """Hand-authored grids: `<name>.txt`, N lines of N chars, `#` on and anything else off.

    Kept as text rather than XML so a diff shows the actual artwork and a fix is a one-character
    edit. The generator converts them through the same `cells_to_path`, so hand and generated icons
    are byte-identical in structure.
    """
    out: dict[str, list[list[bool]]] = {}
    if not hand_dir.is_dir():
        return out
    for f in sorted(hand_dir.glob("*.txt")):
        rows = [ln.rstrip("\n") for ln in f.read_text().splitlines() if ln.strip()]
        size = len(rows)
        if size == 0 or any(len(r) != size for r in rows):
            widths = sorted({len(r) for r in rows})
            raise SystemExit(f"{f}: expected a square grid, got {size} rows of widths {widths}")
        out[f.stem] = [[ch == "#" for ch in row] for row in rows]
    return out


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


ART = 56       # px per icon in the contact sheet
LABEL = 13     # px reserved under each pair for the name
COLUMNS = 7    # pairs per row


def contact_sheet(entries: list[tuple[str, str, list[list[bool]] | None]], out: pathlib.Path) -> None:
    """One PNG pairing every source with its pixel variant, so the whole set can be judged at once.

    `entries` is (name, svg, cells) — `cells` is None for an icon that has no pixel variant, which
    renders as a struck-through slot so the exemptions are visible rather than merely absent.
    """
    import cairosvg
    from PIL import Image, ImageDraw
    import io

    pair_w, pair_h = ART * 2 + 12, ART + LABEL + 8
    rows = (len(entries) + COLUMNS - 1) // COLUMNS
    sheet = Image.new("RGB", (pair_w * COLUMNS + 16, pair_h * rows + 16), "white")
    draw = ImageDraw.Draw(sheet)

    for i, (name, svg, cells) in enumerate(entries):
        ox = 8 + (i % COLUMNS) * pair_w
        oy = 8 + (i // COLUMNS) * pair_h
        png = cairosvg.svg2png(bytestring=svg.encode(), output_width=ART, output_height=ART)
        original = Image.open(io.BytesIO(png)).convert("RGBA")
        sheet.paste(original, (ox, oy), original)
        if cells is None:
            draw.line([ox + ART + 12, oy, ox + ART * 2 + 12, oy + ART], fill="#cccccc", width=2)
        else:
            size = len(cells)
            step = ART / size
            for r, row in enumerate(cells):
                for c, on in enumerate(row):
                    if on:
                        x, y = ox + ART + 12 + c * step, oy + r * step
                        draw.rectangle([x, y, x + step, y + step], fill="black")
        draw.text((ox, oy + ART + 2), name[3:][:26], fill="#666666")

    sheet.save(out)


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("dirs", nargs="+")
    ap.add_argument("--grid", type=int, default=16)
    ap.add_argument("--preview", default=None, help="print an ASCII grid for this icon and exit")
    ap.add_argument("--hand-dir", default="tools/pixelart",
                    help="ASCII grids that win over generation and are never overwritten")
    ap.add_argument("--contact-sheet", default=None, help="write a source-vs-pixel review PNG here")
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

    hand = load_hand_icons(pathlib.Path(args.hand_dir))
    # A hand grid for an icon that no longer exists is dead art. The tool is normally run once per
    # module, so "not a source in THIS run" is expected for the other module's icons — only warn
    # when the name matches no drawable anywhere in the tree.
    elsewhere = {f.stem for f in pathlib.Path(".").glob("*/src/main/res/drawable*/ic_*.xml")}
    orphans = sorted(set(hand) - set(sources) - elsewhere)

    written, multi, empty, review, dropped, sheet = 0, [], [], [], {}, []
    mapped: list[str] = []
    for name, src in sources.items():
        root = ET.parse(src).getroot()
        palette = sorted(source_colours(root))
        keep_colour = len(palette) > 1
        svg, needs_review = vector_to_svg(root, keep_colour=keep_colour)

        if name in REJECTED and name not in hand:
            # Hand art overrides a rejection, so an icon can be rescued by drawing it without
            # having to remember to also edit REJECTED.
            dropped[name] = "rejected on review"
            sheet.append((name, svg, None))
            continue

        if name in hand:
            # Hand art wins outright: no rasterising and no gate. The XML is still emitted from it,
            # so the grid file is the single source of truth and a re-run can never regress the art.
            bits = hand[name]
            paths = [(cells_to_path(bits, len(bits)), "#000000")]
            origin = f"{args.hand_dir}/{name}.txt — edit the grid there, not this file"
        else:
            cells = rasterise(svg, args.grid)
            if not any(c[3] >= ALPHA_THRESHOLD for row in cells for c in row):
                # Distinct from "too sparse": a blank grid means the SVG produced nothing at all,
                # which is a conversion bug (an unreadable colour reference), not a quality call.
                empty.append(name)
                continue

            bits = mask(cells)
            reason = degenerate(bits)
            if reason:
                dropped[name] = reason
                sheet.append((name, svg, None))
                continue
            if needs_review:
                review.append(name)

            if keep_colour:
                paths = colour_paths(cells, args.grid, palette)
                multi.append(name)
            else:
                paths = [(cells_to_path(bits, args.grid), "#000000")]
            origin = f"{src.name}. Do not hand-edit"
            written += 1

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
            f"  <!-- Generated by tools/genpixelicons.py from {origin}. -->\n"
            f"{body}\n"
            "</vector>\n"
        )
        (out_dir_for[name] / f"ic_pixel_{name[3:]}.xml").write_text(xml)
        mapped.append(name)
        sheet.append((name, svg, bits))

    # Sweep every variant this run did not produce, not just the ones still in `sources` — an icon
    # moved into SKIP leaves `sources` entirely, and its old variant would otherwise sit there
    # unreferenced, shipping in the APK and reading like a live asset.
    keep = {f"ic_pixel_{n[3:]}" for n in mapped}
    stale = []
    for out_dir in sorted(set(out_dir_for.values())):
        for f in sorted(out_dir.glob("ic_pixel_*.xml")):
            if f.stem not in keep:
                f.unlink()
                stale.append(f.stem)

    if args.kotlin_out:
        # The R class is imported, so entries reference it by its simple name — emitting the
        # fully-qualified form as well would leave the import unused and ktlint rejects that.
        r_simple = args.kotlin_r.rsplit(".", 1)[-1]
        # Only icons that actually HAVE a variant. Mapping a dropped icon would point the resolver
        # at a drawable that no longer exists and break the build.
        entries = "\n".join(
            f"    {r_simple}.drawable.{n} to {r_simple}.drawable.ic_pixel_{n[3:]},"
            for n in sorted(mapped)
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
        print(f"wrote {args.kotlin_out} ({len(mapped)} entries)")

    if args.contact_sheet:
        contact_sheet(sorted(sheet), pathlib.Path(args.contact_sheet))
        print(f"wrote {args.contact_sheet} ({len(sheet)} pairs)")

    hand_used = sorted(set(hand) & set(sources))
    print(f"{len(mapped)} of {len(sources)} icons have a pixel variant "
          f"({written} generated at {args.grid}x{args.grid}, {len(hand_used)} hand-drawn)")
    if multi:
        print(f"  colour-preserving (multi-colour source): {len(multi)} -> {', '.join(sorted(multi))}")
    if review:
        print(f"  NEEDS EYEBALL ({len(review)}, uses group/clip-path): {', '.join(sorted(review))}")
    if stale:
        print(f"  removed stale variants ({len(stale)}): {', '.join(stale)}")
    if dropped:
        # Not a failure: these fall back to the smooth vector in the 8-Bit kit. Hand-draw one into
        # tools/pixelart/ to bring it back.
        print(f"  QUALITY GATE dropped {len(dropped)} -> smooth vector in the 8-Bit kit:")
        for name in sorted(dropped):
            print(f"      {name:28s} {dropped[name]}")
    if orphans:
        print(f"  WARNING - hand art with no source icon ({len(orphans)}): {', '.join(orphans)}")
    if empty:
        print(f"FAILED - empty grid ({len(empty)}): {', '.join(sorted(empty))}")
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
