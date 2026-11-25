# Spec

## 1. Current state (survey, 2026-11-25)

**What it is.** Lazy Wallet (`com.luna.budgetapp`): Kotlin 2.3 + Jetpack Compose (BOM 2026.04), AGP 9.1, minSdk 33, JDK 21. Log expenses in one tap via reusable *Expense Presets*; filter by date range and category; analysis charts; budgets with outlook cards. Currency ₱, amounts stored as `Long` cents.

**Layout.** ~172 main Kotlin files, clean architecture (see `../../CLAUDE.md` for the full description):
- `domain/`: models (Expense, ExpensePreset, Budget, CategoryFilter, DateFilter, 11-value `Category` enum), repository interfaces, one use case per class grouped in `*UseCases` aggregates.
- `data/`: Room v5 (entities: Expense, ExpensePreset, Budget + BudgetInteractorCategory, CategoryFilter; migrations 1→5), DataStore (auth, settings), Firestore mirror + mappers, Retrofit/Pusher (legacy, backend disconnected).
- `presentation/screen/`: analysis, auth, budget, budgetdetails, expenselist, expensepreset, migration, plus shared `components/`.
- DI: single Koin file `di/appModule.kt`.

**Data flow.** Room is source of truth; writes mirror best-effort to Firestore `users/{uid}/...` keyed by `remoteId` when signed in. Guest mode exists; sign-out clears the DB unless guest. One-time `DataMigrationRepository` uploads pre-auth local data.

**Git.** Only `android/` is a repo. Default branch `master`; origin on GitHub. Current branch `feat/expense-search` (HEAD: expandable search FAB; before it: Room-level search by type/name/category, debounced in ViewModel, affecting list only, not chart/total). Many other feature/fix branches exist locally (budget-outlook, guest-mode, improved-outlook-card, etc.); `playstore-build` is the release branch.

**Tests.** Thin: `ExpenseListViewModelTest`, `ExpensePresetViewModelTest`, four hand-written `Fake*Repository`s, `MainDispatcherRule`, plus leftover `HelloWorldUnitTest` / `ExampleInstrumentedTest`. No tests for Analysis, Budget, BudgetDetails, Auth, use cases, DAOs, migrations or the search feature. `FakeExpenseRepository` and `FakeExpensePresetRepository` contain `TODO("Not yet implemented")` stubs.

**Known rough edges / open questions.**
- README still describes the app as fully offline and shows old screenshots.
- Some repository methods still call the Retrofit `ExpenseService`; they fail silently while the backend is down. Unclear whether the backend is to be revived or removed.
- Firestore errors are swallowed (no user-visible sync status or retry).
- `exportSchema = false`: no Room schema JSON to test migrations against.
- Release builds need `keystore.properties` (present, gitignored) and `google-services.json` (gitignored).
- No lint/format tooling configured.
- `data_extraction_rules.xml` has a template TODO (P01).

## 2. Goal
Keep Lazy Wallet a fast, low-friction personal budgeting app on Android with reliable local data and optional Firebase sync. (Confirm with user.)

## 3. Scope / Out of scope
To be defined when tasks are planned.

## 4. Deliverables
To be defined.

## 5. Details

### §5.1 Figma design file (added 2026-11-25)
- File: https://www.figma.com/design/xvyK4Njkkj2vaSNJ5VHZOV ("Lazy Wallet", team "Jose Luna's team", Starter plan). Figma MCP plugin is installed and authenticated.
- Purpose: have every UI screen in Figma so the design can be iterated there. Built from the Compose code (not screenshots), as editable text/components/icons; dark theme only.
- Variables: collections "Theme Dark" and "Theme Light" (Gruvbox Material colors from `ui/theme/Color.kt` + chart colors). Screens use Dark.
- Page "Components": icon/* (food, beverage, commute, grocery, bills, house, fitness, date, personal, leisure, others), NavBar (variants Selected=Home/Budget/List), Button (Primary/Secondary, Label prop), TextField/Outlined, PresetItem, ExpenseItem, BudgetCard (text props; icon via instance swap).
- Page "Screens" (412x915): Home / Presets, FAB open, Add preset dialog, Sign out dialog; Expenses / List, Search, Date menu, Category filter dialog; Analysis / Overview, Bar selected; Budget / List, Create dialog, Delete dialog, Details; Auth / Sign in; Migration / Cloud sync.
- Verified by screenshot: Home (presets, add dialog), Expenses list, Category filter, Analysis x2, Budget list, Budget details. NOT verified: Auth, Migration, Expenses search/date menu, Budget create/delete dialogs.
- Known issues: Budget Details category icons misshapen; BudgetCard lacks category chips; dialog background color (surfaceVariant) and some layouts (category filter, budget create, auth, migration) are guesses; no Light screens; no month/year or date-range picker, loading/error states; logo is a placeholder; Analysis screens use the "List" nav tab because the app hides Analysis from the bar.
- Tracking: T02-T05 in Plan.md.

## 6. Constraints and approver
Solo project; Jose Luna approves. Never touch `backup/`, `keystore*`, `local.properties`, `google-services.json` contents.

## 7. Accepted gaps
| Gap | Why | Where |
|---|---|---|
| Two modes per variable collection (Light/Dark switching) in Figma | Starter plan limit | Figma file, T05 |
| Backend sync via Retrofit/Pusher | Backend (`../backend`) is disconnected | Legacy repo methods |
