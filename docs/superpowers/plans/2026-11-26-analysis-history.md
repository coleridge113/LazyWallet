# F10 Analysis history and timelines — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** A scrollable Day / Week / Month spending history on the Analysis screen, opened from a chart icon on Expenses.

**Architecture:** One Room query returns daily totals (local date) for the active categories across all history; a pure Kotlin rollup turns them into zero-filled day, week (Mon–Sun) or month periods; `AnalysisViewModel` exposes periods, the selection, the change vs the previous period and the selected period's expenses; the chart is a `LazyRow(reverseLayout = true)` of bar composables with a fit-to-view scale and a dashed average line.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room, Koin, Navigation Compose (type-safe routes); tests with JUnit4, Truth, coroutines-test, `MainDispatcherRule`, `MutableClock`, hand-written fakes.

**Spec:** `docs/superpowers/specs/2026-11-26-analysis-history-design.md` (approved 2026-11-26). Its "Decisions", "Design", "Clarifications" and "Behaviour to keep" sections are binding.

Paths are relative to `app/src/main/java/com/luna/budgetapp/` (main) and `app/src/test/java/com/luna/budgetapp/` (test) unless they start with `app/` or `docs/`.

## Global Constraints

- Defaults: opens on **Day** with the **current period** selected; about **7** bars in view (Day), **5–6** (Week), **6** (Month); weeks **Monday–Sunday**; empty periods are **zero bars**; y-scale fits the **bars in view**; the active category profile filters everything.
- Change = selected period total minus the **whole** previous period's total; percentage is null when there is no previous period or its total is 0.
- Empty active category list → every bar zero (query `IN ()` matches nothing). Never fall back to "all categories".
- No new dependencies. No Room schema change (a new `@Query` only; DB version stays 5).
- All user-facing text in `app/src/main/res/values/strings.xml` (P23 rule); amounts are `Long` cents, formatted only in the UI with the existing `toCurrency()` / `toPeso()` helpers.
- Every `catch (e: Exception)` rethrows `CancellationException` first; keep the P25 retry pipeline (`_retryTrigger.flatMapLatest { … .catch { emit(UiState.Error(...)) } }`).
- Navigation lambdas are hoisted to `NavGraph.kt` and guarded with `backStackEntry.ifResumed { … }` (F02 pattern).
- "Today" comes from the injected `java.time.Clock` via `domain/utils/todayFlow(clock)` (already exists; it re-emits at midnight). In tests use `MutableClock` and `runCurrent()`, never `advanceUntilIdle()` while `todayFlow` is collected (it loops day by day forever).
- Tests: hand-written fakes, no MockK for repositories. Suite is 70 tests at start; it only goes up.
- Commands (from `android/`): `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`. Implementer never commits; the controller does.

## Review Focus

1. **Late-evening expense on the wrong day.** The SQL groups with `strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime')`; Task 1 tests the projection→domain mapping and the fake's grouping by `LocalDate`; the 23:30 case itself stays a device check (no Room instrumented tests exist).
2. **Week boundary.** An expense on Sunday 2026-11-22 belongs to the week starting Mon 2026-11-16; one on Mon 2026-11-23 to the next. Task 1 test `week_mondayToSunday`.
3. **Year boundary.** Month rollup across Dec 2025 → Jan 2026 and a week spanning 2025-12-29 … 2026-01-04 (one period). Task 1 tests `month_acrossYearBoundary`, `week_spanningNewYear`.
4. **Empty category profile.** No active categories → periods exist and are all zero (not all-categories). Task 1 use-case test `emptyCategories_allZero`; Task 2 VM test `emptyProfile_showsZeroBars`.
5. **Restore with a stale selection.** `selected_start` saved as Wed 2026-11-18 with `granularity = WEEK` restores as Mon 2026-11-16. Task 3 test `restore_normalisesSelectionToPeriodStart`.

---

### Task 1: Daily totals query, rollup and history use case

**Files:**
- Modify: `data/local/dao/ExpenseDao.kt` (new query), create `data/local/projection/DailyTotalProjection.kt` (or next to `CategoryTotalProjection`, wherever that lives — match it)
- Modify: `domain/repository/ExpenseRepository.kt`, `data/local/repository/ExpenseRepositoryImpl.kt`
- Create: `domain/model/SpendingHistory.kt` (`Granularity`, `DailyTotal`, `PeriodTotal`, `PeriodChange`)
- Create: `domain/utils/SpendingRollup.kt` (pure functions)
- Create: `domain/usecase/expense/ObserveSpendingHistoryUseCase.kt`
- Modify: `domain/usecase/ExpenseUseCases.kt` (append `observeSpendingHistory`), `di/appModule.kt` (`factoryOf(::ObserveSpendingHistoryUseCase)`)
- Modify (test): `domain/repository/FakeExpenseRepository.kt`; every test that builds `ExpenseUseCases` by name (add the new member)
- Create (test): `domain/utils/SpendingRollupTest.kt`, `domain/usecase/ObserveSpendingHistoryUseCaseTest.kt`

**Interfaces:**
- Produces:
  - `enum class Granularity { DAY, WEEK, MONTH }`
  - `data class DailyTotal(val date: LocalDate, val total: Long)`
  - `data class PeriodTotal(val start: LocalDate, val end: LocalDate, val total: Long)` — `end` inclusive.
  - `data class PeriodChange(val amount: Long, val percent: Double?)`
  - `fun LocalDate.periodStart(granularity: Granularity): LocalDate` — DAY: itself; WEEK: previous-or-same Monday; MONTH: first of month.
  - `fun periodEnd(start: LocalDate, granularity: Granularity): LocalDate`
  - `fun rollUp(daily: List<DailyTotal>, granularity: Granularity, today: LocalDate): List<PeriodTotal>` — oldest first; every period from the one containing the earliest `daily` date (or `today` if `daily` is empty) through the one containing `today`; totals summed; missing periods = 0. Input may be unsorted.
  - `fun change(selected: PeriodTotal, previous: PeriodTotal?): PeriodChange` — `amount = selected.total - (previous?.total ?: 0)`; `percent = amount * 100.0 / previous.total` when `previous != null && previous.total != 0L`, else null. When `previous == null`, `amount = selected.total`.
  - DAO: `fun getDailyTotals(categories: List<String>): Flow<List<DailyTotalProjection>>` with the spec's SQL verbatim; `DailyTotalProjection(day: String, total: Long)`.
  - `ExpenseRepository.getDailyTotals(categories: List<String>): Flow<List<DailyTotal>>` (impl parses `day` with `LocalDate.parse`).
  - `data class SpendingHistory(val today: LocalDate, val periods: List<PeriodTotal>)` (in `SpendingHistory.kt`).
  - `class ObserveSpendingHistoryUseCase(repository: ExpenseRepository, clock: Clock) { operator fun invoke(categories: List<String>, granularity: Granularity): Flow<SpendingHistory> }` — combines `repository.getDailyTotals(categories)` with `todayFlow(clock)` and maps through `rollUp`. (The existing `todayFlow` re-emits at midnight, so a screen left open past midnight rolls over: stricter than the spec's accepted staleness, same cost.)
  - `ExpenseUseCases.observeSpendingHistory: ObserveSpendingHistoryUseCase` (appended last).
  - `FakeExpenseRepository.getDailyTotals` groups its in-memory expenses by `it.date.toLocalDate()`, filtered by `it.category.name in categories`, honouring its existing `throwIfRequested()`.

- [ ] **Step 1: Write the failing tests**

`SpendingRollupTest` (today = 2026-11-26, a Thursday, unless stated):
- `day_zeroFillsGaps` — daily `[11-23: 500, 11-25: 300]`, DAY → 4 periods 11-23..11-26 with totals `[500, 0, 300, 0]`.
- `week_mondayToSunday` — `[11-22 (Sun): 100, 11-23 (Mon): 200]`, WEEK → `[PeriodTotal(11-16, 11-22, 100), PeriodTotal(11-23, 11-29, 200)]`.
- `week_spanningNewYear` — today 2026-01-02, `[2025-12-29: 10, 2026-01-02: 20]`, WEEK → one period `2025-12-29..2026-01-04` total 30.
- `month_acrossYearBoundary` — today 2026-01-15, `[2025-12-31: 10, 2026-01-01: 20]`, MONTH → `[12/2025: 10, 01/2026: 20]` with `end` = last day of month.
- `noExpenses_singleCurrentPeriod` — empty, WEEK → `[PeriodTotal(11-23, 11-29, 0)]`.
- `newestExpenseOld_includesCurrentPeriod` — `[2026-09-10: 50]`, MONTH → Sep, Oct, Nov with `[50, 0, 0]`.
- `change_normal` — selected 150, previous 100 → `PeriodChange(50, 50.0)`; `change_previousZero` → percent null; `change_noPrevious` → `PeriodChange(selected.total, null)`; `change_decrease` — 80 vs 100 → `PeriodChange(-20, -20.0)`.

`ObserveSpendingHistoryUseCaseTest` (FakeExpenseRepository + `MutableClock(Instant.parse("2026-11-26T10:00:00Z"))`, zone UTC):
- `filtersByCategory` — expenses FOOD 100 and COMMUTE 50 on 11-26; categories `["FOOD"]`, DAY → `today == 2026-11-26`, last period total 100.
- `emptyCategories_allZero` — same data, categories `[]` → every period total 0 (Review Focus 4).
- `groupsByLocalDate` — two expenses on 11-25 at 08:00 and 23:30 → one 11-25 period with their sum (Review Focus 1, fake side).

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*SpendingRollupTest" --tests "*ObserveSpendingHistoryUseCaseTest"`
Expected: FAIL (unresolved references).

- [ ] **Step 3: Implement per the Interfaces block**

- [ ] **Step 4: Run the full suite and the build**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL, all tests pass (≥ 83).

- [ ] **Step 5: Report changed files; controller commits** `feat(F10): daily totals query, period rollup and spending history use case`

---

### Task 2: Spending history chart composable

**Files:**
- Create: `presentation/screen/analysis/components/SpendingHistoryChart.kt`
- Create: `presentation/screen/analysis/components/PeriodLabels.kt` (pure label formatting)
- Modify: `app/src/main/res/values/strings.xml`
- Create (test): `presentation/screen/analysis/PeriodLabelsTest.kt`

**Interfaces:**
- Consumes (Task 1): `PeriodTotal`, `Granularity`.
- Produces:
  - `@Composable fun SpendingHistoryChart(periods: List<PeriodTotal>, granularity: Granularity, selectedStart: LocalDate?, onSelect: (LocalDate) -> Unit, modifier: Modifier = Modifier)` — `periods` oldest first. Internally `LazyRow(reverseLayout = true)` over `periods.asReversed()`, keyed by `start`; `LazyListState` scrolled to index 0 whenever `granularity` changes (`LaunchedEffect(granularity)`); bar width chosen from `BoxWithConstraints` width so about 7 (DAY), 6 (WEEK), 6 (MONTH) bars fit; visible max and average via `derivedStateOf` over `layoutInfo.visibleItemsInfo`; bar heights relative to visible max with `animateDpAsState`; a dashed line (`PathEffect.dashPathEffect`) at the visible average drawn over the row (`drawWithContent` or an overlay `Canvas`); selected bar uses `MaterialTheme.colorScheme.primary`, others a muted container colour from the theme; each bar `clickable(role = Role.Button)` with semantics `contentDescription = "<label>, <amount>"` and `selected = (start == selectedStart)`; label text under each bar from `barLabel`.
  - In `PeriodLabels.kt` (pure, no Compose; take a `Locale`):
    - `fun barLabel(start: LocalDate, granularity: Granularity, locale: Locale): String` — DAY: `"Thu 19"` (`EEE d`); WEEK: `"Nov 16"` (`MMM d` of the Monday); MONTH: `"Nov"` (`MMM`), and `"Jan '26"` for January (`MMM ''yy`) so year changes are visible.
    - `fun summaryLabel(period: PeriodTotal, granularity: Granularity, today: LocalDate, locale: Locale): SummaryLabel` where `sealed interface SummaryLabel { data object Today; data object ThisWeek; data object ThisMonth; data class Text(val value: String) }` — current period → the matching object (UI resolves to strings `analysis_today` "Today", `analysis_this_week` "This week", `analysis_this_month` "This month"); otherwise DAY `"Thu, Nov 19"`, WEEK `"Nov 9 – 15"` (or `"Dec 29 – Jan 4"` across months), MONTH `"October 2026"`.
  - Strings (exact keys and text): `analysis_day` "Day", `analysis_week` "Week", `analysis_month` "Month", `analysis_today` "Today", `analysis_this_week` "This week", `analysis_this_month` "This month", `analysis_vs_previous` "vs previous", `analysis_no_spending` "No spending", `cd_open_analysis` "Spending over time", `cd_back` "Back" (add only if no back description string exists).

- [ ] **Step 1: Write the failing tests** (`Locale.US`)

`PeriodLabelsTest`:
- `barLabel_day` — 2026-11-19, DAY → `"Thu 19"`.
- `barLabel_week` — 2026-11-16, WEEK → `"Nov 16"`.
- `barLabel_month_january_showsYear` — 2026-01-01, MONTH → `"Jan '26"`; 2026-11-01 → `"Nov"`.
- `summary_currentPeriods` — today 2026-11-26: day 11-26 → `Today`; week 11-23..11-29 → `ThisWeek`; month 11-01..11-30 → `ThisMonth`.
- `summary_pastDay` — 2026-11-19 → `Text("Thu, Nov 19")`.
- `summary_pastWeek_sameMonth` — 11-09..11-15 → `Text("Nov 9 – 15")`; `summary_pastWeek_acrossMonths` — 2025-12-29..2026-01-04 → `Text("Dec 29 – Jan 4")`.
- `summary_pastMonth` — 2026-10-01..10-31 → `Text("October 2026")`.

- [ ] **Step 2: Run to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "*PeriodLabelsTest"` — Expected: FAIL.

- [ ] **Step 3: Implement `PeriodLabels.kt`, the strings and `SpendingHistoryChart` with two `@Preview`s (DAY with 30 periods incl. zeros; MONTH with 14 periods), light and dark via `LazyWalletTheme`**

- [ ] **Step 4: Run the full suite and the build**

Run: `./gradlew testDebugUnitTest assembleDebug` — Expected: BUILD SUCCESSFUL, all pass.

- [ ] **Step 5: Report changed files; controller commits** `feat(F10): scrollable spending history chart and period labels`

---

### Task 3: AnalysisViewModel and screen on the new history

**Files:**
- Modify: `presentation/screen/analysis/AnalysisStateEvents.kt`, `AnalysisViewModel.kt`, `AnalysisRoute.kt`
- Modify: `di/appModule.kt` only if the VM constructor gains a dependency Koin can't resolve (`Clock` is already a `single`)
- Delete: `presentation/screen/analysis/components/DailyExpenseBarChart.kt`; `domain/model/DailyExpense.kt` and `toLast7DaysExpenses()` if nothing else references them (grep first)
- Modify (test): `presentation/screen/analysis/AnalysisViewModelTest.kt`

**Interfaces:**
- Consumes (Task 1): `ExpenseUseCases.observeSpendingHistory` (emits `SpendingHistory(today, periods)`), `Granularity`, `PeriodTotal`, `periodStart`, `periodEnd`, `change`, `PeriodChange`; existing `getExpensesByDateRange(categories, start, end)`. (Task 2): `SpendingHistoryChart`, `summaryLabel`, `SummaryLabel`, the `analysis_*` strings.
- Produces:
  - `AnalysisViewModel(savedStateHandle: SavedStateHandle, expenseUseCases: ExpenseUseCases, profileUseCases: ProfileUseCases, clock: Clock)`.
  - SavedStateHandle keys: `"granularity"` (enum `name`), `"selected_start"` (epoch day). Remove `"selected_date"`. On construction, a restored `selected_start` is normalised with `periodStart(granularity)`.
  - `data class HistoryState(val granularity: Granularity = Granularity.DAY, val periods: List<PeriodTotal> = emptyList(), val selected: PeriodTotal? = null, val change: PeriodChange? = null, val today: LocalDate? = null)` replacing `DateState`; `UiState.Success(historyState, expensesState, categoryProfileState)`; `ExpensesState.filteredExpenses` = the selected period's expenses (drop the raw `expenses` list if unused).
  - Selection rule: `selected` = the period whose `start == selectedStart`; when `selectedStart` is null (default, or after `SelectGranularity`) it is the newest period (the current one).
  - `selectedExpenses` from `getExpensesByDateRange(activeCategories, selected.start.atStartOfDay(), selected.end.atTime(LocalTime.MAX))`, re-queried via `flatMapLatest` when the selection or profile changes.
  - Events: `SelectGranularity(granularity: Granularity)` (sets granularity, clears `selectedStart`, persists both), `SelectPeriod(start: LocalDate)`, `SelectCategoryProfile(profileName)` and `Retry` unchanged. Remove `SelectBar` and `DeselectBar`.
  - Route layout per spec "UI" items 1–4: `SingleChoiceSegmentedButtonRow` (Day/Week/Month) → summary (label via `summaryLabel` + `stringResource` for the objects; total; change as "▲ ₱120 (+15%) vs previous" / "▼ …"; omit the percent when null; omit the change line for the very first period) → `SpendingHistoryChart` → the selected period's expense list (reuse the existing list composable; show `analysis_no_spending` when empty). Keep the category-profile dropdown in the top bar. (Back arrow and navigation come in Task 4.)

- [ ] **Step 1: Write the failing tests** (rewrite `AnalysisViewModelTest` on fakes + `MutableClock(Instant.parse("2026-11-26T10:00:00Z"))`, UTC; collect `uiState` in a `launch` and use `runCurrent()`)
- `defaults_dayGranularity_currentPeriodSelected` — `historyState.granularity == DAY`, `selected.start == 2026-11-26`.
- `selectPeriod_selectsAndLoadsItsExpenses` — expenses FOOD 40 on 11-23, FOOD 100 on 11-24, FOOD 70 on 11-26; `SelectPeriod(11-24)` → `selected.start == 11-24`, `filteredExpenses` = just the 11-24 one, `change == PeriodChange(60, 150.0)`.
- `selectGranularity_resetsToCurrentPeriod` — after `SelectPeriod(11-24)`, `SelectGranularity(WEEK)` → `selected.start == 2026-11-23 && selected.end == 2026-11-29` and `savedStateHandle["granularity"] == "WEEK"`, `savedStateHandle.get<Long>("selected_start") == null`.
- `weekSelection_listsWholeWeek` — WEEK, select 11-16 → `filteredExpenses` contains expenses dated 11-16 … 11-22 only (seed one on 11-15 and one on 11-23 to prove the edges).
- `emptyProfile_showsZeroBars` — active profile with every category off → every `periods[i].total == 0L` and `filteredExpenses` empty (Review Focus 4).
- `restore_normalisesSelectionToPeriodStart` — `SavedStateHandle(mapOf("granularity" to "WEEK", "selected_start" to LocalDate.of(2026,11,18).toEpochDay()))` → `selected.start == 2026-11-16` (Review Focus 5).
- Keep a retry/error test equivalent to the existing one (fake `throwIfRequested`) asserting `UiState.Error` then `Success` after `Event.Retry`.

- [ ] **Step 2: Run to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*AnalysisViewModelTest"` — Expected: FAIL.

- [ ] **Step 3: Implement the state, ViewModel and Route per the Interfaces block; delete the old chart files**

- [ ] **Step 4: Run the full suite and the build**

Run: `./gradlew testDebugUnitTest assembleDebug` — Expected: BUILD SUCCESSFUL, all pass.

- [ ] **Step 5: Report changed files; controller commits** `feat(F10): Analysis screen on scrollable Day/Week/Month history`

---

### Task 4: Open Analysis from Expenses as a detail screen

**Files:**
- Modify: `presentation/screen/expenselist/ExpenseListRoute.kt` (top-bar action), `presentation/screen/analysis/AnalysisRoute.kt` (back arrow), `presentation/nav/NavGraph.kt`

**Interfaces:**
- Consumes (Task 2): `cd_open_analysis`, `cd_back` (or the existing back description).
- Produces:
  - `ExpenseListRoute(…, onOpenAnalysis: () -> Unit)` — `TopAppBar` `actions` gains `IconButton(onClick = onOpenAnalysis) { Icon(Icons.Filled.BarChart, contentDescription = stringResource(R.string.cd_open_analysis)) }` (the import already exists).
  - `AnalysisRoute(viewModel, onBack: () -> Unit)` — `TopAppBar` `navigationIcon` = back arrow (`Icons.AutoMirrored.Filled.ArrowBack`) calling `onBack`; category dropdown stays in `actions`.
  - `NavGraph`: Expenses passes `onOpenAnalysis = { backStackEntry.ifResumed { navController.navigate(Routes.AnalysisRoute) } }`; Analysis passes `onBack = { backStackEntry.ifResumed { navController.navigateUp() } }`; remove the `AnalysisRoute → NavOptions.ANALYSIS` line so `selectedOption` is null there (no bottom bar, like BudgetDetails). Leave `NavOptions.ANALYSIS` and the `BottomNavBar` filter as they are.

- [ ] **Step 1: Implement per the Interfaces block (no unit test: navigation and top-bar wiring are covered by the build and the device check)**

- [ ] **Step 2: Verify**

Run: `./gradlew testDebugUnitTest assembleDebug` — Expected: BUILD SUCCESSFUL, all pass.
Run: `grep -n "AnalysisRoute" app/src/main/java/com/luna/budgetapp/presentation/nav/NavGraph.kt` — Expected: the `composable<Routes.AnalysisRoute>` block and the navigate call; no `NavOptions.ANALYSIS` mapping line.

- [ ] **Step 3: Report changed files; controller commits** `feat(F10): open Analysis from Expenses; Analysis is a detail screen`

---

## After the tasks (controller)

- Final whole-branch review; tracker (F10 → Building, then Features-done after merge).
- Device check for the user (spec "Done when"): chart icon on Expenses opens Analysis, back returns, no bottom bar; drag back months smoothly at each granularity; tap bars; switch the category profile; an expense logged at 23:30 lands on that day; rotate / "Don't keep activities" restores granularity and selection.
