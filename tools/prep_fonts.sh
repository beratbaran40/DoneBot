#!/usr/bin/env bash
#
# Subsets the upstream JetBrains Mono masters into the four static weights the TERMINAL kit uses,
# writing them straight into uikit/src/main/res/font/.
#
# Why subset at all: the app ships two locales (androidResources.localeFilters = en, tr) but upstream
# JetBrains Mono carries Cyrillic and Greek as well. Dropping them takes each weight from ~200 KB to
# ~46 KB, and the release AAB has a hard 20 MiB ceiling enforced in CI with under 2 MiB of headroom.
#
# Why the NL ("no ligatures") masters: JetBrains Mono's signature coding ligatures would rewrite a
# user's task titled "A -> B" into an arrow glyph and "!=" into a single character. They also break
# tools/textfit.py, which measures per-character advances and cannot model a substitution.
#
# Requires fonttools; it is NOT a build dependency and CI never runs this:
#   python3 -m venv .venv && .venv/bin/pip install fonttools
#   tools/prep_fonts.sh /path/to/JetBrainsMono-x.y.z/fonts/ttf [path/to/pyftsubset]
#
# JetBrains Mono is licensed SIL OFL 1.1 — keep the entry in ui/licenses/LicensesScreen.kt in sync.
set -euo pipefail

SRC_DIR="${1:?usage: prep_fonts.sh <upstream fonts/ttf dir> [pyftsubset]}"
PYFTSUBSET="${2:-pyftsubset}"
DEST_DIR="$(cd "$(dirname "$0")/.." && pwd)/uikit/src/main/res/font"

# Google Fonts' canonical latin + latin-ext, which is every glyph Turkish needs (ı U+0131, İ U+0130,
# ğĞ U+011E-F, şŞ U+015E-F, çÇ, öÖ, üÜ), plus general punctuation, currency, the trend arrows the
# activity screen draws, and the true minus sign.
UNICODES="U+0000-00FF,U+0100-017F,U+0180-024F,U+2000-206F,U+20A0-20BF,U+2122,U+2190-2193,U+2212"

# kern/mark/mkmk keep spacing and accent placement; ccmp keeps composed glyphs working; locl carries
# the Turkish dotted/dotless-i forms. liga and calt are omitted on purpose — see the header.
FEATURES="kern,ccmp,locl,mark,mkmk"

# Upstream master -> the weight slot it fills in Type.kt's JetBrainsMono FontFamily.
WEIGHTS=("Regular:regular" "Medium:medium" "SemiBold:semi_bold" "ExtraBold:extra_bold")

total_src=0
total_out=0
for pair in "${WEIGHTS[@]}"; do
    src_name="${pair%%:*}"
    dest_name="${pair##*:}"
    src="$SRC_DIR/JetBrainsMonoNL-$src_name.ttf"
    dest="$DEST_DIR/jetbrains_mono_$dest_name.ttf"

    [ -f "$src" ] || { echo "missing master: $src" >&2; exit 1; }

    # No --flavor: res/font accepts raw TTF/OTF only and rejects woff/woff2 at inflate time.
    # No --no-hinting either: the kit's ramp bottoms out near 10sp, where hinting earns its bytes.
    "$PYFTSUBSET" "$src" \
        --output-file="$dest" \
        --unicodes="$UNICODES" \
        --layout-features="$FEATURES" \
        --drop-tables+=DSIG \
        --notdef-outline \
        --name-IDs='*' --name-legacy

    src_bytes=$(wc -c < "$src")
    out_bytes=$(wc -c < "$dest")
    total_src=$((total_src + src_bytes))
    total_out=$((total_out + out_bytes))
    printf '%-14s %8d -> %6d bytes  (-%d%%)\n' \
        "$src_name" "$src_bytes" "$out_bytes" $((100 - out_bytes * 100 / src_bytes))
done

printf '%-14s %8d -> %6d bytes  (-%d%%)\n' \
    "TOTAL" "$total_src" "$total_out" $((100 - total_out * 100 / total_src))
