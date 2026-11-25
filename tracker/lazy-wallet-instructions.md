# Lazy Wallet: project instructions

**Model rule:** tasks and Sonnet-ready Polish tickets run through the global `task-runner` agent (Sonnet, no sub-agents, hook-blocked from commits/pushes/branch switches): `@task-runner do P10`. Planning and reviews run on Opus in the main session. If the session is on the wrong model, say so first.

## Source of truth
`android/tracker/` (Markdown): Plan.md (tasks), Polish.md (cosmetic tickets, only when asked), Progress.md (log, newest first), Learnings.md (override these instructions), Spec.md (reference + accepted gaps). Architecture and commands: `../CLAUDE.md`.

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

## Working in sessions
Follow the 7-step routine in Plan.md. Imperfections get sorted: broken/blocking → new Ready task; cosmetic → Polish ticket; platform can't do it → Accepted gaps in Spec; unclear → Your call task.

## Checks
- `./gradlew testDebugUnitTest` (or `--tests` for one class) from `android/`
- `./gradlew assembleDebug` compiles
- Use real use cases over `Fake*Repository` in ViewModel tests; extend fakes when adding repository methods

## Reporting
Plain language: what's done, what's saved but not live, what's next, and the next decision for the user.
