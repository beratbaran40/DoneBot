---
id: 50-04
title: Components tier C — mechanical, plus the text fields
layer: design
status: TODO
depends_on: [50-00]
blocks: [40-auth-02, 40-core-03]
parallel_safe: true
estimate: 20h
reversible: true
owner_files:
  - uikit/src/commonMain/kotlin/com/todoapp/uikit/components/**
  - uikit/src/commonMain/kotlin/com/todoapp/uikit/previews/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Verify the ~45 straightforward components, rebuild the preview infrastructure, and — the real content of this task — validate the **846 lines of custom text-field behaviour** on iOS.

## 2. Why this way

**Forty-five components are genuinely mechanical.** `TDText`, `TDButton`, `TDSwitch`, `TDStatusChip`, `TDPriorityBadge`, `TDInfoCard`, `TDEmptyState`, `TDErrorState`, `TDChoiceChip`, `TDChoiceTile`, `TDOptionCard`, `TDFeatureCard`, `TDSettingsGroup`/`TDSettingsItem`, the navigators, the permission cards, the picker fields, the dialogs. They compose primitives and read tokens. They port and need a look, not a plan.

**Two files are not mechanical at all.** `TDTextField.kt` (584) and `TDOutlinedTextField.kt` (262) are **846 lines of behaviour that `TextField` does not give you**, and text input is the single most-cited CMP-on-iOS maturity gap. `10-03` already ran a torture screen precisely so this would not be a surprise here; this task is where the findings get resolved.

**The escape hatch is contained and worth stating up front.** If a behaviour cannot be made right in Compose, host a `UITextField` in a `UIKitView` behind the *same* `TDTextField` API. Two files change; the 22 call sites do not. That is a legitimate outcome, not a failure — record it in `DECISIONS.md`.

**Previews genuinely regress and it is in scope.** `TDCustomPreviews.kt` builds its light+dark multipreviews on `Configuration.UI_MODE_NIGHT_*`; CMP's `@Preview` has no `uiMode`. `20-10` replaced them with a `darkTheme`-parameterised wrapper, which changes every preview signature in the module. `CLAUDE.md` treats missing previews as an incomplete change, so this is work, not cleanup.

## 3. Source — read before writing

| Path | LOC | Why |
|---|---|---|
| `uikit/…/components/TDTextField.kt` | 584 | **The main risk.** |
| `uikit/…/components/TDOutlinedTextField.kt` | 262 | Same. |
| `uikit/…/components/TDCompactOutlinedTextField.kt`, `TDLabeledTextField.kt` | — | Same family. |
| `uikit/…/theme/ComponentColors.kt` | 41 | M3 `TextFieldColors` adapter. **Typography bakes in a colour** — text fields need `.copy(color = onSurface)`, a documented trap in this codebase. |
| `uikit/…/previews/TDCustomPreviews.kt` | 79 | The six multipreview annotations |
| `uikit/…/components/TDButton.kt` | — | Exhaustive `when (TDTheme.palette)` for PRIMARY |
| `uikit/…/components/TDText.kt` | 126 | 479 call sites depend on it |
| Call sites of the text fields (22 files) | — | auth, chat, task title, search, journal, group names |

## 4. Target

No new files — verification and, if the torture screen demands it, a contained `UIKitView` fallback inside the two text-field files.

## 5. Steps

1. **Sweep the ~45 mechanical components.** Render each in a gallery, both platforms, light and dark, all three kits. Most need only a glance.

2. **Re-run the text-input torture screen from `10-03`** and work through the findings:
   - single-line, multiline, password with visibility toggle
   - the chat composer (multiline, grows, send action)
   - EN and TR keyboards, including Turkish-specific characters
   - IME insets with the keyboard open — the field must stay visible
   - selection handles, copy/paste, autocorrect, return-key behaviour
   - focus traversal between fields (login: email → password)
   - clearing, max length, error state

3. **Check `imePadding` on iOS.** There is a recorded lesson here: edge-to-edge zeroes the insets, and `imePadding` belongs on the Scaffold root. The iOS equivalent needs the same care or the keyboard covers the field being typed into.

4. **Check the trailing-icon slot stability.** Another recorded trap: a `TextField` trailing icon that flips between `null` and a composable causes a recomposition glitch. Gate *inside* the slot, never null↔lambda.

5. **Check the typography colour trap.** `TDTypography` styles bake in a colour; text fields must `.copy(color = ...)` or the text is invisible in one theme.

6. **Decide on the fallback.** If something is unusable, implement the `UIKitView` route inside `TDTextField`/`TDOutlinedTextField` and record it.

7. **Rebuild the previews.** Every component gets light + dark coverage through the new wrapper; screens get one per reachable `UiState`.

8. **Verify `TDButton`'s exhaustive palette `when`** still compiles exhaustively.

## 6. Code skeleton

```kotlin
// The trailing-icon slot must stay stable. Flipping between null and a composable
// causes a recomposition glitch — gate INSIDE the slot instead.
trailingIcon = {
    if (value.isNotEmpty()) {
        IconButton(onClick = onClear) { Icon(tdPainter(Res.drawable.ic_close), null) }
    }
}
```

```kotlin
// The escape hatch, if the torture screen demands it. Two files change; the 22
// call sites do not.
@Composable
internal expect fun PlatformTextFieldOrNull(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier,
): Boolean   // true = the platform field handled it; false = fall through to Compose
```

## 7. Acceptance

- [ ] All ~45 mechanical components render correctly on both platforms, light and dark, all three kits
- [ ] **Text input passes every item in step 2 on both platforms**
- [ ] Turkish characters (ı, ğ, ş, ç, ö, ü) type and render correctly
- [ ] Keyboard never covers the focused field
- [ ] Focus traversal works (email → password advances)
- [ ] Selection handles, copy/paste and autocorrect behave natively on iOS
- [ ] Trailing icons do not glitch on clear
- [ ] Text-field text is visible in both themes (the typography colour trap)
- [ ] Every component has a preview covering light + dark
- [ ] `TDButton`'s palette `when` is still exhaustive
- [ ] If the `UIKitView` fallback was used, it is recorded in `DECISIONS.md` with the specific behaviour that forced it

## 8. Pitfalls

- **Text input is the known CMP-on-iOS gap.** Budget real time here, and do not discover it at the end.
- **`imePadding` on the Scaffold root.** Edge-to-edge zeroes insets; this is a recorded lesson in this codebase.
- **Never flip a trailing icon between `null` and a composable.** Gate inside the slot.
- **`TDTypography` bakes in a colour.** Text fields need `.copy(color = ...)` or text is invisible in one theme.
- **Turkish casing.** `.uppercase()` without a locale mangles dotted/dotless i. Relevant to any component that uppercases a label.
- **Do not fork the text field per platform.** If the fallback is needed, it goes *behind* the same API, in the same file.
- **Previews are mandatory.** `CLAUDE.md` treats a component without previews as incomplete.
- **`TDText` has 479 call sites.** Any change to its signature is a large sweep — avoid it.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug

# The torture screen, both platforms, EN + TR keyboards
#   every item from step 2

# Real screens that depend on it
#   login (focus traversal), register (password strength), chat composer (multiline),
#   task title, search, journal entry, group name

# Component gallery, both platforms, light + dark, 3 kits
```
