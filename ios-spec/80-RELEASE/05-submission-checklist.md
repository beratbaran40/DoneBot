---
id: 80-05
title: Submission checklist
layer: release
status: TODO
depends_on: [80-03, 80-04, 60-03]
blocks: []
parallel_safe: false
estimate: 10h
reversible: true
owner_files: []
verify:
  - "App Review submission accepted; processingState VALID"
---

## 1. Goal

Submit DoneBot to App Review with everything in place, and know what to do when it comes back.

## 2. Why this way

**Most rejections are avoidable and predictable.** This checklist is ordered by how often each item actually causes one, drawn from the guidelines that apply to this specific app rather than a generic list.

**Two items are hard blockers for DoneBot specifically:**

1. **Sign in with Apple** (Guideline 4.8) — mandatory because Google Sign-In is offered.
2. **In-app account deletion** (Guideline 5.1.1(v)) — already implemented; verify it is reachable on iOS.

**The review notes matter more than people expect.** A reviewer who cannot log in, or cannot see the social features, rejects for incomplete functionality — and that costs a full cycle for a reason that has nothing to do with the app.

**Have a rejection plan.** Most rejections are a short conversation in Resolution Center, not a rebuild. Read what was actually cited, respond with specifics, and resubmit. Guessing at a fix and resubmitting blindly costs another cycle.

## 3. Source — read before writing

| Path | Why |
|---|---|
| `ios-spec/00-CONTEXT/04-constraints.md` §1.6 | The requirement list |
| `ios-spec/80-RELEASE/02-privacy-and-compliance.md` | Privacy manifest and copy audit |
| `ios-spec/90-STATE/PROGRESS.md` | Milestone gates M0–M9 |
| `donebot prod/LAUNCH_CHECKLIST.md` | The Play submission tracker — the model for this one, with `[kod]` / `[sen]` tagging |

## 4. Target

No files. The output is a submitted build.

## 5. Steps

1. **Verify every milestone gate M0–M8** is complete in `PROGRESS.md`.

2. **Work the blocker list:**
   - [ ] Sign in with Apple present, prominent, functional (`60-03`)
   - [ ] In-app account deletion reachable and working
   - [ ] Privacy manifest correct; no upload warnings (`80-02`)
   - [ ] App Privacy answers match the manifest (`80-01`)
   - [ ] All required screenshot sizes, EN and TR, **including 13" iPad** (`80-03`)
   - [ ] Demo account with a populated group, credentials in review notes
   - [ ] Support URL reachable
   - [ ] Export compliance declared
   - [ ] Age rating complete
   - [ ] No placeholder text or debug UI anywhere

3. **Write the review notes.** Include: demo credentials, a note that the demo account has a group so social features can be exercised, an explanation of what the AI assistant does and that it runs through the app's own backend, and a note that reminders are local notifications with a documented coverage window.

4. **Verify guideline-specific items for this app:**
   - **2.1 Completeness** — no broken features, no placeholders
   - **4.8 Login Services** — Sign in with Apple
   - **5.1.1(v) Account deletion** — in-app
   - **5.1.2 Data use** — matches the privacy declarations
   - **1.2 User-generated content** — groups have reporting (`POST family-groups/{id}/reports`) and blocking (`BlockedUsersPreferences`); make sure both are visible to a reviewer
   - **3.1.1 In-app purchase** — none; nothing to declare
   - **2.5.1 Private APIs** — none used

5. **Do a final device pass** on the exact build being submitted: all 43 destinations, both languages, all three palette kits, iPhone and iPad.

6. **Submit**, choosing manual release so launch timing is yours.

7. **Watch Resolution Center.** Typical review is 24–48 hours.

8. **If rejected:** read the citation, reproduce it, fix precisely that, and reply with specifics. Do not resubmit on a guess.

## 6. Code skeleton

No code. Review notes template:

```
DEMO ACCOUNT
  Email:    reviewer@donebot.app
  Password: <recorded in PROGRESS.md>
  This account has an existing shared group with another member, so the group
  features (invitations, shared tasks, activity feed, reporting, blocking) can
  be exercised without creating a second account.

ABOUT THE ASSISTANT
  "DoneBot" is an in-app assistant. Simple requests (today's tasks, starting a
  pomodoro) are handled on-device. Anything else goes through our own backend,
  which calls a language model server-side. It creates and edits the user's own
  tasks only; it cannot write to shared group tasks.

ABOUT REMINDERS
  Reminders are local notifications. Because iOS limits an app to 64 pending
  requests, the app schedules a rolling window and tells the user in Settings
  how far ahead it is currently scheduled ("Scheduled through <date>").

NOTES
  No account is required to use the app — guest mode keeps data on device.
  The journal is stored only on the device and is never uploaded.
```

## 7. Acceptance

- [ ] M0–M8 complete
- [ ] Every blocker in step 2 checked
- [ ] Review notes written, including demo credentials and the group note
- [ ] Final device pass on the submitted build — iPhone and iPad, both languages, all three kits
- [ ] Build uploaded, `processingState == VALID`
- [ ] Submitted with manual release
- [ ] M9 marked in `PROGRESS.md`

## 8. Pitfalls

- **A reviewer who cannot log in rejects for incomplete functionality.** Verify the demo credentials on a clean device before submitting.
- **A demo account without a group means the social features cannot be reviewed** — same outcome.
- **Sign in with Apple is not optional.** Guideline 4.8, because Google Sign-In is offered.
- **Account deletion must be in-app**, not an email instruction.
- **Missing 13" iPad screenshots** — mandatory for a Universal app.
- **Debug UI left in a release build** is a common and embarrassing rejection.
- **Do not resubmit on a guess.** Reproduce the citation first. A wrong fix costs another full cycle.
- **Manual release.** Automatic release ships the moment review passes, possibly at 3am.
- **Version and build numbers must be higher than any previously uploaded**, including builds that were deleted — the same rule Play has, and it has already bitten this project once on Android.

## 9. Verification

```bash
# Build is ready to submit
curl -s -H "Authorization: Bearer $ASC_JWT" \
  "https://api.appstoreconnect.apple.com/v1/apps/$APP_ID/builds" \
  | jq '.data[0].attributes | {version, processingState}'

# Then, in App Store Connect: submit for review, manual release.
# Watch Resolution Center for the outcome.
```
