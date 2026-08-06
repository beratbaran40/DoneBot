---
id: 50-06
title: Adaptive layout & iPad
layer: design
status: TODO
depends_on: [20-11]
blocks: [80-03]
parallel_safe: true
estimate: 24h
reversible: true
owner_files:
  - shared/ui/src/commonMain/**
  - composeApp/src/commonMain/**
  - iosApp/**
verify:
  - ./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
  - "xcodebuild -destination 'platform=iOS Simulator,name=iPad Pro 13-inch (M4)' build"
---

## 1. Goal

Ship a Universal app: iPad gets the navigation rail, the two-pane layouts and correct behaviour in Split View and Slide Over.

## 2. Why this way

**Most of this already exists.** Android's tablet support is complete — `ResponsiveContainer`, `TDNavigationRail`, `GroupsTwoPane`, a 720dp breakpoint and window size classes threaded through the UI. And `material3-window-size-class` is available in common code, so `20-11` already replaced `calculateWindowSizeClass(this)` with the no-arg form and made `LocalWindowSizeClass` platform-free.

So this is not "build iPad support." It is "verify the existing adaptive layout on a different device family, and handle the three things iPad does that Android tablets do not."

**Those three things are the actual work:**

1. **Split View and Slide Over.** The window can be *any* width, and it can change while the app runs, without a configuration change. A layout that reads the size class once at composition and caches it will be wrong the moment the user drags the divider.
2. **The keyboard is detachable.** Hardware keyboards are common on iPad. Focus traversal and keyboard shortcuts matter more than on phone.
3. **Screenshots.** A Universal app requires 13" iPad screenshots for the App Store listing — a real deliverable, not a checkbox.

**iPad also inherits an existing crash risk.** `UIActivityViewController` (the share sheet, via `ExternalLinks`) **crashes on iPad without a `popoverPresentationController` source**. `30-15` covers it; verify it here on the actual device family.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `ui/common/ResponsiveContainer.kt` | The width-based layout switch |
| `navigation/TDNavigationRail.kt` (92 LOC) | The rail; exhaustive `when (TDTheme.palette)` for icon tint |
| `navigation/TDBottomBar.kt` (144 LOC) | The phone counterpart |
| `ui/groups/GroupsTwoPane.kt` | The worked two-pane example |
| `MainActivity.kt` → `LocalWindowSizeClass` | Now platform-free after `20-11` |
| `docs/screenshots/` | Phone references; iPad has none yet |
| `shared/ui/…/platform/ExternalLinks` (`30-15`) | The share-sheet popover source |

## 4. Target

- Verification across `shared/ui/commonMain`
- `iosApp/` — `UISupportedInterfaceOrientations~ipad`, `UIRequiresFullScreen` decision
- New iPad screenshots for `80-03`

## 5. Steps

1. **Run on the iPad simulator** and check every one of the 43 destinations. Most will be correct already.

2. **Verify the rail appears** at expanded width and the bottom bar at compact.

3. **Test Split View and Slide Over.** Drag the divider while the app is running — the layout must respond live. This is the most likely defect.

4. **Verify two-pane screens.** Groups is the worked example; check that selection state survives a width change.

5. **Decide on `UIRequiresFullScreen`.** Setting it to `true` opts out of Split View entirely and simplifies everything — but Apple discourages it and it can draw reviewer attention. **Recommended: support multitasking**, since the adaptive layout already exists. Record the decision.

6. **Set iPad orientations.** Unlike phone, iPad apps are expected to support all four. The polaroid camera stays portrait-locked, which is acceptable for a single screen.

7. **Test with a hardware keyboard.** Focus traversal through forms, Escape to dismiss sheets, Return to submit.

8. **Verify the share sheet does not crash.** This is the inherited iPad-specific crash.

9. **Check text sizing.** Fixed dp on a 13" screen can look small. Verify the type ramp reads well at that size rather than assuming.

10. **Capture 13" screenshots** for the listing.

## 6. Code skeleton

```kotlin
// Read the size class from the composition, never cached. On iPad the window width
// changes live when the user drags the Split View divider — there is no configuration
// change to invalidate a cached value.
@Composable
fun AdaptiveScaffold(content: @Composable () -> Unit) {
    val widthClass = LocalWindowSizeClass.current.widthSizeClass
    when (widthClass) {
        WindowWidthSizeClass.Compact -> PhoneLayout(content)
        else -> RailLayout(content)      // Medium and Expanded
    }
}
```

```swift
// iosApp/Info.plist — iPad users expect all four orientations.
<key>UISupportedInterfaceOrientations~ipad</key>
<array>
  <string>UIInterfaceOrientationPortrait</string>
  <string>UIInterfaceOrientationPortraitUpsideDown</string>
  <string>UIInterfaceOrientationLandscapeLeft</string>
  <string>UIInterfaceOrientationLandscapeRight</string>
</array>
```

## 7. Acceptance

- [ ] The app builds and runs on the iPad simulator
- [ ] All 43 destinations render correctly at iPad sizes
- [ ] Navigation rail at expanded width; bottom bar at compact
- [ ] **Split View: dragging the divider re-lays out live**, with no crash and no stale layout
- [ ] Slide Over works
- [ ] Two-pane screens keep selection state across a width change
- [ ] All four orientations supported on iPad
- [ ] Hardware keyboard: focus traversal, Escape, Return all work
- [ ] **Share sheet does not crash on iPad**
- [ ] Type ramp reads well at 13"
- [ ] Both bars tint icons correctly in all three palette kits
- [ ] 13" iPad screenshots captured for the listing
- [ ] The `UIRequiresFullScreen` decision is recorded in `DECISIONS.md`

## 8. Pitfalls

- **Never cache the window size class.** iPad width changes live with no configuration change. This is the defect most likely to ship.
- **`UIActivityViewController` crashes on iPad without a popover source.** Inherited from `30-15`; verify it here.
- **`UIRequiresFullScreen = true` is a tempting shortcut.** It works, but it opts out of multitasking that the adaptive layout already supports, and reviewers notice.
- **iPad expects all four orientations.** Phone-style portrait-only looks broken on a tablet.
- **Fixed dp does not scale to 13".** Check readability rather than assuming the phone ramp transfers.
- **Sheets present differently on iPad** — as a form sheet or popover, not a bottom sheet. Check every sheet in the app.
- **Do not build a separate iPad UI.** The adaptive layout exists; use it.
- **Screenshots are a real deliverable.** A Universal app cannot be submitted without 13" images.

## 9. Verification

```bash
./gradlew ktlintCheck detektAll testDebugUnitTest assembleDebug
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp \
  -destination 'platform=iOS Simulator,name=iPad Pro 13-inch (M4)' build

# On an iPad (or the simulator)
#   all 43 destinations
#   Split View: drag the divider slowly while on Home, Groups, Calendar
#   Slide Over
#   rotate through all four orientations
#   hardware keyboard: tab through the login form, Escape a sheet
#   share a task → sheet appears, no crash
#   all three palette kits
```
