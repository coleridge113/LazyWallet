# Lazy Wallet: project instructions

**Model rule:** Planning, design and orchestration run on Opus in the main session. Implementation runs through the `task-runner` agent (Sonnet): dispatched by Superpowers subagent-driven-development for feature plan tasks, or directly (`@task-runner do Pxx`) for Polish tickets. Sub-agents are allowed when orchestrated this way. If the session is on the wrong model for its job, say so first.

## Source of truth
`android/tracker/` (Markdown), read and updated with Read/Edit/Grep. Architecture and commands: `../CLAUDE.md`.
- Features.md: open features, status and next step (Features-done.md for finished ones)
- Polish.md: small fixes, only when asked (Polish-done.md for Fixed / Won't fix)
- Progress.md: session log, newest first (Progress-archive.md past 50 rows)
- Learnings.md: gotchas not yet in these instructions; they override them
- Spec.md: overarching facts and Accepted gaps
- docs/superpowers/specs and plans: one spec and one plan per feature
- Superpowers worktree preference: work in place on a feature branch; never create worktrees (gitignored build files such as `google-services.json` and `local.properties`)

## Tools
Read/Edit/Grep, then Bash for Gradle. Don't load other connectors or plugins.

## Facts
| Item | Value |
|---|---|
| Package | `com.luna.budgetapp` |
| Repo | `android/` only; default branch `master`; work on a feature branch |
| Money | `Long` cents, convert only at the UI boundary |
| DB | Room v5, `exportSchema=false`; new schema = hand-written Migration + version bump + register in `databaseModule` |
| DI | everything in `di/appModule.kt` |
If something doesn't match what you see, trust what you see and tell the user.

## Boundaries
Never: edit `backup/`, `keystore*`, `local.properties`, `google-services.json`; run `firestore/*.js` scripts against the live project.
Ask first: commits/pushes/merges, Firestore or schema changes, deleting branches, anything affecting release (`playstore-build`).

## Workflows

**Feature (Opus main session):** "plan F05".
1. `superpowers:brainstorming` writes the spec; fold the seed notes into it and delete them from Features.md. Status Designing, link Spec.
2. `superpowers:writing-plans` writes the plan. Status Planned, link Plan.
3. `superpowers:subagent-driven-development`, working in place on a feature branch (no worktree). The controller dispatches each implementer with `subagent_type: task-runner, model: sonnet` (not `general-purpose`), overriding the brief's "commit your work" step with "leave changes uncommitted; list the files you changed". A reviewer checks each task and a final review checks the branch. Status Building. The **controller commits** each task, staging only the files the implementer listed (never `git add -A`), and updates Features/Progress only at milestones.
4. `superpowers:finishing-a-development-branch`; the user picks merge or PR. Status Done: move the row to Features-done.md (a merge or PR link not yet known is filled in by handover's resume).

**Small fix:** `@task-runner do P13`. One Polish ticket, no spec or plan. It updates Polish, Progress and Learnings and doesn't commit.

**Research:** researcher agents report; the main session triages with the user's OK: small items become Polish tickets, larger ones a new feature row (Idea, with seed notes) or are appended to an existing feature's seed notes.

**Session rules:**
1. Read Features.md (or Polish.md for a ticket), the newest 3 rows of Progress, and all of Learnings. Learnings override the instructions where they conflict.
2. Take the item the user named. Set it In progress before starting.
3. Do only that item, inside its scope. Where it needs the user's OK, stop before that step and ask.
4. Check the result against its done-when and the project's checks.
5. Progress: one row at the top per milestone (spec approved, plan approved, build finished) or per Polish ticket, not per plan task. Keep 50 rows; move older ones to Progress-archive.md.
6. Move finished work out: Done feature to Features-done.md; Fixed or Won't fix ticket to Polish-done.md (Won't fix also gets an Accepted-gaps row in Spec.md).
7. Add any new gotcha to Learnings, then stop and say what was done and what is next.

Imperfections get sorted, not just mentioned: broken or blocking becomes a feature or Polish ticket; cosmetic but fixable becomes a Polish ticket (only when the user asks); something the platform can't do becomes an Accepted gap (never retried); unclear becomes "Your call" in a Next step.

## Checks
- `./gradlew testDebugUnitTest` (or `--tests` for one class) from `android/`
- `./gradlew assembleDebug` compiles
- Use real use cases over `Fake*Repository` in ViewModel tests; extend fakes when adding repository methods
- `python3 ~/.claude-personal/scripts/check_tracker.py android` (from the budget folder) prints OK

## Reporting
Plain language: what's done, what's saved but not live, what's next, and the next decision for the user.
