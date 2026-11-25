# Handover — 2026-11-25 23:01 (Opus)

## Where things stand
- PR #3 (polish P04–P13, paging bump, feature-based tracker F08) and PR #4 (P15 budget card, P19 shape tokens, P21 strings) are merged into `master`.
- Branch `feat/navigation-events` (from master after PR #4) holds the approved **F02** spec and plan, committed as `ebe30c3`. No code is written yet.
- F09 (guest to account upgrade) and F10 (Analysis history and timelines) are shaped as Ideas with detailed seed notes in `tracker/Features.md`.
- This handover note and two new Learnings lines are uncommitted.

## Next step
F02: run `docs/superpowers/plans/2026-11-25-navigation-events.md` with `superpowers:subagent-driven-development` on `feat/navigation-events`, working in place with no worktree. Dispatch each task to `subagent_type: task-runner, model: sonnet`. Override the brief's commit step with "leave changes uncommitted; list files changed". The controller commits after each task review. There are 5 tasks, and the expected test counts go 27 → 29 → 31 → 34 → 34.
Recommended model: Opus orchestrates in the main session; implementation runs on Sonnet via task-runner.

## Waiting on the user
- F05: bottom-bar labels, an Analysis tab (P17), and a snackbar undo in place of the Undo FAB (P18).
- F04: does the app need a tuned light theme?
- F07: do tablets and landscape matter?
- P01: should Android's automatic backup include app data?
- P23: which pattern ViewModels should use for strings (`@StringRes` ids suggested). It needs a small Opus plan.
- P22 (AGP 9.2 + compileSdk 37 so navigation and lifecycle can be bumped): the user said "later".

## Context the next chat would otherwise rediscover
- In F02, result navigation is deliberately not wrapped in `dropUnlessResumed`, because it would drop the navigation instead of deferring it. Only the click lambdas get the `ifResumed` guard. See the spec's "Builder's calls" section.
- F02 removes dead navigation on purpose: the Analysis/Expenses events, the bar-graph event and the home-total tap. F10 adds Analysis entry back later. The user confirmed those paths were disabled intentionally.
- Migration screen: F02 gives it an `isDone` flag and an `onMigrationDone` parameter but does not register it in `NavGraph`. F09 does that.
- Data-loss bug, F09: `DataMigrationRepository.syncFromCloud()` wipes every local table on each sign-in, and on every launch for signed-in users. A guest who signs in loses their data. `performMigration()` is never called.
- The UI-research audit's Analysis/BudgetDetails/Migration/dialog screens were never audited. A narrower `@ui-researcher` run would cover them.

## Don't
- Don't rewrite Material3 `Typography` roles as plain `TextStyle`s (see Learnings).
- Don't bump navigation-compose to 2.10.x before P22: it pulls lifecycle 2.11, which needs compileSdk 37.
- Don't create worktrees in this project.

## Files touched this session
- `tracker/*`: migrated to the feature layout. F02 is Planned; F09 and F10 were added; P15, P19 and P21 are archived; P22 and P23 were added.
- `docs/superpowers/specs/2026-11-25-{feature-based-tracker,navigation-events}-design.md` and the matching `docs/superpowers/plans/`.
- Global (outside git):
  - `~/.claude-personal/agents/{task-runner,ui-researcher,android-researcher}.md`;
  - `~/.claude-personal/hooks/task-runner-guard.sh`;
  - `~/.claude-personal/scripts/check_tracker.py` (with tests);
  - the `handover` and `project-tracker` skills;
  - `~/.claude-personal/research/{android,ui}/`.
- `../CLAUDE.md`: added the Project tracking section and the worktree rule.
- `.gitignore`: now ignores `.superpowers/`.

## Start the next chat with
`resume` (it picks up F02). Or say `run the F02 plan` directly.
