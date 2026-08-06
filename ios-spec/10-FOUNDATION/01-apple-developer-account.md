---
id: 10-01
title: Apple Developer Program enrolment & App Store Connect setup
layer: foundation
status: TODO
depends_on: []
blocks: [10-03, 30-03, 30-10, 30-11, 60-03, 80-01, 80-02, 80-03, 80-04, 80-05]
parallel_safe: true
estimate: 2h of work, up to several weeks of waiting
reversible: true
owner_files: []
verify:
  - "Manual: App Store Connect shows the DoneBot app record with bundle id com.todoapp.mobile"
---

## 1. Goal

Have an active Apple Developer Program membership, a reserved app name, a registered bundle identifier with the required capabilities, an App Store Connect app record, an APNs authentication key, and an App Store Connect API key for automated uploads.

## 2. Why this way

**This is the longest-lead item in the entire project and it is not on the technical critical path.** Individual enrolment requires identity verification that routinely takes days and sometimes weeks. Everything in `20-MIGRATION` — roughly 500 of the ~880 total hours — needs no Apple account whatsoever. Start this on day one and let it run in the background.

**The app name is reserved, not claimed on submission.** "DoneBot" may already be taken on the App Store. Finding that out at submission time is a branding crisis; finding out in week one is a naming conversation. Reserve it as the very first App Store Connect action.

**Almost every step here is a human action.** If you are an agent: do what you can, record the rest in `BLOCKERS.md`, mark this `BLOCKED`, and move to `20-01`. Do not stall the queue on it.

## 3. Source — read before writing

| Path | What to look for |
|---|---|
| `app/build.gradle.kts` `defaultConfig` | `applicationId = "com.todoapp.mobile"` — the iOS bundle id mirrors it. Read `versionCode`/`versionName` from the file; they move. |
| `app/build.gradle.kts` (BuildConfig block) | `PRIVACY_POLICY_URL`, `TERMS_OF_SERVICE_URL`, `SUPPORT_EMAIL` — App Store Connect requires all three |
| `donebot prod/FAZ0_STORE_COPY.md` | Existing Play store copy — the basis for the App Store listing, not a copy-paste source |
| `ios-spec/00-CONTEXT/04-constraints.md` §1.6 | The full App Store requirement list |

## 4. Target

No files in this repository. Outputs are external artifacts, recorded in `90-STATE/PROGRESS.md`:

- Apple Developer Program membership (Team ID)
- Bundle identifier `com.todoapp.mobile` with capabilities enabled
- App Store Connect app record with the name reserved
- APNs authentication key (`.p8`) + Key ID + Team ID
- App Store Connect API key (`.p8`) + Key ID + Issuer ID
- Secrets stored outside the repository

## 5. Steps

1. **Check name availability first.** Search the App Store for "DoneBot". If taken, decide the fallback name *now*, before anything else is branded. Record the decision in `DECISIONS.md`.

2. **Enrol** at developer.apple.com → Account → Enrol. Individual (not Organization — Organization requires a D-U-N-S number and a registered legal entity).
   - Legal name exactly as on the government ID. Not a nickname, not a brand.
   - Payment: Turkish cards are frequently declined. Have a Wise / Revolut / Payoneer card ready.
   - Tax: W-8BEN, tax residence Turkey, Turkish national ID number.
   - 99 USD/year.

3. **Complete identity verification.** Government photo ID. This is the step that takes weeks. Nothing below can start until it clears.

4. **Accept agreements** in App Store Connect → Business. The Free Apps agreement is required even for a free app.

5. **Register the bundle identifier** — Certificates, Identifiers & Profiles → Identifiers → App IDs → App. Use `com.todoapp.mobile` to mirror the Android `applicationId`.

   Enable these capabilities (each one is a rejection or a build failure if missed later):

   | Capability | Needed by |
   |---|---|
   | Push Notifications | `30-03` |
   | Sign in with Apple | `60-03` — **submission blocker** |
   | App Groups (`group.com.todoapp.mobile`) | `60-01` widgets, `30-04` Live Activity |
   | Associated Domains | `30-13` universal links |
   | App Attest | `30-11` Firebase App Check |

6. **Create the App Store Connect app record.** Platform iOS, name reserved, primary language English, bundle id from step 5, SKU `donebot-ios`. Add Turkish as a second localization.

7. **Create the APNs authentication key** — Keys → new key → Apple Push Notifications service. Download the `.p8` **once** (it cannot be re-downloaded). Record Key ID and Team ID. This goes into Firebase in `30-11`.

8. **Create an App Store Connect API key** — Users and Access → Integrations → App Store Connect API. Role: App Manager. Download the `.p8` once. Record Key ID and Issuer ID. Used by `80-04`/`80-05` for automated uploads.

9. **Store the secrets outside the repository.** Both `.p8` files, the Team ID, Key IDs and Issuer ID. The repository `.gitignore` already covers `*.p8`, but do not rely on that — keep them out of the working tree entirely. Record *where* they live (not their contents) in `PROGRESS.md`.

## 6. Code skeleton

No code. The identifiers produced here are consumed later as:

```
TEAM_ID          → iosApp/ExportOptions.plist, Xcode signing
BUNDLE_ID        → com.todoapp.mobile
APP_GROUP_ID     → group.com.todoapp.mobile
APNS_KEY_ID      → Firebase Console → Cloud Messaging → APNs auth key
APNS_KEY_FILE    → AuthKey_<KEYID>.p8   (outside the repo)
ASC_KEY_ID       → xcrun altool --apiKey
ASC_ISSUER_ID    → xcrun altool --apiIssuer
ASC_KEY_FILE     → AuthKey_<KEYID>.p8   (outside the repo)
```

## 7. Acceptance

- [ ] Membership shows **Active** in the Apple Developer account
- [ ] App name searched on the App Store; availability confirmed or a fallback recorded in `DECISIONS.md`
- [ ] Bundle id `com.todoapp.mobile` registered
- [ ] All five capabilities from step 5 enabled
- [ ] App Store Connect app record exists, name reserved, EN + TR localizations added
- [ ] APNs `.p8` downloaded and stored outside the repo; Key ID + Team ID recorded
- [ ] App Store Connect API `.p8` downloaded and stored outside the repo; Key ID + Issuer ID recorded
- [ ] No `.p8` file, key id or team id appears anywhere in `git status`

## 8. Pitfalls

- **`.p8` keys download exactly once.** Losing one means revoking and reissuing, and for APNs that means every existing device token stops receiving pushes until Firebase is updated.
- **Individual vs Organization is not reversible in place.** Individual is correct for a solo developer. Organization requires a D-U-N-S number and a registered company, and switching later is a migration, not a setting.
- **The legal name on the account becomes the App Store seller name.** For an individual account it is your legal name, publicly visible. Know this before enrolling.
- **App Groups id format.** `group.com.todoapp.mobile` — the `group.` prefix is mandatory. Widgets and Live Activities silently read an empty container if it is wrong.
- **Sign in with Apple must be enabled on the App ID before the entitlement will work.** Enabling it later requires regenerating provisioning profiles.
- **Do not enable capabilities you will not use.** Each one adds entitlements that App Review may ask you to justify.
- **Enrolment can stall silently.** If verification exceeds two weeks, contact Apple Developer Support rather than waiting — the process does occasionally get stuck.

## 9. Verification

Manual, in the Apple Developer portal and App Store Connect:

```
developer.apple.com/account            → Membership: Active, Team ID recorded
developer.apple.com/account/resources/identifiers   → com.todoapp.mobile present
                                          with Push, Sign in with Apple, App Groups,
                                          Associated Domains, App Attest enabled
appstoreconnect.apple.com/apps         → DoneBot record exists, EN + TR
developer.apple.com/account/resources/authkeys      → APNs key listed
appstoreconnect.apple.com → Users and Access → Integrations  → API key listed
```

In this repository:

```bash
git status --porcelain | grep -E '\.p8$' && echo "LEAK - a .p8 is in the tree" || echo "clean"
```
