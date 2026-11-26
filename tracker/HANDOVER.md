# Handover — 2026-11-26 (Opus 5.5)

## Where things stand
- Merged to `master` this session: F02 (PR #5), P24/P30 (#6), P25/P27/P29 (#7), P23/P28 (#8), F03 Firebase out of presentation/domain (#9), P26 Auth uiState (#10), F10 Analysis history (#11, `f13afb1`). `master` = `15c0bfa` (F10 tracker + `.gitignore` `docs/learning/*`).
- **F12 Budget outlook arc**: built on `feat/outlook-arc` (6 commits beyond master, head `31de712`), **pushed, no PR yet**. 109 tests green, `assembleDebug` OK. Not device-tested; the arc has never been looked at visually (previews unrendered).
- The screen-by-screen UI rebuild is the current direction. F12 (Budget) is first; F13 (Home) brainstorm was started and paused.
- The tracker updates for this handover are **uncommitted** on `feat/outlook-arc` (Features, Polish, Progress, Learnings, HANDOVER).

## Next step
F12: wait for the user's device check, then open the PR from `feat/outlook-arc` (or apply requested changes on that branch). The PR body should list the spec's "Done when" device checks. After merge: move F12 to Features-done and decide P31.
Then F13: resume the Home-screen brainstorm from its seed notes in Features.md. The open question is "what bothers you about Home / what should it feel like".
Recommended model: Opus for brainstorming, planning and orchestration; Sonnet via task-runner for implementation.

## Waiting on the user
- F12 device check: colours match a BudgetCard at a similar %, overspent = full red + "overspent", no budgets = "No budgets yet", largest font size (amount auto-shrinks), TalkBack reads one sentence, light and dark.
- P31: pull "₱X of ₱Y" up into the arc gap? Decide after seeing it.
- F13: answer to the opening brainstorm question.
- Still open from before: F11 (snackbar vs dialog), P01 (backup rules), F04/F05/F07 decisions, P06/P22 (build tools, "later"), F09 (guest upgrade, ready to brainstorm).

## Context the next chat would otherwise rediscover
- F12 rulings (in the spec and the PR-to-be):
  - Task 2 has no unit tests (all logic is in `OutlookDetails`).
  - "₱X of ₱Y" sits below the gauge (→ P31).
  - Exactly-at-target reads "left" in both BudgetOutlookCard and BudgetCard (`remaining >= 0`).
  - With no budgets, the month's spend is hidden.
  - There is no pace phrase in TalkBack.
- `feat/improved-outlook-card` (the user's earlier two-arc attempt, 62 commits behind) is kept as reference. Do not build on it or delete it.
- `feat/analysis-history` is merged; its stray `.gitignore` commit was cherry-picked to master. The user may delete it locally (`git branch -D`).
- Teaching write-up of F10: `docs/learning/2026-11-26-F10-analysis-history-walkthrough.md` (git-ignored on purpose, per the user).
- SDD routine that worked:
  - The controller commits each task-runner result (`git add -A app/`), then `review-package` and a sonnet task reviewer per task.
  - Opus does the final whole-branch review, then one task-runner fix wave and a sonnet re-review.
  - Reviewer and re-review instructions are saved as files in the plan workspace, and dispatches point at them.
- User preferences seen this session:
  - They want push/PR only on explicit ask.
  - They're happy with direct-to-master for tracker-only fixes (chose option C once).
  - They like concise option lists with a recommendation.

## Don't
- Don't push or open PRs without an explicit ask; the user said "push first" for F12, i.e. no PR yet.
- Don't build new work on merged or stale branches; branch fresh from `master`.
- Don't use `advanceUntilIdle()` while `todayFlow` is collected (see Learnings).

## Files touched this session (latest work)
- `docs/superpowers/specs/2026-11-26-budget-outlook-arc-design.md`, `docs/superpowers/plans/2026-11-26-budget-outlook-arc.md`: F12 spec and plan.
- `presentation/screen/budget/model/OutlookDetails.kt`: `usedFraction`, `paceFraction(today)`.
- `presentation/screen/budget/BudgetViewModel.kt`: the outlook combines `_today`.
- `presentation/screen/budget/components/BudgetCard.kt`: shared `budgetProgressColor()`, `remaining >= 0`.
- `presentation/screen/budget/components/BudgetOutlookCard.kt`: gauge arc rewrite.
- `presentation/screen/budget/BudgetRoute.kt`: card `padding(4.dp)`.
- `res/values/strings.xml`: `outlook_spent_of`, `outlook_no_budgets`, `outlook_cd`; old `outlook_*` removed.
- Tests: `OutlookDetailsTest.kt` (new), `BudgetViewModelTest.kt` (pace rollover).
- `tracker/*`: F12 Building, F13 + seed notes, P31, Learnings (+4), Progress rows.

## Start the next chat with
`resume`
