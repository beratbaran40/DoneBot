---
id: 80-01
title: App Store Connect setup
layer: release
status: TODO
depends_on: [10-01]
blocks: [80-04]
parallel_safe: true
estimate: 6h
reversible: true
owner_files: []
verify:
  - "Manual: the app record is complete enough to accept a build"
---

## 1. Goal

An App Store Connect record ready to receive builds and, later, a submission: identity, pricing, availability, age rating and the App Privacy questionnaire.

## 2. Why this way

Most of this is metadata that blocks submission but not development, so it is worth completing early — the App Privacy questionnaire in particular takes real thought and is easy to get wrong under deadline.

**The privacy answers must match what the app actually does**, not what a template suggests. DoneBot's posture is unusually clean and should be declared accurately:

- **No AdID.** Android removes the `AD_ID` permission trio; iOS does not link `AdSupport`. So the answer to tracking is genuinely **no**, and there is no App Tracking Transparency prompt.
- **Analytics and crash reporting are consent-gated.** Crashlytics + Analytics are opt-out (default on), Performance is opt-in (default off). The questionnaire has no "consent-gated" nuance, so declare the data types that *can* be collected.
- **The journal never leaves the device.** Do not declare it as collected.
- **Location is never accessed.** The picker is text-search only, with no location permission on either platform.

**Reserve the name first.** `10-01` covers it; if "DoneBot" was taken, the fallback decision should already be recorded.

## 3. Source — read before writing

| Path | Why |
|---|---|
| `donebot prod/FAZ0_STORE_COPY.md` | The Play listing copy — the basis for App Store copy, not a paste source (character limits and tone differ) |
| `app/build.gradle.kts` | `PRIVACY_POLICY_URL`, `TERMS_OF_SERVICE_URL`, `SUPPORT_EMAIL` |
| `docs/analytics-events.md` | What Analytics actually collects |
| `data/source/remote/api/ToDoApi.kt` | What the backend stores — the basis for the data-collection answers |
| `ios-spec/00-CONTEXT/04-constraints.md` §1.6 | The requirement list |
| Play Console Data Safety declaration | The existing, reviewed answers — the closest available reference |

## 4. Target

No files in this repository. Outputs live in App Store Connect and are recorded in `PROGRESS.md`.

## 5. Steps

1. **Complete app information**: name, subtitle, category (Productivity), content rights, age rating.

2. **Set pricing and availability.** Free. Decide the territory list — worldwide unless there is a reason.

3. **Fill the App Privacy questionnaire.** Work from what the backend actually stores plus the Firebase products in use:
   - **Contact info** — email, name (account)
   - **User content** — tasks, groups, photos (backend); **not** journal (device-only)
   - **Identifiers** — user id, device token
   - **Usage data** — analytics events, consent-gated
   - **Diagnostics** — crash and performance data, consent-gated
   - **Tracking: NO.** No AdID, no cross-app tracking, no ATT prompt.

4. **Add both localizations**, EN and TR, matching the app's supported languages.

5. **Set the support URL and marketing URL.** Support must be reachable — reviewers check.

6. **Set the privacy policy URL** from `BuildConfig.PRIVACY_POLICY_URL` (served by the backend at `/legal/privacy.html`).

7. **Create a demo account** with realistic data, **including a group with at least one other member**, so a reviewer can exercise the social features. Record the credentials for the review notes.

8. **Set the copyright and the seller name.** For an individual account this is your legal name and it is publicly visible.

## 6. Code skeleton

No code. The App Privacy answers, for the record:

```
Data collected and linked to the user
  Contact Info    → Email Address, Name        (App Functionality, Account Management)
  User Content    → Photos, Other User Content (App Functionality)
  Identifiers     → User ID, Device ID         (App Functionality)
  Usage Data      → Product Interaction        (Analytics)     — consent-gated
  Diagnostics     → Crash Data, Performance    (App Functionality) — consent-gated

Tracking: NO
  No AdSupport, no AppTrackingTransparency, no AD_ID. No ATT prompt is shown.

Not collected
  Location — the picker is text search only; no location permission on either platform.
  Journal entries and journal photos — device-only, never transmitted.
```

## 7. Acceptance

- [ ] App record exists with the reserved name
- [ ] Category, age rating, content rights complete
- [ ] Pricing (free) and availability set
- [ ] App Privacy questionnaire complete and **accurate**
- [ ] Tracking answered **No**
- [ ] Journal data **not** declared as collected
- [ ] Location **not** declared
- [ ] EN and TR localizations added
- [ ] Support, marketing and privacy policy URLs set and reachable
- [ ] Demo account created with realistic data **including a group**; credentials recorded
- [ ] The record accepts a build upload

## 8. Pitfalls

- **Do not copy the Play Data Safety answers mechanically.** The taxonomies differ and Apple's is more granular.
- **Answering "Yes" to tracking triggers ATT.** The app does not track; answering yes would require a prompt it does not show and is simply wrong.
- **Do not declare journal data as collected.** It never leaves the device. Over-declaring is not "safe" — it is inaccurate and it makes the label worse for no reason.
- **The demo account must have a populated group.** A reviewer who cannot exercise the social features may reject for incomplete functionality.
- **The support URL must work.** Reviewers check it.
- **The seller name is public.** For an individual account it is your legal name.
- **Privacy answers can be updated**, but a mismatch found during review costs a cycle.

## 9. Verification

```
appstoreconnect.apple.com → the app record
  App Information   → name, subtitle, category, age rating, content rights
  Pricing           → free, territories
  App Privacy       → questionnaire complete, Tracking = No
  Localizations     → EN + TR
  App Review        → demo credentials present
curl -I <privacy policy URL>   → 200
curl -I <support URL>          → 200
```
