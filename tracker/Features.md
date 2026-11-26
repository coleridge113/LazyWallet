# Features

Open features, one row each. Design lives in the linked spec, the build steps in the linked plan. Status: Idea · Designing · Planned · Building · Blocked · Later.

| ID | Feature | Status | Spec | Plan | Next step |
|---|---|---|---|---|---|
| F01 | Figma design file | Blocked | – | – | Figma MCP rate limit; resume T02 when it resets |
| F04 | Theme, contrast & light theme | Idea | – | – | Your call: does the app need a tuned light theme? |
| F05 | Screen chrome: top bars, nav labels, undo | Idea | – | – | Your call: P17 labels/Analysis tab, P18 snackbar undo; then brainstorm |
| F06 | Test coverage | Idea | – | – | Brainstorm when wanted |
| F07 | Adaptive layouts | Later | – | – | Only if tablets/landscape matter |
| F09 | Guest to account upgrade (Cloud Sync page) | Idea | – | – | Brainstorm spec (direction agreed 2026-11-25) |
| F11 | Write-failure feedback | Idea | – | – | Your call: snackbar vs dialog for failed saves/deletes (ties to P18/F05); then brainstorm |
| F12 | Budget outlook arc (UI rebuild: Budget screen) | Building | [design](../docs/superpowers/specs/2026-11-26-budget-outlook-arc-design.md) | [plan](../docs/superpowers/plans/2026-11-26-budget-outlook-arc.md) | Pushed `feat/outlook-arc` 2026-11-26 (no PR yet). Your call: device check, then open PR or request changes |
| F13 | UI rebuild: Home (ExpensePreset) screen | Idea | – | – | Brainstorm (started 2026-11-26, paused for F12); first question: what bothers you about Home / what should it feel like |

New features get the next F ID. Seed notes are folded into the spec by brainstorming, then deleted here. Done rows move to Features-done.md.

### F01 seed notes (from Plan.md T02-T05 and Spec §5.1)

(Moved verbatim on 2026-11-25: "Spec §5.1" is now Spec §4; Plan.md was retired, T02–T05 are the rows below.)

| ID | Source text |
|---|---|
| T02 | Verify the unchecked Figma screens (Auth sign in, Cloud sync, Expenses search/date menu, Budget create/delete dialogs) with screenshots; fix issues found **Status was:** Blocked. **Depends on:** –. **Scope:** Figma file only (see Spec §5.1); no app code. **Done when:** Each screen screenshotted and matches the Compose source. **Needs OK:** No |
| T03 | Fix known Figma gaps: Budget Details category icons (misshapen at 12px), BudgetCard missing category chips and bottom space **Status was:** Blocked. **Depends on:** T02. **Scope:** Figma file only. **Done when:** Screenshots show icons and chips correct. **Needs OK:** No |
| T04 | Add missing Figma screens: month/year picker, date-range picker, loading/error states, real `lazy_wallet_logo` **Status was:** Later. **Depends on:** T02. **Scope:** Figma file only. **Done when:** Screens exist, built from components. **Needs OK:** No |
| T05 | Your call: build a Light theme in Figma (Starter plan = 1 mode per collection; use the separate "Theme Light" collection) **Status was:** Your call. **Depends on:** T02. **Scope:** Figma file only. **Done when:** User decides whether it's wanted and how to swap collections. **Needs OK:** No |
| Spec §5.1 known issues | Known issues: Budget Details category icons misshapen; BudgetCard lacks category chips; dialog background color (surfaceVariant) and some layouts (category filter, budget create, auth, migration) are guesses; no Light screens; no month/year or date-range picker, loading/error states; logo is a placeholder; Analysis screens use the "List" nav tab because the app hides Analysis from the bar. |
| Spec §5.1 verified | Verified by screenshot: Home (presets, add dialog), Expenses list, Category filter, Analysis x2, Budget list, Budget details. NOT verified: Auth, Migration, Expenses search/date menu, Budget create/delete dialogs. |
| Spec §5.1 tracking | Tracking: T02-T05 in Plan.md. T02 and T03 are Blocked: the Figma Starter plan's MCP call limit was hit on 2026-11-25. Resume when it resets or the plan is upgraded. |


### F04 seed notes (from P14; see also T05 in F01)

| ID | Source text |
|---|---|
| P14 | Contrast and color roles: `primaryContainer` = `primary` (GruvboxOrange); `onSurfaceVariant` #928374 on `surfaceVariant` #3c3836 borderline for secondary text; text on `primary.copy(alpha=0.1f/0.6f)` containers (`BudgetCard`, `ExpensePresetItem`); search placeholder `onSecondary` @0.6; light scheme `onSurfaceVariant` likely <4.5:1; unused template colors (`Purple80`, `Pink40`…); redundant SDK check for dynamic color (minSdk 33) **Area:** `ui/theme/Theme.kt`, `Color.kt`, alpha-container sites. **Status was:** Open. **Found:** 2026-11-25, UI audit. **Notes:** Needs Opus plan, tied to T05 (Light theme). Done when all text pairs ≥4.5:1 and containers use real roles |

### F05 seed notes (from P16, P17, P18)

| ID | Source text |
|---|---|
| P16 | Top bars and padding: each screen adds an empty `TopAppBar(title = {}, windowInsets = WindowInsets(0))`; hard-coded `padding(bottom = 72.dp)` / `80.dp` (ExpensePreset, ExpenseList, Budget) assume nav bar height; preset FAB menu trigger uses `Icons.Default.Menu` (reads as nav drawer) **Area:** all Routes, `nav/NavGraph.kt`. **Status was:** Open. **Found:** 2026-11-25, UI audit. **Notes:** Needs Opus plan + user sign-off on layout. Real top app bars (title + actions like Logout, date range) with `scrollBehavior`; use Scaffold padding |
| P17 | Bottom bar has no labels; descriptions use `item.name` ("HOME"); Analysis not in the bar (transition treats it as index -1) **Area:** `components/BottomNavBar.kt`, `nav/NavGraph.kt`. **Status was:** Your call. **Found:** 2026-11-25, UI audit. **Notes:** Decide: add labels (M3 default) and whether Analysis becomes a tab |
| P18 | Undo is a FAB; legend-tap-to-reset is undiscoverable **Area:** `ExpensePresetRoute.kt` + VM, `NavGraph.kt` (SnackbarHost). **Status was:** Your call. **Found:** 2026-11-25, UI audit. **Notes:** Proposal: snackbar "Expense deleted · Undo" after add/delete instead of the Undo FAB. Needs Opus plan if accepted |

### F06 seed notes (from P10)

| ID | Source text |
|---|---|
| P10 | Test coverage gaps: no tests for Analysis, Budget, BudgetDetails, Auth, Migration VMs, use cases, DAOs or Room migrations; leftover template `HelloWorldUnitTest` / `ExampleInstrumentedTest` **Area:** `app/src/test`, `app/src/androidTest`. **Status was:** Open. **Found:** 2026-11-25, survey. **Notes:** Do after P08/P09; split per screen when worked on |

### F07 seed notes (from P20)

| ID | Source text |
|---|---|
| P20 | Not adaptive: fixed `fillMaxWidth(0.6f)` + `offset(16.dp)` chart, `GridCells.Fixed(2)` presets, no rail/window size classes **Area:** ExpenseList/ExpensePreset, `NavGraph.kt`. **Status was:** Your call. **Found:** 2026-11-25, UI audit. **Notes:** Only if tablets/landscape matter. `GridCells.Adaptive`, `NavigationSuiteScaffold` |

### F09 seed notes (brainstorm 2026-11-25)

- Intent (user): a guest is offline-only, never touches Firebase. To get sync they sign in; that's when the Cloud Sync (migration) page is used: it moves their guest data into the account. Doesn't work this way yet.
- Today (code): guest sign-in doesn't touch Firebase (good). No "connect account" path; a guest must sign out (data kept) then sign in. **Bug:** after sign-in `DataMigrationRepository.syncFromCloud()` deletes every local table and replaces it with cloud data, so the guest's data is silently lost; `performMigration()` (upload) is never called. `MigrationRoute` isn't registered in `NavGraph`.
- Decided (user, option B): on sign-in after guest use, show the Cloud Sync page. Account empty → confirm "Move your data to your account". Account has data → choose **Merge** (default, recommended) or **Keep only my account's data** (warns this phone's data will be deleted).
- Open for the spec: what counts as a duplicate when merging (presets/category profiles by name?), show only to former guests, offline/failure handling, sign-in entry point for a guest (without signing out first?).


### F11 seed notes (MVVM research 2026-11-26)

- Failures in writes are swallowed: ExpensePreset `saveExpensePreset`/`deleteExpensePreset` only log; `addExpense`, `deleteExpense`, `editExpense`, `saveBudget` (ExpenseList/Budget/BudgetDetails) have no try/catch. Dialogs close in `finally` even on failure; `isSaving` in `DialogState.ExpenseForm` and the other dialog states is never set true.
- Auth already uses `DialogState.ErrorMessage`, so two error mechanisms exist. Google's guidance: transient errors as a `userMessage` in state that the UI shows (usually a snackbar) and acknowledges (https://developer.android.com/topic/architecture/ui-layer/events).
- Open for the spec: snackbar vs dialog, keep form open on failure with `isSaving`, shared shape across screens, strings via P23 pattern.
- Sign-out failure (F02 final review 2026-11-26): `signOutUser` now catches and logs, but the confirm dialog stays open with no feedback; `SignOutUseCase` calls `auth.signOut()` before `clearAllTables()`, so a failed clear leaves the user signed out of Firebase with local data still shown. Include in the same write-failure pattern.

### F13 seed notes (brainstorm started 2026-11-26)

- Screen-by-screen UI rebuild (user, 2026-11-26); F12 (Budget outlook arc) went first, Home is next.
- Home today (top→bottom): empty TopAppBar with only a sign-out icon; today's total in `displayMedium` ("-" when 0, ~¼ of the screen, `ExpenseAmountDisplay`); 2-column `ExpensePresetTable` where each preset is a round category icon (tap = custom-amount form) + a pill (tap = log instantly), swipe for edit/delete; a FAB with a ☰ icon expanding to Add preset + Undo (delete latest expense, confirmed).
- Overlaps it would absorb: P14 (translucent preset containers contrast), P16 (empty top bar, hard-coded `padding(bottom = 72.dp)`, ☰ reads as nav drawer), P18 (undo FAB → snackbar "Expense added · Undo"), F04/F05 chrome questions. Figma file (F01) mirrors the current Home.
- Open question asked, not yet answered: what bothers you about Home today / what should it do or feel like after the rebuild (visual refresh vs rethink of logging).
