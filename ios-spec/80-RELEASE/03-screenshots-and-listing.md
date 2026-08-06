---
id: 80-03
title: Screenshots & store listing
layer: release
status: TODO
depends_on: [50-06]
blocks: [80-05]
parallel_safe: true
estimate: 16h
reversible: true
owner_files: []
verify:
  - "Manual: all required screenshot sizes uploaded for EN and TR"
---

## 1. Goal

App Store screenshots for iPhone and iPad, and listing copy in EN and TR.

## 2. Why this way

**iPad screenshots are mandatory because the app is Universal.** 13" iPad images are a hard requirement, not a nice-to-have. If they are not ready, the alternative is dropping iPad support — a decision, not an oversight.

**There is already a screenshot discipline in this project to inherit.** `docs/screenshots/` holds 102 PNGs across 23 screens, named `NN_{state}_{lang}_{theme}.png` — empty and populated states, EN and TR, light and dark. That naming convention and that coverage are the model; the App Store set is a curated subset of the same idea.

**The listing is not a translation of the Play listing.** `donebot prod/FAZ0_STORE_COPY.md` is the starting point, but character limits, tone and the subtitle/promotional-text fields differ. Reusing Play copy verbatim reads as ported, and the Play copy has no equivalent of the App Store subtitle.

**Lead with what is distinctive.** DoneBot's differentiators are the AI assistant, shared groups, the pomodoro with ambience, the palette kits (especially the 8-bit one) and the polaroid journal. A generic to-do screenshot set wastes the strongest asset the app has.

## 3. Source — read before writing

| Path | Why |
|---|---|
| `docs/screenshots/` | 102 references across 23 screens — the shot list and naming convention |
| `.github/assets/*.png` | 18 README screenshots and feature graphics, EN and TR |
| `donebot prod/FAZ0_STORE_COPY.md` | Play copy — a starting point |
| `README.md` features section | The differentiator list, already written |
| `DONEBOT_CAPABILITIES.md` | What the assistant actually does |

## 4. Target

No files in this repository. Assets and copy live in App Store Connect.

## 5. Steps

1. **Confirm the required sizes.** At minimum 6.9" iPhone and 13" iPad. Verify current requirements in App Store Connect at capture time — they change.

2. **Choose 6–8 shots** that tell a story rather than enumerate features:
   1. Home with a populated task list — the core
   2. DoneBot chat mid-conversation — the differentiator
   3. Pomodoro running, with the Live Activity visible — iOS-specific
   4. A shared group with members — the social hook
   5. Activity heatmap with hearts — the retention mechanic
   6. The polaroid journal — the surprise
   7. App Colors showing the three kits, PIXEL prominent — the personality
   8. iPad two-pane — justifies Universal

3. **Capture on the simulator** at the exact required sizes, with realistic data. Empty states do not sell.

4. **Capture both light and dark**, choose per shot for contrast across the set.

5. **Capture the same set in Turkish.**

6. **Write the copy:** app name, subtitle (30 chars), promotional text (170), description (4000), keywords (100), what's new.

7. **Localize into Turkish** — write it, do not machine-translate. A recorded preference in this project is that machine-translated prose gets rewritten, keeping the structure but producing fresh sentences.

8. **Consider an app preview video.** Optional, and the pomodoro-with-Live-Activity flow is the natural subject. Cut if the schedule is tight.

## 6. Code skeleton

No code. The shot list, in order:

```
1. Home, populated                 "Your day, organised"
2. DoneBot chat                    "Ask, and it's done"
3. Pomodoro + Live Activity        "Focus that follows you"     ← iOS-specific
4. Group with members              "Share the load"
5. Activity heatmap + hearts       "Watch the streak build"
6. Polaroid journal                "A page for the day"
7. App Colors, PIXEL prominent     "Make it yours"
8. iPad two-pane                   "At home on iPad"            ← required for Universal
```

## 7. Acceptance

- [ ] 6.9" iPhone screenshots uploaded — EN and TR
- [ ] **13" iPad screenshots uploaded** — EN and TR
- [ ] All shots use realistic, populated data
- [ ] The Live Activity appears in at least one shot
- [ ] At least one shot shows the PIXEL kit
- [ ] At least one shot shows iPad two-pane
- [ ] App name, subtitle, promotional text, description, keywords complete in **both** languages
- [ ] Turkish copy is written, not machine-translated
- [ ] Keywords do not repeat words already in the name or subtitle
- [ ] No screenshot shows a real person's data or a real email address
- [ ] No screenshot or copy claims screenshot protection (see `80-02`)
- [ ] "What's New" written for 1.0

## 8. Pitfalls

- **iPad screenshots are mandatory for a Universal app.** Missing them means dropping iPad support.
- **Empty states do not sell.** Populate the demo data properly before capturing.
- **Do not translate the Play copy mechanically.** Different limits, different fields, different tone.
- **Keywords are 100 characters total, comma-separated.** Repeating words from the name or subtitle wastes them — those are already indexed.
- **The subtitle is 30 characters.** It is the highest-value line in the listing and the easiest to waste.
- **Real data in screenshots is a privacy problem.** Use fabricated names and addresses.
- **Screenshot requirements change.** Verify sizes at capture time rather than trusting a written list.
- **Status bar consistency.** Use a clean simulator status bar (`xcrun simctl status_bar`) across the set.

## 9. Verification

```bash
# Clean, consistent status bar across the set
xcrun simctl status_bar booted override --time "9:41" --batteryState charged --batteryLevel 100

# Capture
xcrun simctl io booted screenshot ~/Desktop/donebot-01-home-en.png

# Check dimensions against the current requirements
sips -g pixelWidth -g pixelHeight ~/Desktop/donebot-*.png

# In App Store Connect: all sizes present for EN and TR; copy complete in both
```
