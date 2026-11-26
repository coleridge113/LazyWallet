# Handover — 2026-11-26 (Opus 5.5)

## Where things stand
- **F12** (Budget outlook arc) is merged: PR #12, `6b4c812`. It is in Features-done.
- **F13**, the ExpensePreset screen redesign, is built on `feat/home-redesign`.
  - The branch name is legacy; the screen is called ExpensePreset everywhere else.
  - 15 commits on top of `master`, head `46172ec`. **Not pushed, no PR.** 129 tests and `assembleDebug` pass.
  - Spec: `docs/superpowers/specs/2026-11-26-expensepreset-redesign-design.md`. Plan: `docs/superpowers/plans/2026-11-26-expensepreset-redesign.md`.
- **F13 device testing has started.** The user's feedback led to three fixes, all committed and written into the spec:
  - The snackbar's lifespan no longer resets.
  - The sheet dismisses the snackbar when it opens or closes.
  - Tiles have a ⋮ menu instead of long-press.
- **F14** (Budget heat grid + outlook sheet + day popover) was added as an Idea. It comes from the same design handoff and makes P31 moot.
- **The tracker edits from this handover are uncommitted**: Features, Polish (new P32), Progress and HANDOVER. Learnings is already committed.

## Next step
F13:
1. The user finishes the device check, starting with the new ⋮ button on the tiles.
2. Then present the finish menu: merge locally, push + PR, or keep the branch.
3. If they choose a PR, the body should list the spec's "Done when" device checks and the rulings below.
4. After merge: move F13 to Features-done and commit the tracker.

Then F14: brainstorm from the handoff note in `docs/screens/redesign/` (git-ignored).
Recommended model: Opus for orchestration and brainstorming; Sonnet via `task-runner` for code.

## Waiting on the user
- F13: device check result, then a choice of merge, PR or keep.
- P32 (sheet/snackbar cosmetics): only if they see the issues on device.
- Still open from before: F11, P01, F04/F05/F07 decisions, P06/P22, F09.

## Context the next chat would otherwise rediscover
**F13 rulings made during the build** (the SDD ledger was deleted; they are listed here and in git history):
- Snackbar:
  - It is marked shown only when `showSnackbar` returns normally, so a newer removal's message isn't wiped.
  - `Event.ClearUndo` is sent on `DisposableEffect` dispose, so leaving the screen (or rotating) drops the pending Undo.
  - The snackbar host moves into the sheet while the sheet is shown.
  - The Route owns the timeout: `Indefinite` duration plus its own accessibility-aware timer of about 4 s.
  - Opening or closing the sheet dismisses the snackbar (`snapshotFlow { sheetShown }.drop(1)`).
- Removal:
  - It is ignored while one is in flight and for 500 ms after, to guard against double taps.
  - remove, undo and add are wrapped in try/catch.
  - `lastRemoved` is set only after the delete succeeds.
- The ExpensePreset screen ignores the Expenses category filter (user choice A).
- The new category colours apply app-wide, so the Expenses donut changed too.

**Not verified on a device:**
- TalkBack labels.
- The largest font size (tile text should ellipsize).
- Light-theme legibility of the tints. Tuning them belongs to F04.
- Whether the ⋮ button feels cramped on a 118dp tile.

**User preferences seen this session:**
- Push or PR only when explicitly asked.
- `docs/screens/` is git-ignored and must never be committed.
- The user wants "ExpensePreset" naming, never "Home", in code, spec and plan.
- Execution stays subagent-driven with Sonnet `task-runner`s; Opus usage was high (86% of this session), so keep Opus turns lean.

## Don't
- Don't commit anything under `docs/screens/` (local UI map + redesign mockups).
- Don't name anything "Home" (the design tool's word).
- Don't build on `feat/outlook-arc` or `feat/improved-outlook-card`; branch from a fresh `master`.

## Files touched this session
- `docs/screens/current-screens.html`: local UI map of all screens and overlays, with metadata for AI tools (git-ignored).
- F13 code: `presentation/screen/expensepreset/` (Route, ViewModel, StateEvents, `model/ExpensePresetSummary.kt`, `components/ExpensePresetHero.kt`, `LastLoggedStrip.kt`, `PresetTileGrid.kt`, `RelativeTimeText.kt`, `RecentlyAddedSheet.kt`).
- Other F13 changes:
  - `nav/NavGraph.kt` (`navigateToTab`, `onOpenExpenses`);
  - `ui/theme/Color.kt` (category colours);
  - `res/values/strings.xml`;
  - tests (`ExpensePresetSummaryTest`, `ExpensePresetViewModelTest`, `FakeExpenseRepository`).
- Deleted: `ExpensePresetTable.kt` and `ExpenseAmountDisplay.kt`.
- `tracker/*`: F12 done, F13, F14, P31 note, P32, Progress rows, Learnings (+3: sheet hides snackbar; tab back stack keeps VM state; M3 SnackbarHost owns its timer).

## Start the next chat with
`resume`
