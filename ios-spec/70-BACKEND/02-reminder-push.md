---
id: 70-02
title: Backend — reminder push, timezone & locale
layer: backend
status: TODO
depends_on: [30-01, 30-03]
blocks: [80-04]
parallel_safe: true
estimate: 28h
reversible: true
owner_files:
  - "~/AndroidStudioProjects/ToDoBackend/**"
verify:
  - "A lapsed iOS user receives a reminder push for a task beyond their coverage horizon"
---

> **Executed in the backend repository**, `~/AndroidStudioProjects/ToDoBackend`. Not in this repo.

## 1. Goal

Give iOS a server-side safety net for reminders that fall beyond the device's 64-slot coverage horizon — and fix three real defects in the existing due-soon job along the way.

## 2. Why this way

**The existing `task_due_soon` job is not the safety net it looks like.** It runs on a 5-minute tick with a ~20-minute horizon, and it has three concrete limitations:

1. It covers **group tasks only**. Personal reminders — the overwhelming majority — get nothing.
2. Its text is **hard-coded English**. Turkish users get English pushes.
3. It uses **one fixed timezone**, because the `users` table has no timezone column.

So the assumption that push already compensates for iOS's scheduling limits is wrong, and the fix is worth doing on its own merits: two of the three are bugs that affect Android users today.

**The design keeps push volume near zero.** The client reports its coverage horizon after every successful reminder rebuild. The job only pushes for reminders firing **beyond** that horizon. An active user's horizon is always ~21 days out, so they receive nothing; a lapsed user receives exactly the reminders that would otherwise be lost.

**It rides the existing 5-minute tick.** No new scheduled job, no new Neon wakeups — which matters, because `NEON_COST_OPTIMIZATION.md` documents that as a hard constraint on this deployment.

## 3. Source — read before writing

In the backend repository:

| What | Why |
|---|---|
| The notification package — the due-soon job | The tick to extend; note the group-only scope and the hard-coded strings |
| `users` table + entity | Where `timezone` and `locale` go |
| `device_tokens` table | Where `platform` goes |
| Flyway migrations, `h2/` **and** `postgresql/` | Both dialects required |
| The FCM send path | Where localized text is chosen |
| `NEON_COST_OPTIMIZATION.md` | The wakeup constraint |

In this repository:

| Path | Why |
|---|---|
| `data/model/network/request/FCMTokenRequest.kt` | Already carries `deviceId`, `deviceName`, `timeZone` — `platform` is additive |
| `ios-spec/30-PLATFORM/01-notifications-and-alarms.md` | Where `coverageHorizon` comes from |
| `data/source/remote/fcm/PushPayload.kt` | The 10 payload types |

## 4. Target

In the backend repository:

- Migration: `users.timezone`, `users.locale`, `device_tokens.platform`, `device_tokens.reminder_coverage_horizon` — **both** dialects
- `POST /devices/{deviceId}/reminder-coverage`
- The due-soon job extended to personal tasks, localized text, per-user timezone
- Tests

## 5. Steps

1. **Write the migration** for all four columns, in both dialect folders. Default `timezone` to `'UTC'` and `locale` to `'en'` for existing rows; the client backfills them on next login.

2. **Accept `timeZone` and `locale` on token registration.** `FCMTokenRequest` already carries `timeZone`; persist it to the user, and add `locale` and `platform`.

3. **Add the coverage endpoint.** The client POSTs its horizon after every successful rebuild; store it against the device token.

4. **Extend the job to personal tasks.** Currently group-only — this is a bug fix that benefits Android too.

5. **Localize the push text.** Load per-locale strings server-side from the user's stored `locale`. The chat system instruction already lives in backend resources (`chat/system-instruction-{en,tr}.md`); follow that pattern.

6. **Use the user's timezone**, not a fixed offset. This is the third existing bug.

7. **Gate on the coverage horizon.** For an iOS device, push only when the reminder fires **after** the stored horizon. For Android, and for devices with no horizon recorded, keep today's behaviour.

8. **Keep it on the existing tick.** No new `@Scheduled` job.

9. **Deduplicate.** A user with several devices must not get the same reminder several times. Key on `(user, task, reminderTime)`, not on device.

## 6. Code skeleton

```sql
-- V<n>__reminder_push_support.sql — in BOTH h2/ and postgresql/
ALTER TABLE users ADD COLUMN timezone VARCHAR(64) DEFAULT 'UTC';
ALTER TABLE users ADD COLUMN locale VARCHAR(8) DEFAULT 'en';
ALTER TABLE device_tokens ADD COLUMN platform VARCHAR(16);
ALTER TABLE device_tokens ADD COLUMN reminder_coverage_horizon TIMESTAMP NULL;
```

```kotlin
// Rides the EXISTING 5-minute tick — no new scheduled job, no new Neon wakeups.
fun findRemindersNeedingPush(now: Instant): List<ReminderPush> =
    reminderRepository.findUpcoming(now, now.plus(20.minutes))
        .filter { reminder ->
            val device = deviceTokens.forUser(reminder.userId)
            when {
                device.platform != "ios" -> true                        // Android unchanged
                device.reminderCoverageHorizon == null -> true           // unknown → be safe
                // The device already has this one scheduled locally. Pushing would double it.
                reminder.firesAt <= device.reminderCoverageHorizon -> false
                else -> true
            }
        }
        .distinctBy { Triple(it.userId, it.taskId, it.firesAt) }         // multi-device dedup
```

## 7. Acceptance

- [ ] Migration in **both** dialect folders; boots with `ddl-auto=validate`
- [ ] `timezone`, `locale` and `platform` persist on token registration
- [ ] `POST /devices/{deviceId}/reminder-coverage` accepts and stores the horizon
- [ ] The job covers **personal** tasks, not only group tasks
- [ ] Push text is localized — a Turkish user receives Turkish
- [ ] The job uses each user's timezone, not a fixed offset
- [ ] An **active** iOS user (horizon ~21 days out) receives **no** reminder pushes
- [ ] A **lapsed** iOS user receives a push for a reminder beyond their horizon
- [ ] Android behaviour is unchanged
- [ ] Multi-device users receive one push, not several
- [ ] No new scheduled job; no measurable change in Neon wakeups
- [ ] Deployed and verified against a real device

## 8. Pitfalls

- **Do not add a new `@Scheduled` job.** Every additional tick wakes Neon and costs money — a documented constraint on this deployment.
- **Push volume must stay near zero for active users.** Getting the horizon comparison backwards floods every user with duplicates of reminders their phone already has.
- **Deduplicate by `(user, task, time)`, not by device.** Otherwise a user with a phone and a tablet gets everything twice.
- **Migrations in both dialect folders**, or the app fails to boot with `ddl-auto=validate`.
- **H2 upper-cases unquoted identifiers, Postgres lower-cases them.** Write unquoted and qualified; do date arithmetic in Kotlin rather than SQL.
- **Existing rows need defaults.** A null timezone must not break the job for users who have not logged in since the migration.
- **Localized strings belong in backend resources**, following the chat system-instruction pattern — not in client `strings.xml`.
- **Timezone is per user, not per device.** A user who travels changes it on next login; the job should read the current value.

## 9. Verification

```bash
# In the backend repository
./gradlew test
./gradlew bootRun     # ddl-auto=validate proves the migration

# Against the deployed instance
curl -X POST https://donebot-backend.onrender.com/devices/<id>/reminder-coverage \
  -H 'Authorization: Bearer <jwt>' -H 'Content-Type: application/json' \
  -d '{"horizonUtc":"2026-09-01T00:00:00Z"}'

# End to end
#   active iOS device (horizon far out)  → no reminder pushes
#   set the horizon to yesterday          → the next reminder arrives as a push
#   Turkish user                          → Turkish text
#   user in UTC+3                         → correct local time
#   two devices                           → one push, not two
```
