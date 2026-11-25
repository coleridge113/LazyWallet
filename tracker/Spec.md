# Spec

## 1. What it is and current state (as of 2026-11-25)

Lazy Wallet (`com.luna.budgetapp`): Kotlin 2.3 + Jetpack Compose (BOM 2026.04), AGP 9.1, minSdk 33, JDK 21. Log expenses in one tap via reusable *Expense Presets*; filter by date range and category; analysis charts; budgets with outlook cards. Currency ₱, amounts stored as `Long` cents. Room is the source of truth with a best-effort Firestore mirror; guest mode exists.

Only `android/` is a git repo (default branch `master`, origin on GitHub; `playstore-build` is the release branch). Current branch `polish/presentation-fixes`; `feat/expense-search` was merged via PR #2. Unit tests: 27 passing (ExpenseList, ExpensePreset and Budget ViewModels, use cases, Utils); no tests yet for Analysis, BudgetDetails, Auth, Migration, DAOs or Room migrations (see F06).

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

## 3. Architecture
See `../../CLAUDE.md` (layers, data flow, Room, DI, tests).

## 4. Project-wide facts

### Figma design file (added 2026-11-25)
- File: https://www.figma.com/design/xvyK4Njkkj2vaSNJ5VHZOV ("Lazy Wallet", team "Jose Luna's team", Starter plan). Figma MCP plugin is installed and authenticated.
- Purpose: have every UI screen in Figma so the design can be iterated there. Built from the Compose code (not screenshots), as editable text/components/icons; dark theme only.
- Variables: collections "Theme Dark" and "Theme Light" (Gruvbox Material colors from `ui/theme/Color.kt` + chart colors). Screens use Dark.
- Page "Components": icon/* (food, beverage, commute, grocery, bills, house, fitness, date, personal, leisure, others), NavBar (variants Selected=Home/Budget/List), Button (Primary/Secondary, Label prop), TextField/Outlined, PresetItem, ExpenseItem, BudgetCard (text props; icon via instance swap).
- Page "Screens" (412x915): Home / Presets, FAB open, Add preset dialog, Sign out dialog; Expenses / List, Search, Date menu, Category filter dialog; Analysis / Overview, Bar selected; Budget / List, Create dialog, Delete dialog, Details; Auth / Sign in; Migration / Cloud sync.
- Known issues and the verification backlog live in F01's seed notes (Features.md).

## 5. Constraints and approver
Solo project; Jose Luna approves. Never touch `backup/`, `keystore*`, `local.properties`, `google-services.json` contents.

## 6. Accepted gaps
| Gap | Why | Where |
|---|---|---|
| Two modes per variable collection (Light/Dark switching) in Figma | Starter plan limit | Figma file, F01 |
| Backend sync via Retrofit/Pusher | Backend (`../backend`) is disconnected | Legacy repo methods |

## 7. Feature specs
Per-feature specs live in `docs/superpowers/specs/`; see Features.md.
