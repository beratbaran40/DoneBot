---
id: 40-core-05
title: Search
layer: ui
status: TODO
depends_on: [40-core-01, 50-04]
blocks: []
parallel_safe: true
estimate: 8h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**/search/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
---

## 1. Goal

Global task and group search with filters.

## 2. Why this way

**Search is a per-keystroke text-input surface over a growing dataset**, which makes it the third place (after the chat composer and the password meter) where iOS text latency would show.

**Two recorded lessons apply.** A controlled `TextField` must bind the immediate value and derive the debounced query from it — binding the debounced value directly makes typing feel laggy. And `\b` word boundaries do not work for Turkish characters in Java regex: `ı ç ş ğ ö ü` are treated as non-word, so a trailing `\b` fails to match. Use `(?![\p{L}\p{N}])` instead. If search does any word-boundary matching, this affects half the user base.

**Secret tasks are filtered here**, so secret mode gating applies.

## 3. Source

| Path | LOC |
|---|---|
| `ui/search/` (6 files) | 1,453 |
| `ui/search/SearchGroupItems.kt` | `when (TDTheme.palette)` ramp |
| `domain/usecase/security/IsSecretModeActiveUseCase.kt` | secret filtering |
| `docs/screenshots/search/` | references |

## 4. Target

`shared/ui/commonMain/…/ui/search/` — verification.

## 5. Steps

1. Verify all 6 files compile in `commonMain`.
2. Verify typing is responsive on iOS — immediate binding, derived debounce.
3. **Verify Turkish-character matching** — search for a term containing `ı`, `ş`, `ğ`.
4. Verify the filters dialog.
5. Verify group results render with the palette ramp.
6. Verify secret tasks are hidden unless secret mode is active.
7. Verify empty and no-results states.
8. Three kits, two themes, two languages.

## 7. Acceptance

- [ ] All 6 files compile in `commonMain`
- [ ] Typing is responsive; no input lag on iOS
- [ ] **Turkish characters match correctly**
- [ ] Filters dialog works
- [ ] Group results render with the palette ramp
- [ ] Secret tasks hidden unless secret mode is active
- [ ] Empty and no-results states render
- [ ] Three kits, two themes, two languages
- [ ] Previews cover empty, results, no-results and filters-open

## 8. Pitfalls

- **Bind the immediate value, derive the debounce.** Binding the debounced value makes typing feel broken.
- **`\b` fails on Turkish characters.** Use `(?![\p{L}\p{N}])`.
- **Secret tasks must stay hidden** outside secret mode — a privacy feature, not a filter.
- **Per-keystroke search over a growing dataset** needs the debounce; do not remove it.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
# Both platforms: type quickly and check responsiveness; search "ışık", "şey", "ğ";
# filters; secret mode on and off; EN + TR
```
