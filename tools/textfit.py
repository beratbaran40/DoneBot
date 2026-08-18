#!/usr/bin/env python3
"""Check that every localized label still fits the slot that draws it, on a narrow phone.

The app's policy is "grow, don't cut": a label that needs two lines gets two lines and its container
grows. That makes "does not fit on one line" a non-event, and it is why this tool does NOT report it.
Three things are still defects, and those are what it looks for:

  WORD_TOO_WIDE     The slot is narrower than the string's longest UNBREAKABLE token. Wrapping cannot
                    help — Android's LineBreaker falls back to grapheme-level breaking, so the label
                    renders "Tamamlan / dı". The slot has to widen or the word has to change.
  ROW_MIN_OVERFLOW  A row's children ask for more hard minimum width than the row has. This does not
                    overflow: `widthIn(min=)` is coerced into the incoming constraints, so a trailing
                    child silently comes out narrower than its neighbour. Weight the children.
  FIXED_HEIGHT_CLIP A wrapped label needs more lines than its fixed-height container can show.

Why this exists: the reported Galaxy A25 bug was one dp of headroom on a button — invisible on the
developer's machine, broken on a real phone with the system font one notch up. Nothing in the repo
could see that. Previews render at one width and fontScale 1.0, there are no Compose UI tests, and
`ResponsiveContainer` is a no-op below 600dp. This reads the shipped TTFs and does the arithmetic.

WHAT IT CANNOT SEE, and you should not trust it to:

  * User-generated content. Group names, task titles, custom category labels — none of them are in
    strings.xml. The synthetic __ugc_* entries below stand in for the shapes that hurt, but the real
    coverage for those is the debug probe (`LocalTDTextOverflowReporter`), which reports a mid-word
    break from the running app.
  * Any slot not in the table. Slot widths cannot be derived from Compose source, so SLOTS is
    hand-maintained. Each entry cites the file:line its numbers came from and the tool re-reads that
    line: if the code moved or the padding changed, you get a loud PROVENANCE error rather than a
    quietly wrong answer. Re-read an entry whenever you touch its file.
  * Kerning. All ten faces ship GPOS; this sums `hmtx` advances only, which over-estimates Latin by
    1-2%. That is the correct direction for a gate, but it means "fits by 3dp" is not a real pass —
    hence SAFETY_MARGIN.

Three faces, not four: `Style.kt`'s `monochromeStyle()` returns `defaultStyle()`, so ORIGINAL and
MONOCHROME share Poppins. PIXEL swaps in Pixelify Sans and floors the small end of the ramp to 12sp;
TERMINAL swaps in JetBrains Mono, scales the whole ramp by `fontScale` and floors it at 10sp.

Usage:
    tools/textfit.py                      # the gate: 360 and 384dp, fontScale 1.0 and 1.3
    tools/textfit.py --widths 320 360 384 # add the max-screen-zoom case
    tools/textfit.py --all                # list every cell, not just failures
    tools/textfit.py --slot home.statcard.label

Exits 1 if anything FAILs. Requires: nothing — stdlib only.
"""
from __future__ import annotations

import argparse
import os
import html
import re
import struct
import sys
from dataclasses import dataclass, field
from pathlib import Path

REPO = Path(__file__).resolve().parent.parent

# Advances ignore kerning, so treat a margin this thin as a failure rather than a pass. Four percent
# comfortably covers the 1-2% GPOS error and the sub-dp cases that started this whole sweep.
SAFETY_MARGIN = 0.04

FONT_DIR = REPO / "uikit/src/main/res/font"
STRING_FILES = [
    REPO / "app/src/main/res/values/strings.xml",
    REPO / "app/src/main/res/values-tr/strings.xml",
    REPO / "uikit/src/main/res/values/strings.xml",
    REPO / "uikit/src/main/res/values-tr/strings.xml",
]


# --------------------------------------------------------------------------------------- TTF metrics


class Face:
    """Advance widths for one TTF, in em units. Enough to measure a run of text; nothing more."""

    def __init__(self, path: Path, line_height_em: float):
        self.name = path.stem
        self.line_height_em = line_height_em
        data = path.read_bytes()
        tables = {}
        for i in range(struct.unpack(">H", data[4:6])[0]):
            off = 12 + 16 * i
            tag = data[off:off + 4].decode("latin1")
            tables[tag] = struct.unpack(">II", data[off + 8:off + 16])
        head = tables["head"][0]
        self.upem = struct.unpack(">H", data[head + 18:head + 20])[0]
        hhea = tables["hhea"][0]
        count = struct.unpack(">H", data[hhea + 34:hhea + 36])[0]
        hmtx = tables["hmtx"][0]
        self.advances = [struct.unpack(">H", data[hmtx + 4 * i:hmtx + 4 * i + 2])[0] for i in range(count)]
        self.cmap = self._parse_cmap(data, tables["cmap"][0])

    @staticmethod
    def _parse_cmap(data: bytes, base: int) -> dict[int, int]:
        sub = None
        for i in range(struct.unpack(">H", data[base + 2:base + 4])[0]):
            pid, eid, off = struct.unpack(">HHI", data[base + 4 + 8 * i:base + 12 + 8 * i])
            if (pid, eid) in ((3, 1), (3, 10), (0, 3), (0, 4), (0, 6)):
                sub = base + off
                break
        if sub is None:
            raise ValueError("no unicode cmap subtable")
        seg_x2 = struct.unpack(">H", data[sub + 6:sub + 8])[0]
        segs = seg_x2 // 2
        ends, starts = sub + 14, sub + 14 + seg_x2 + 2
        deltas, ranges = starts + seg_x2, starts + 2 * seg_x2
        out: dict[int, int] = {}
        for i in range(segs):
            end = struct.unpack(">H", data[ends + 2 * i:ends + 2 * i + 2])[0]
            start = struct.unpack(">H", data[starts + 2 * i:starts + 2 * i + 2])[0]
            delta = struct.unpack(">h", data[deltas + 2 * i:deltas + 2 * i + 2])[0]
            range_off = struct.unpack(">H", data[ranges + 2 * i:ranges + 2 * i + 2])[0]
            if start == 0xFFFF:
                continue
            for code in range(start, min(end, 0xFFFE) + 1):
                if range_off == 0:
                    gid = (code + delta) & 0xFFFF
                else:
                    idx = ranges + 2 * i + range_off + 2 * (code - start)
                    if idx + 2 > len(data):
                        continue
                    gid = struct.unpack(">H", data[idx:idx + 2])[0]
                    if gid:
                        gid = (gid + delta) & 0xFFFF
                if gid:
                    out[code] = gid
        return out

    def width(self, text: str, size_sp: float) -> float:
        """Width of `text` in dp at `size_sp`. Unmapped codepoints are reported by `missing()`."""
        total = 0
        for ch in text:
            gid = self.cmap.get(ord(ch), 0)
            total += self.advances[gid] if gid < len(self.advances) else self.advances[-1]
        return total / self.upem * size_sp

    def missing(self, text: str) -> set[str]:
        """Codepoints with no glyph — they would measure as .notdef and silently under-report."""
        return {ch for ch in text if ord(ch) not in self.cmap and not ch.isspace()}


# Poppins sets USE_TYPO_METRICS: (1050 + 350 + 100) / 1000 em per line. PixelifySans is 1.200em,
# JetBrains Mono 1.320em (it also sets USE_TYPO_METRICS).
POPPINS = {w: Face(FONT_DIR / f"poppins_{w}.ttf", 1.500) for w in ("regular", "medium", "semi_bold", "bold")}
PIXELIFY = {w: Face(FONT_DIR / f"pixelify_sans_{w}.ttf", 1.200) for w in ("regular", "bold")}
# JetBrains Mono's heaviest shipped master is ExtraBold, which is the weight the 96sp pomodoro hero
# asks for; the ramp never requests W700, so "bold" maps onto it rather than onto a missing file.
JBMONO = {
    w: Face(FONT_DIR / f"jetbrains_mono_{f}.ttf", 1.320)
    for w, f in (("regular", "regular"), ("medium", "medium"), ("semi_bold", "semi_bold"), ("bold", "extra_bold"))
}

#: TERMINAL shrinks the whole ramp (Style.kt: terminalStyle().fontScale) because a monospace advance
#: runs wider than Poppins' proportional one. Kept here so this probe measures what the kit renders;
#: overridable from the environment so the value itself can be re-derived by sweeping it.
TERMINAL_SCALE = float(os.environ.get("TEXTFIT_TERMINAL_SCALE", "0.96"))
TERMINAL_FLOOR = 10.0


@dataclass(frozen=True)
class Style:
    """One entry of the type ramp. Mirrors uikit/.../theme/Type.kt — keep them in step."""

    name: str
    size_sp: float
    weight: str
    #: PIXEL floors the small end of the ramp (Style.kt: pixelStyle().minFontSize = 12.sp).
    pixel_floored: bool = False
    #: TDButton builds its own TextStyle and is NOT subject to that floor.
    button: bool = False


HEADING3 = Style("heading3", 18, "semi_bold")
HEADING5 = Style("heading5", 16, "semi_bold")
HEADING5_BOLD = Style("heading5 (bold)", 16, "bold")
HEADING6 = Style("heading6", 16, "medium")
REGULAR = Style("regularTextStyle", 14, "medium")
SUBHEADING3 = Style("subheading3", 14, "medium")
SUBHEADING1 = Style("subheading1", 12, "regular", pixel_floored=True)
SUBHEADING2 = Style("subheading2", 10, "regular", pixel_floored=True)
SUBHEADING4 = Style("subheading4", 12, "medium", pixel_floored=True)
BTN_MEDIUM = Style("TDButton MEDIUM", 18, "semi_bold", button=True)
BTN_SMALL = Style("TDButton SMALL", 14, "medium", button=True)


def faces_for(style: Style) -> list[tuple[str, Face, float]]:
    """(kit label, face, effective sp) for each kit that renders `style` differently."""
    poppins = POPPINS[style.weight]
    out = [("poppins", poppins, style.size_sp)]
    pixel_weight = "bold" if style.weight in ("semi_bold", "bold") else "regular"
    pixel_sp = max(style.size_sp, 12.0) if style.pixel_floored else style.size_sp
    out.append(("pixel", PIXELIFY[pixel_weight], pixel_sp))
    # TERMINAL scales first, then floors — the same order as TDTypography.sz(). TDButton applies the
    # scale itself but has no floor, which is why `button` styles skip the max().
    term_sp = style.size_sp * TERMINAL_SCALE
    if style.pixel_floored and not style.button:
        term_sp = max(term_sp, TERMINAL_FLOOR)
    out.append(("terminal", JBMONO[style.weight], term_sp))
    return out


# ----------------------------------------------------------------------------------------- geometry


@dataclass(frozen=True)
class Full:
    """Slot spans the screen. `chrome` is everything between the screen edges and the text."""

    outer: float
    chrome: float = 0.0

    def text_width(self, screen: float) -> float:
        return screen - self.outer - self.chrome


@dataclass(frozen=True)
class NUp:
    """`n` equal columns across the screen. `chrome` is per-column padding/icons inside one column."""

    n: int
    gap: float
    outer: float
    chrome: float = 0.0

    def text_width(self, screen: float) -> float:
        return (screen - self.outer - self.gap * (self.n - 1)) / self.n - self.chrome


@dataclass(frozen=True)
class Fixed:
    """Slot has a hard width regardless of screen size."""

    dp: float

    def text_width(self, screen: float) -> float:
        return self.dp


@dataclass(frozen=True)
class RowMin:
    """A row of children with hard minimum widths. Checked for over-subscription, not for text."""

    minimums: list[float]
    gap: float
    outer: float

    def room(self, screen: float) -> float:
        return screen - self.outer

    def demand(self) -> float:
        return sum(self.minimums) + self.gap * (len(self.minimums) - 1)


@dataclass(frozen=True)
class Slot:
    id: str
    #: file:line the geometry was read from, and a substring that must still be on that line.
    source: str
    expect: str
    geometry: object
    style: Style | None = None
    keys: list[str] = field(default_factory=list)
    #: Fixed container height in dp, for FIXED_HEIGHT_CLIP. None means the container can grow.
    fixed_height: float | None = None
    note: str = ""
    #: Why this slot is allowed to fail. Set ONLY for a deliberate policy exception; findings are
    #: printed as NOTE with this reason and do not fail the gate. Never use it to quiet a real bug.
    accepted: str = ""


# Synthetic stand-ins for user-typed content, which no string file contains.
UGC = {
    "__ugc_long_word__": "Değerlendirmelerimiz",
    "__ugc_email__": "member@example.com",
}

# ------------------------------------------------------------------------------------------- slots
#
# Post-fix geometry: these describe the code as it stands, so a regression shows up as a new FAIL.

SLOTS = [
    Slot(
        id="home.statcard.label",
        source="uikit/src/main/java/com/todoapp/uikit/components/TDStatisticCard.kt",
        expect="Modifier.padding(16.dp)",
        # The icon stacks above the text, so the only chrome is the card's own 16dp padding.
        geometry=NUp(n=2, gap=12, outer=32, chrome=32),
        style=SUBHEADING1,
        keys=["task_complete", "task_pending"],
        note="was a Row: a 44dp icon + 14dp gutter left 68dp and truncated 'Tamamlandı'",
    ),
    Slot(
        id="home.statcard.period",
        source="uikit/src/main/java/com/todoapp/uikit/components/TDStatisticCard.kt",
        expect="horizontalArrangement = Arrangement.spacedBy(6.dp)",
        # Same card interior as the label above, minus the count sharing its line. Three digits of
        # heading5-bold at font scale 1.3 is ~42dp, plus the row's 6dp gap.
        geometry=NUp(n=2, gap=12, outer=32, chrome=32 + 48),
        style=SUBHEADING4,
        keys=["weekly"],
        note="the count moved onto this line to buy the list below a row of height",
    ),
    Slot(
        id="pomodoro.summary.statlabel",
        source="app/src/main/java/com/todoapp/mobile/ui/pomodorosummary/PomodoroSummaryScreen.kt",
        expect="padding(horizontal = 24.dp)",
        geometry=NUp(n=3, gap=12, outer=48, chrome=16),
        style=SUBHEADING1,
        keys=["pomodoro_focus_sessions", "pomodoro_total_focus", "pomodoro_break_time"],
        note="'Odaklanma' did not fit and could not wrap; renamed to 'Odak Süresi'",
    ),
    Slot(
        id="activity.pomodoro.statlabel",
        source="app/src/main/java/com/todoapp/mobile/ui/activity/ActivityPomodoroSection.kt",
        expect="horizontalArrangement = Arrangement.spacedBy(12.dp)",
        # 16dp screen padding + 16dp card padding, both sides.
        geometry=NUp(n=3, gap=12, outer=64),
        style=SUBHEADING3,
        keys=[
            "activity_pomodoro_focus_time",
            "activity_pomodoro_sessions",
            "activity_pomodoro_best_day",
        ],
        note="same three-up shape as pomodoro.summary.statlabel, one card deeper so 16dp tighter",
    ),
    Slot(
        id="pomodoro.launch.cta",
        source="app/src/main/java/com/todoapp/mobile/ui/pomodorolaunch/PomodoroLaunchScreen.kt",
        expect="padding(horizontal = 24.dp)",
        geometry=Full(outer=48, chrome=24),
        style=BTN_MEDIUM,
        keys=["pomodoro_configure_timer", "start"],
        note="the reported A25 bug: 311.3dp of label against 312dp of room",
    ),
    Slot(
        id="auth.forgot.cta",
        source="app/src/main/java/com/todoapp/mobile/ui/forgotpassword/ForgotPasswordScreen.kt",
        expect="TDButton(",
        geometry=Full(outer=32, chrome=24),
        style=BTN_MEDIUM,
        keys=["send_reset_link"],
    ),
    Slot(
        id="settings.item.title",
        source="uikit/src/main/java/com/todoapp/uikit/components/TDSettingsItem.kt",
        expect="Modifier.weight(1f)",
        # 32 screen + 32 card padding + 40 medallion + 12 gutter + 52 trailing switch.
        geometry=Full(outer=32, chrome=136),
        style=HEADING6,
        keys=["settings_exact_alarms_title", "settings_crash_analytics_title", "settings_daily_plan_reminder"],
    ),
    Slot(
        id="taskcard.title",
        source="uikit/src/main/java/com/todoapp/uikit/components/TDTaskCardWithCheckbox.kt",
        expect="Column(modifier = Modifier.weight(1f))",
        # 32 screen + 24 card padding + 24 checkbox + 10 gutter.
        geometry=Full(outer=32, chrome=58),
        style=REGULAR,
        keys=["__ugc_long_word__", "__ugc_email__"],
        note="user-typed titles; the debug probe is the real coverage here",
    ),
    Slot(
        id="activity.category.label",
        source="app/src/main/java/com/todoapp/mobile/ui/activity/ActivityScreen.kt",
        expect="widthIn(min = 96.dp, max = 132.dp)",
        geometry=Fixed(132),
        style=HEADING6,
        keys=["__ugc_long_word__"],
    ),
    Slot(
        id="bottombar.tab",
        source="app/src/main/java/com/todoapp/mobile/navigation/TDBottomBar.kt",
        expect="TDTheme.typography.subheading2",
        geometry=NUp(n=5, gap=0, outer=0, chrome=8),
        style=SUBHEADING2,
        keys=[
            "navbar_home_screen_page_name",
            "groups",
            "bottombar_chat_tab_label",
            "navbar_calendar_screen_page_name",
            "navbar_statistic_screen_page_name",
        ],
        accepted=(
            "fixed-height chrome — a second line is not an option here, so the labels ellipsize by "
            "design (maxLines = 1). The n=5 model is also pessimistic: alwaysShowLabel = false means "
            "only the selected tab draws a label, so it gets more than a fifth of the bar."
        ),
    ),
    Slot(
        id="groupcard.details.button",
        source="uikit/src/main/java/com/todoapp/uikit/components/TDGroupsSummary.kt",
        expect="Modifier.weight(1f)",
        geometry=Full(outer=32, chrome=40 + 140 + 8),
        style=SUBHEADING4,
        keys=["group_card_created_label"],
    ),
    Slot(
        id="creationhub.card.title",
        source="app/src/main/java/com/todoapp/mobile/ui/creationhub/CreationHubScreen.kt",
        expect="private val CREATION_GRID_GAP = 12.dp",
        # 20dp screen padding both sides, 12dp between the two columns, 8dp card padding both sides.
        geometry=NUp(n=2, gap=12, outer=40, chrome=16),
        style=HEADING5_BOLD,
        keys=[
            "create_task_card_title",
            "journal_card_title",
            "pomodoro_card_title",
            "group_card_title",
        ],
        note="was a full-width pager page at heading2; the 2x2 grid cut the column to 126dp at 360dp",
    ),
    Slot(
        id="creationhub.card.subtitle",
        source="uikit/src/main/java/com/todoapp/uikit/components/TDFeatureCard.kt",
        expect="private val COMPACT_PADDING_H = 8.dp",
        # Same cell as creationhub.card.title; this entry pins the card's half of the arithmetic.
        geometry=NUp(n=2, gap=12, outer=40, chrome=16),
        style=SUBHEADING1,
        keys=[
            "create_task_card_subtitle",
            "journal_card_subtitle",
            "pomodoro_card_subtitle",
            "group_card_subtitle",
        ],
        note="wrapping is expected here — the row grows to its tallest card, so only a broken word fails",
    ),
    # --- rows checked for hard-minimum over-subscription rather than text ---
    Slot(
        id="planpicker.footer",
        source="uikit/src/main/java/com/todoapp/uikit/components/TDPlanTimePickerField.kt",
        expect="Modifier.weight(1f)",
        # Weighted now, so the floors no longer apply. Kept as a guard: drop the weights and this
        # goes red again.
        geometry=RowMin(minimums=[], gap=12, outer=368),
        note="dialog Column is widthIn(max = 320) with 24dp padding -> 272dp of room",
    ),
    Slot(
        id="invitations.card.actions",
        source="app/src/main/java/com/todoapp/mobile/ui/invitations/InvitationsScreen.kt",
        expect="Modifier.weight(1f)",
        geometry=RowMin(minimums=[], gap=10, outer=64),
        note="weighted now; 140+10+140 against ~296dp of card before",
    ),
]


# -------------------------------------------------------------------------------------------- input


def load_strings() -> dict[str, dict[str, str]]:
    """{key: {lang: value}} across both modules. Placeholders and escapes are normalised."""
    out: dict[str, dict[str, str]] = {}
    for path in STRING_FILES:
        lang = "tr" if "values-tr" in str(path) else "en"
        text = path.read_text(encoding="utf-8")
        for match in re.finditer(r'<string name="([^"]+)"[^>]*>(.*?)</string>', text, re.S):
            value = re.sub(r"<[^>]+>", "", match.group(2))
            value = html.unescape(value)
            value = value.replace("\\n", " ").replace("\\'", "'").replace('\\"', '"')
            value = re.sub(r"%\d\$[sdf]|%[sdf]", "88", value)
            out.setdefault(match.group(1), {})[lang] = value.strip()
    # One language only: the probe strings are not translations of each other, they are shapes.
    for key, value in UGC.items():
        out[key] = {"--": value}
    return out


#: Characters a line may break on or after — mirrors TDText.isBreakOpportunity.
BREAKABLE = re.compile(r"[\s\-/·—–]+")


def longest_token(text: str) -> str:
    tokens = [t for t in BREAKABLE.split(text) if t]
    return max(tokens, key=len) if tokens else ""


def check_provenance() -> list[str]:
    """A slot whose source no longer contains `expect` is describing code that moved."""
    problems = []
    for slot in SLOTS:
        path = REPO / slot.source
        if not path.exists():
            problems.append(f"{slot.id}: source file is gone — {slot.source}")
        elif slot.expect not in path.read_text(encoding="utf-8"):
            problems.append(f"{slot.id}: {slot.source} no longer contains {slot.expect!r}")
    return problems


# ------------------------------------------------------------------------------------------- checks


def run(widths: list[float], scales: list[float], show_all: bool, only: str | None) -> int:
    strings = load_strings()
    failures = 0
    rows: list[tuple[str, str]] = []

    for slot in SLOTS:
        if only and slot.id != only:
            continue

        if isinstance(slot.geometry, RowMin):
            demand = slot.geometry.demand()
            for screen in widths:
                room = slot.geometry.room(screen)
                if demand > room:
                    failures += 1
                    rows.append(("FAIL", f"ROW_MIN_OVERFLOW {slot.id} {screen:.0f}dp room={room:.0f} demand={demand:.0f}"))
                elif show_all:
                    rows.append(("ok", f"ROW_MIN_OK        {slot.id} {screen:.0f}dp room={room:.0f} demand={demand:.0f}"))
            continue

        assert slot.style is not None, f"{slot.id}: a text slot needs a style"
        for key in slot.keys:
            values = strings.get(key)
            if values is None:
                failures += 1
                rows.append(("FAIL", f"MISSING_KEY      {slot.id} {key}"))
                continue
            for lang, value in sorted(values.items()):
                token = longest_token(value)
                for kit, face, size_sp in faces_for(slot.style):
                    absent = face.missing(token)
                    if absent:
                        failures += 1
                        rows.append(("FAIL", f"NO_GLYPH         {slot.id} {key} [{lang}/{kit}] {sorted(absent)}"))
                        continue
                    # A synthetic user-content probe cannot be a gate: no fixed column fits an
                    # arbitrary word, and the honest coverage for typed text is the runtime probe.
                    # It is still worth printing — it says how much room real content actually has.
                    level = "NOTE" if slot.accepted else ("WARN" if key in UGC else "FAIL")
                    # Only a FAIL needs every cell spelled out. A warning or a declared exception is
                    # the same finding at every width, so report its tightest case and move on.
                    cells = (
                        [(min(widths), max(scales))] if level != "FAIL"
                        else [(w, s) for w in widths for s in scales]
                    )
                    for screen, scale in cells:
                        available = slot.geometry.text_width(screen)
                        need = face.width(token, size_sp) * scale
                        budget = available * (1 - SAFETY_MARGIN)
                        tag = f"{slot.id} {key} [{lang}/{kit}] {screen:.0f}dp x{scale}"
                        if need > budget:
                            if level == "FAIL":
                                failures += 1
                            rows.append((
                                level,
                                f"WORD_TOO_WIDE    {tag} slot={available:.0f} word={need:.0f} {token!r}",
                            ))
                        elif show_all:
                            rows.append(("ok", f"FITS             {tag} slot={available:.0f} word={need:.0f}"))
                        if slot.fixed_height is not None:
                            lines = max(1, -(-face.width(value, size_sp) * scale // max(available, 1)))
                            needed = lines * size_sp * scale * face.line_height_em
                            if needed > slot.fixed_height:
                                if level == "FAIL":
                                    failures += 1
                                rows.append((
                                    level,
                                    f"FIXED_HEIGHT_CLIP {tag} box={slot.fixed_height:.0f} needs={needed:.0f}",
                                ))

    prefix = {"FAIL": "FAIL ", "WARN": "warn ", "NOTE": "note ", "ok": "     "}
    for level, line in rows:
        print(prefix[level] + line)

    flagged = {line.split()[1] for level, line in rows if level == "NOTE"}
    for slot in SLOTS:
        if slot.id in flagged:
            print(f"\nnote  {slot.id} is a declared exception — {slot.accepted}")
    warns = sum(1 for level, _ in rows if level == "WARN")
    return failures, warns


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--widths", type=float, nargs="+", default=[360, 384],
                        help="screen widths in dp (default: 360 384; add 320 for max screen zoom)")
    parser.add_argument("--scales", type=float, nargs="+", default=[1.0, 1.3],
                        help="system font scales (default: 1.0 1.3)")
    parser.add_argument("--all", action="store_true", help="print passing cells too")
    parser.add_argument("--slot", help="check one slot id")
    args = parser.parse_args()

    stale = check_provenance()
    if stale:
        print("PROVENANCE — a slot's geometry was read from code that has since moved.")
        print("Re-read the entry and update it; do not silence this.\n")
        for problem in stale:
            print(f"  {problem}")
        return 2

    failures, warns = run(args.widths, args.scales, args.all, args.slot)
    scope = f"widths {args.widths}, font scales {args.scales}, {SAFETY_MARGIN:.0%} margin"
    tail = f" {warns} warning(s) — user-typed content, not gated." if warns else ""
    print()
    if failures:
        print(f"{failures} failure(s). {scope}.{tail}")
        return 1
    print(f"Every gated slot fits. {scope}.{tail}")
    return 0


if __name__ == "__main__":
    sys.exit(main())
