# F10 Analysis history and timelines — design

Date: 2026-11-26 · Status: approved 2026-11-26 · Owner: Jose Luna · Branch: `feat/analysis-history` · Seed notes (brainstorm 2026-11-25) folded in here

## Intent and why

Jose wants to compare spending over time: today against previous days, this week against previous weeks, this month against previous months. The Analysis screen used to be reachable but was hidden because its bar chart was fixed to the last 7 days and couldn't go further back. F10 rebuilds the chart so it scrolls back through all history at Day, Week or Month level, and makes the screen reachable again.

## Goals

- Analysis is reachable from a chart icon in the Expenses screen's top bar.
- Day / Week / Month switch; newest bar on the right; drag right to go back in time with no hard limit.
- Selected bar's total and its change against the previous bar, shown at the top.
- Dashed line at the average of the bars currently in view.
- Tap a bar to select it and list that period's expenses below the chart.
- The active category profile filters everything, as today.

## Non-goals

- Making Analysis a bottom-bar tab (→ F05/P17, if ever). The bottom bar stays at three tabs.
- Real top-app-bar titles across screens (→ P16/F05). The Expenses top bar stays title-less; only an action icon is added.
- New chart types (pie, line), budgets on the chart, forecasting, export.
- Changing how expenses are stored.

## Anti-goals

- A chart that stutters or janks while dragging through a few years of data.
- Silent gaps: a period with no spending disappearing instead of showing as zero.
- Bars that silently ignore the active category profile.
- Timezone drift: an expense at 23:30 counted on the wrong day.

## Decisions (Jose, 2026-11-25 and 2026-11-26)

| Topic | Decision |
|---|---|
| Layout | A "compare-first" (seed, 2026-11-25) |
| Entry | Option B: from another screen, specifically a chart icon on the Expenses screen (2026-11-26) |
| Change vs previous, current (unfinished) period | **a**: period-so-far vs the *whole* previous period (2026-11-26). Finished periods compare full totals. |
| Chart build | Approach 1A: bars as composables in a `LazyRow` (2026-11-26) |
| Data | Approach 2A: daily totals from Room across all history, rolled up to weeks/months in Kotlin (2026-11-26) |
| Defaults | Accepted 2026-11-26 (proposed in seed, not objected to): opens on **Day** with **today** selected; about **7** bars in view for Day, **5–6** for Week, **6** for Month; weeks **Monday–Sunday**; empty periods show a **zero bar**; y-scale **fits the bars in view** (rescales while dragging); category profile filter applies |
| Tap behaviour | "Good enough for now": tap selects the bar and lists that period's expenses |

## Approaches considered

**Chart**
- **A. `LazyRow` of bar composables (chosen).** `reverseLayout = true` puts the newest bar at the right and scrolling right goes back in time; scroll, fling and lazy loading come from the list. The average line and the fit-to-view scale are computed from `LazyListState.layoutInfo.visibleItemsInfo`. Taps are ordinary `clickable`s; each bar can carry a semantics label. No new dependency; replaces the existing `Canvas` chart.
- **B. Chart library (e.g. Vico).** Built-in scroll and axes, but the average-of-visible and rescale-while-dragging rules need custom work against its API, plus a dependency and theming to the Gruvbox palette. Rejected.
- **C. Extend the current `Canvas` with drag.** Hand-written scroll physics, hit-testing and accessibility. Most code, easiest to get subtly wrong. Rejected.

**Data**
- **A. One daily-totals query over all history (chosen).** A Room query returns `(local date, total)` per day that has spending, filtered by the active categories. A pure Kotlin function fills missing days with zero and rolls days into Monday–Sunday weeks and calendar months. Years of data is about a thousand small rows. Tapping a bar reuses the existing `getExpensesByDateRange` for that period.
- **B. Per-granularity paged queries.** Three SQL groupings plus paging as you drag. Scales without limit but is more code than this app's data needs. Rejected.

## Current state (code, 2026-11-26)

| Where | Today |
|---|---|
| `presentation/screen/analysis/components/DailyExpenseBarChart.kt` | Custom `Canvas`; `expenses.toLast7DaysExpenses()`; tap via `detectTapGestures`. |
| `AnalysisViewModel` | `DateState(dateFilter = Last7Days, selectedDate)`; loads raw expenses for the 7-day range via `getExpensesByDateRange(categories, start, end)` and filters the selected day in memory; `selectedDate` persisted in `SavedStateHandle` (`selected_date`, epoch day, P28); P25 retry pipeline; category profile from `observeCategoryProfileState()`. |
| `AnalysisRoute` | Empty `TopAppBar` with the category-profile dropdown action; chart; `AnimatedVisibility` list of the selected day's expenses. |
| Navigation | `Routes.AnalysisRoute` registered in `NavGraph.kt:153`; nothing navigates to it. `BottomNavBar.kt:17` filters out `NavOptions.ANALYSIS`. `NavGraph` maps an `AnalysisRoute` destination to `NavOptions.ANALYSIS`, so the bar would show with no item selected. |
| `ExpenseListRoute` | Empty `TopAppBar(title = {})` at line ~117; `Icons.Filled.BarChart` imported but unused. |
| Storage | `expenses.date` is a `LocalDateTime` stored as epoch **milliseconds** in the system zone (`Converters.kt`); `category` is a string column; amounts are `Long` cents. |

## Design

### Data

- **DAO** (`ExpenseDao`): `fun getDailyTotals(categories: List<String>): Flow<List<DailyTotalProjection>>`:
  ```sql
  SELECT strftime('%Y-%m-%d', date / 1000, 'unixepoch', 'localtime') AS day,
         SUM(amount) AS total
  FROM expenses
  WHERE category IN (:categories)
  GROUP BY day
  ORDER BY day
  ```
  `DailyTotalProjection(day: String, total: Long)`. `'localtime'` uses the device zone, matching how `Converters` writes dates.
- **Domain**: `data class PeriodTotal(val start: LocalDate, val end: LocalDate, val total: Long)` and `enum class Granularity { DAY, WEEK, MONTH }`. Repository method `getDailyTotals(categories): Flow<List<Pair<LocalDate, Long>>>` (or a small domain type; builder's call), and a use case `ObserveSpendingHistoryUseCase(categories, granularity, today): Flow<List<PeriodTotal>>`.
- **Rollup** (pure function, unit-tested), `rollUp(daily, granularity, today): List<PeriodTotal>`:
  - Range runs from the period containing the earliest expense to the period containing `today`, **oldest first**. With no expenses, it returns just the current period with total 0.
  - Every period in the range is present; periods with no spending have total 0.
  - Week = Monday–Sunday (`DayOfWeek.MONDAY`). Month = calendar month.
- **Comparison** (pure function): `change(selected, previous)` returns the absolute difference and the percentage. If there is no previous period, or the previous total is 0, the percentage is null and the UI shows only the absolute amount. For the current period this is "so far vs whole previous", per decision a.

### ViewModel (`AnalysisViewModel`)

- State gains `granularity: Granularity` (default DAY) and `selectedStart: LocalDate?` (start of the selected period; default = start of the current period). Both are persisted in `SavedStateHandle` (`granularity` as the enum name, `selected_start` as an epoch day), replacing `selected_date`.
- `uiState` combines the history Flow (keyed on active categories + granularity), the selection, and the category profile. It exposes `periods: List<PeriodTotal>` (oldest first), the selected period, the previous period's total and the change, and `selectedExpenses` (from `getExpensesByDateRange(categories, selected.start atStartOfDay, selected.end atTime MAX)`). The P25 retry and error pipeline stays as it is.
- Events: `SelectGranularity(Granularity)`, which resets the selection to the current period; `SelectPeriod(start: LocalDate)`; the existing `DeselectBar`, `SelectCategoryProfile` and `Retry`. `SelectBar(date)` is replaced by `SelectPeriod`.
- "Today" comes from an injectable clock. The test tree already has `MutableClock`; follow the pattern other ViewModels use for time.

### UI

- **Entry**: in `ExpenseListRoute`'s `TopAppBar`, add an `actions` `IconButton` with `Icons.Filled.BarChart` and a content description from strings (e.g. `cd_open_analysis` = "Spending over time"). The Route takes an `onOpenAnalysis: () -> Unit` lambda. `NavGraph` passes `{ backStackEntry.ifResumed { navController.navigate(Routes.AnalysisRoute) } }` (F02 pattern).
- **Analysis as a detail screen**: no bottom bar, like BudgetDetails. Remove the `AnalysisRoute → NavOptions.ANALYSIS` mapping so `selectedOption` is null there. The top bar gets a back arrow (`onBack` lambda, `ifResumed { navigateUp() }`, as BudgetDetails does) plus the existing category-profile dropdown. *(My call: this matches how the app treats BudgetDetails, and with three tabs there is no Analysis tab to highlight.)*
- **Layout, top to bottom**:
  1. Day / Week / Month `SingleChoiceSegmentedButtonRow`.
  2. Summary: the selected period's label ("Today", "Wed, Nov 19"; "This week", "Nov 10 – 16"; "November", "October 2026"), its total via `toCurrency`/`toPeso` (builder's call, consistent with the screen), and the change, e.g. "▲ ₱120 (+15%) vs previous". Up = spent more. No colour judgement beyond the theme's existing tokens.
  3. Chart: `LazyRow(reverseLayout = true)` over `periods.asReversed()`. Fixed bar width per granularity, chosen so about 7 / 5–6 / 6 bars fit the screen width. The selected bar is highlighted. A label under each bar (day: weekday letter + date; week: start date; month: short month name). A dashed horizontal line at the average of the visible bars' totals. Bar heights scale to the max of the visible bars, and the height change animates while dragging. On open and on a granularity switch, scroll to the newest item (index 0).
  4. The selected period's expense list. Reuse the current `AnimatedVisibility` list pattern or show it always for the selected period (builder's call). Empty period: a short "No spending" line.
- **Accessibility**: each bar's semantics = period label + amount + "selected" state; the bar is a `Role.Button`.
- **Strings**: all new text in `strings.xml` (P23 rule).
- **Delete**: `DailyExpenseBarChart.kt` and `toLast7DaysExpenses()` if nothing else uses them.

### Tests

- Rollup: zero-fill for days; Monday–Sunday weeks, including an expense on a Sunday and on a Monday; months across a year boundary; the current period included when the newest expense is older than today; no expenses returns a single zero period.
- Change: normal, previous 0 (percentage null), first period (no previous).
- DAO day grouping in local time, an expense at 23:30 local, is an instrumented Room test (`connectedAndroidTest`) if one can be added cheaply; otherwise cover the mapping from the projection in a JVM test and list the 23:30 check as a device check.
- `AnalysisViewModelTest` on fakes: defaults (DAY, current period selected), switching granularity resets the selection, selecting a period loads that range's expenses, the category profile filters history, `SavedStateHandle` restores granularity and selection.

## Behaviour to keep

| Path | Must stay |
|---|---|
| Category profile dropdown on Analysis | Changes the active profile app-wide, as today |
| Load failure | P25 Retry screen |
| Process death | Selection restored (now granularity + period) |
| Expenses screen | Everything else unchanged; only the icon is added |

## Done when

- The chart icon on Expenses opens Analysis; back returns to Expenses; no bottom bar on Analysis.
- Day / Week / Month all scroll back to the first expense; empty periods show zero bars.
- Unit tests pass (count goes up from 70); `assembleDebug` OK.
- Device check: drag back several months smoothly; tap bars at each granularity; switch the category profile; an expense logged late in the evening lands on the right day; rotate or process-death restores the selection.

## Clarifications (builder check, 2026-11-26)

- **"Today" (my call):** "today" is read from the injected clock when the history Flow is collected, and again whenever `uiState` is re-subscribed (the screen returns after `WhileSubscribed(5000)` lapses). If the screen stays open across midnight, it shows yesterday as "today" until it is reopened. That's accepted; there is no midnight ticker. If the time zone changes, `strftime('localtime')` and `Converters` both use the current zone, so day totals and the tapped-period list agree. Old expenses may move to a neighbouring day, which is accepted.
- **Change comparison:** always the selected period against the whole previous period, with no like-for-like rule (decision a). For finished periods, both are full totals anyway.
- **Empty category profile (my call):** if no categories are active, `IN ()` matches nothing and every bar is zero, the same as the Expenses screen today. Don't fall back to "all", and don't crash. Add a test for it.
- **Performance (my call):** compute the visible average and max with `derivedStateOf` over `layoutInfo.visibleItemsInfo`, with no extra debouncing. Bars are lazy items, so hundreds of Day bars are fine.
- **Selection scrolled out of view (my call):** the selection stays until another bar is tapped. The summary and the list keep showing the selected period while you drag. That matches "tap behaviour good enough for now". There is no auto-select and no re-centring.
- **Granularity switch:** resets the selection to the current period and scrolls to the newest bar, as above.
- **Restoring state (my call):** on restore, `selected_start` is normalised to the start of the period containing that date for the restored granularity.

## Left to the builder

- Exact bar widths, colours from the existing theme, label formats within the examples above.
- Whether the repository returns pairs or a small domain type; file names.
- Whether the expense list is always visible or toggled.
- Task split and order in the plan.
