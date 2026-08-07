# BLOCKERS

Append-only. Never rewrite or delete history — a resolved blocker gets a `**RESOLVED**` line appended, not removal.

**When to write here:** a task needs something only a human can provide (account, payment, device, secret), a `verify:` command fails in a way you cannot fix, a dependency does not exist, or you find real work that belongs to no task file.

**After writing here:** set the task `BLOCKED` in `PROGRESS.md` and immediately pick the next available task. Never idle.

Format:

```markdown
## [YYYY-MM-DD] <task-id> — <short title>
**Needs:** what exactly is required, and from whom.
**Impact:** which task ids this blocks.
**Workaround:** what can proceed in the meantime.
**Tried:** what you attempted, with the exact error if any.
```

---

## [2026-08-06] ADR-001 — cannot delete the `DoneBot-iOS` repository

**Needs:** a human. The `gh` token is missing the `delete_repo` scope, so the API refuses with `HTTP 403: Must have admin rights to Repository`.

**Impact:** cosmetic only. Nothing depends on the repository being gone; it is empty and unreferenced. It is recorded here so a later session does not mistake it for the intended home of iOS source (see ADR-001).

**Workaround:** either
```bash
gh auth refresh -h github.com -s delete_repo    # interactive, needs a browser
gh repo delete beratbaran40/DoneBot-iOS --yes
```
or delete it from the GitHub web UI: Settings → General → Danger Zone.

**Tried:** `gh repo delete beratbaran40/DoneBot-iOS --yes` → 403, scope missing. Current scopes: `gist, read:org, repo`.

**RESOLVED [2026-08-06]** — the owner deleted the repository manually. Verified: `gh repo view beratbaran40/DoneBot-iOS` returns `Could not resolve to a Repository`. The account now holds `DoneBot`, `DoneBot-Backend` and `DoneBot-Admin` only. ADR-001 stands: one repository, `iosApp/` at the root of this one.

---

## [2026-08-06] 10-01 — Apple Developer Program enrolment

**Needs:** a human. Individual enrolment, 99 USD/yr. Identity verification with a government photo ID; Turkish cards are frequently declined, so a Wise/Revolut/Payoneer card may be required. W-8BEN plus a Turkish national ID number.

**Impact:** blocks `10-03` (iOS app shell — needs a signing team), `30-03` (APNs auth key), `30-10`/`60-03` (Sign in with Apple capability), `30-11` (Firebase iOS app registration), and all of `80-RELEASE`.

**Workaround:** substantial. Everything in `20-MIGRATION` (the largest phase, ~500 hours) needs no Apple account at all, and simulator development works without a paid membership. Start enrolment immediately and work the migration in parallel — by design this is not on the critical path until `10-03`.

**Tried:** nothing yet — this is a day-one action item, recorded here so no agent re-attempts it.

**AMENDED [2026-08-07]** — the impact line above overstated the blast radius, and the dependency graph agreed with it. Measured: `10-01` alone made **54 of 105 tasks unreachable**, because `10-03` claimed to need a paid team (it does not — this entry's own workaround line says so) and `30-10` bundled Google with Apple, dragging login and 41 downstream tasks behind `70-01`. Corrected in ADR-004/005/006. **Revised impact: `30-03`, `30-11`, `60-03`, `80-01` and, transitively, the rest of `80-RELEASE` — 10 of 107 tasks.** With `10-01` blocked, 97 tasks / 1,456 h remain reachable. Enrolment is still a day-one action item; it is simply no longer a wall.

---

## [2026-08-06] 10-00 — macOS upgrade required before Xcode

**Needs:** a human. The machine runs **macOS 14.5 (Sonoma)**. Xcode 26 requires **macOS 15.6 (Sequoia)** minimum; Xcode 26.4 requires **macOS Tahoe 26.2**. Xcode 26 is mandatory for any App Store submission since 28 April 2026.

**Impact:** blocks every iOS build task. Does **not** block `20-MIGRATION`.

**Workaround:** do the upgrade in week 1, **before any code changes**, so a rollback costs nothing. Take a bootable backup first. Re-run the full Android CI gate after the upgrade and before touching code, so any OS-induced breakage is isolated from migration changes.

**Tried:** verified current state — `sw_vers` reports 14.5 (build 23F79); `xcodebuild` reports only Command Line Tools present; no iPhoneOS SDK; `xcrun simctl` unavailable.

**RE-SCOPED [2026-08-07] → this blocker now belongs to `10-05`, not `10-00`.** The two were one task, which put a human-only OS upgrade on the critical path of the entire migration: `10-00` blocked `20-01`, and its own step told an agent to mark itself `BLOCKED` and "move on to `20-01`" — which the pick rule forbids. `10-00` is now the 2-hour JDK pin only; `10-05` owns macOS + Xcode and blocks exactly `10-03` and `20-13` (ADR-004).

Two corrections to the text above:
- **"do the upgrade in week 1" is no longer required.** 32 tasks / **712 hours** run with no Xcode installed at all. The real deadline is *before `20-11` starts*, so a failed upgrade does not stall the moment iOS switches on.
- **`20-13` genuinely needs full Xcode**, not just Command Line Tools — Kotlin/Native links against the real iOS SDK via `xcrun`. That dependency was missing from the graph entirely and is now explicit.

Still open: the machine is still on 14.5. This stays `BLOCKED` until a human does it.

---

## [2026-08-06] 10-04 — `gh` token lacks the `workflow` scope

**Needs:** a human to re-authenticate. Current scopes are `gist, read:org, repo`. Creating or editing `.github/workflows/*` through the GitHub API fails without `workflow`.

**Impact:** partially blocks `10-04` (CI changes) if attempted via the API.

**Workaround:** edit `.github/workflows/ci.yml` as a normal file and push over SSH — the existing remote is SSH and works. Only the API path is blocked.

**Tried:** `gh auth status` — logged in as `beratbaran40`, scopes as above.

---

## [2026-08-06] 20-00 — `keystore.properties` is absent

**Needs:** a human to create `keystore.properties` at the repo root with `storeFile`, `storePassword`, `keyAlias`, `keyPassword`. The keystore itself is present at `~/donebot-upload.jks` (valid to 2053).

**Impact:** blocks shipping the signed v1.2 AAB (migration step 0). Does not block any code task — when the file is absent the release build simply produces an unsigned AAB.

**Workaround:** all migration work proceeds; only the Play upload is gated. `local.properties` is present and the version fields in `app/build.gradle.kts` are current — this is the only remaining item.

**Tried:** verified `keystore.properties` does not exist; `~/donebot-upload.jks` does.
