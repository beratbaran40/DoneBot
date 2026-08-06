# DECISIONS — in-flight ADRs

Append-only. Decisions made **during execution**, when a task file was ambiguous, wrong, or silent on something that mattered. Binding on every later session.

Architecture decisions locked before execution live in `../00-CONTEXT/01-decision-record.md` and are **not** revisited here. This file is for what the spec did not anticipate.

**Write an ADR when:** you deviate from a task file, choose between two defensible implementations, discover a spec error, or make a choice a later agent could plausibly reverse by accident.

Format:

```markdown
## ADR-NNN [YYYY-MM-DD] <short imperative title>
**Task:** <task-id>
**Context:** what forced a decision.
**Decision:** what you did.
**Alternatives:** what you rejected, and why.
**Consequence:** what this constrains later. Include the test or gate that locks it, if any.
```

---

## ADR-001 [2026-08-06] Single repository — `DoneBot-iOS` is not used

**Task:** planning

**Context:** A private `DoneBot-iOS` repository was created early, before the monorepo decision (D-02) was settled. Once D-02 landed, a second repository had no source to hold, and keeping an empty repository around invites a later session to "helpfully" start putting iOS code in it.

**Decision:** Everything lives in this repository — Kotlin, Swift, the spec, release runbooks and store assets. `DoneBot-iOS` is deleted.

**Alternatives rejected:**
- *Keep it for release/store assets* — splits the release process across two repositories for no gain; the AAB size budget applies to the app bundle, not the git repo, so keeping assets out of this repo buys nothing.
- *Keep it reserved but empty* — an empty repository with a plausible name is exactly the kind of thing a future session mistakes for the intended home of iOS code.
- *XCFramework published from here, consumed there* — forces a publish→version→consume round trip on every shared change; unacceptable friction for a single developer.

**Consequence:** `iosApp/` lives at the root of this repository, sibling to `app/`. Any proposal to move iOS source out reopens D-02 and must be argued there.

**Status:** deletion pending — the `gh` token lacks the `delete_repo` scope. See `BLOCKERS.md`.

---

## ADR-003 [2026-08-06] Rename the repository and Gradle root project to `DoneBot`

**Task:** planning

**Context:** `DoneBot-Android` describes a repository that is about to contain the iOS app as well.

**Decision:** Renamed on GitHub (`beratbaran40/DoneBot`), local remote updated, `rootProject.name` in `settings.gradle.kts` changed to `DoneBot`, and the seven `DoneBot-Android` references in `README.md` / `README.tr.md` (CI badges and clone instructions) updated. Verified with `./gradlew assembleDebug` — build successful.

**Alternatives rejected:** *Rename later, during migration* — the rename touches the remote URL and CI badge URLs; doing it while the tree is otherwise clean makes it a one-line-per-file diff instead of a merge hazard.

**Consequence:** GitHub redirects the old URL, so existing clones keep working, but any external link or bookmark should be updated. `rootProject.name` changes the Gradle project identity, which invalidates the build cache once — expected, not a defect.

---

## ADR-002 [2026-08-06] Spec files are written in English

**Task:** planning

**Context:** The user communicates in Turkish; their working documents (`donebot prod/`) are Turkish. The codebase, `CLAUDE.md`, the public README and every symbol, path and command are English.

**Decision:** Task files are English. Progress reporting to the user is Turkish.

**Alternatives rejected:** *Turkish spec* — every code identifier, Gradle task, file path and API name would still be English, producing constant code-switching mid-sentence and inviting term drift between instruction and implementation.

**Consequence:** If the user prefers Turkish, this is a mechanical translation of the prose sections; the front-matter, commands and code skeletons stay as they are.
