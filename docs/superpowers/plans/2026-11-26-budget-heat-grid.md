# F14 Budget heat grid, outlook sheet and day popover — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Budget screen's F12 gauge with a "₱X left this month" hero over a daily-spend heat grid, a day popover (M3 `RichTooltip`) and a "<Month> outlook" bottom sheet with a cumulative chart labelled against the budget.

**Architecture:** A new unfiltered daily-totals read (DAO → repository → use case) feeds a pure `MonthOutlook` model built in `BudgetViewModel` from budgets + daily totals + `_today`. The Route renders it with two new components (`BudgetHeatGridCard` with its popover, `OutlookSheet` with its chart); the popover day and sheet visibility are Route-local `rememberSaveable` state.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3 (BOM 2026.04.01), Room, Koin, JUnit4/Truth/coroutines-test.

**Spec:** `docs/superpowers/specs/2026-11-26-budget-heat-grid-design.md` (read §5 for every visual value; this plan does not repeat them).

## Global Constraints

- All amounts are `Long` cents; convert only in UI. Hero, popover, sheet amounts are **whole pesos** ("₱15,700"), rounded half-up.
- All spending counts (no category filter). "today" comes only from `BudgetViewModel._today` (`todayFlow(clock)`).
- Grid weeks start on **Sunday**; current month only; future cells not clickable and hidden from TalkBack.
- `projected` is null while `today.dayOfMonth < 3`; UI shows "Too early" / "Too early to project".
- Intensity = `0.38 + 0.62 * spent / maxSpent` for days with spend; zero-spend days have no fill.
- Sheet's dashed budget line is labelled "Budget ₱X"; no budgets ⇒ no budget line/label.
- New text goes in `res/values/strings.xml` (escape apostrophes). Never name anything "Home". Never commit `docs/screens/`.
- Tests with a live `uiState` collector use `runCurrent()`/`advanceTimeBy`, never `advanceUntilIdle()`; cancel the job.
- Run from `android/`: `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` must pass at the end of every task.

## Review Focus

1. **Month boundary at midnight** (today = last day → 1st): grid, hero and sheet switch month together; previous-month spend leaves "spent so far". Test in Task 3.
2. **Expenses dated in the future or in last month** in the raw daily totals: ignored by `spentSoFar`, grid and chart. Test in Task 2.
3. **Expense with a category string not in `Category.entries`** still counts. Pinned by the Task 1 DAO query having no `WHERE` (reviewer checks; the domain `Expense` can't carry an unknown category, so no unit test).
4. **Popover near the screen edge** (Saturday / Sunday columns) and after the list scrolls: stays on screen; closes on list scroll. Device check in Task 4.
5. **Overspent month with a high pace**: chart Y scale grows to fit `projected`/`spentSoFar`, budget label stays readable, hero shows "over". Test in Task 2 (`chartMax`), preview in Task 5.

---

### Task 1: Unfiltered daily totals read

**Files:**
- Modify: `app/src/main/java/com/luna/budgetapp/data/local/dao/ExpenseDao.kt` (next to `getDailyTotals`)
- Modify: `app/src/main/java/com/luna/budgetapp/domain/repository/ExpenseRepository.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/data/local/repository/ExpenseRepositoryImpl.kt` (next to `getDailyTotals`)
- Create: `app/src/main/java/com/luna/budgetapp/domain/usecase/expense/ObserveAllDailyTotalsUseCase.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/domain/usecase/ExpenseUseCases.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/di/appModule.kt` (beside `factoryOf(::ObserveSpendingHistoryUseCase)`)
- Modify: `app/src/test/java/com/luna/budgetapp/domain/repository/FakeExpenseRepository.kt`
- Modify (add the new named arg `observeAllDailyTotals = mockk()`): every test that builds `ExpenseUseCases(...)`: `expenselist/ExpenseListViewModelTest.kt`, `presentation/screen/analysis/AnalysisViewModelTest.kt`, `presentation/screen/budgetdetails/BudgetDetailsViewModelTest.kt`, `presentation/screen/expensepreset/ExpensePresetViewModelTest.kt`, `presentation/screen/budget/BudgetViewModelTest.kt`
- Test: `app/src/test/java/com/luna/budgetapp/domain/usecase/expense/ObserveAllDailyTotalsUseCaseTest.kt`

**Interfaces:**
- Produces: `ExpenseDao.getAllDailyTotals(): Flow<List<DailyTotalProjection>>` (same SQL as `getDailyTotals` without the `WHERE`); `ExpenseRepository.getAllDailyTotals(): Flow<List<DailyTotal>>`; `class ObserveAllDailyTotalsUseCase(repository: ExpenseRepository) { operator fun invoke(): Flow<List<DailyTotal>> }`; `ExpenseUseCases.observeAllDailyTotals: ObserveAllDailyTotalsUseCase` (add as the last constructor property).

- [ ] **Step 1: Write the failing test** `ObserveAllDailyTotalsUseCaseTest`

```kotlin
@Test fun `sums every expense per local day regardless of category`() = runTest {
    // FakeExpenseRepository seeded with: FOOD ₱100 and BEVERAGE ₱50 on 2026-11-18 10:00, FOOD ₱30 on 2026-11-19
    val totals = ObserveAllDailyTotalsUseCase(repo)().first()
    assertThat(totals).containsExactly(
        DailyTotal(LocalDate.of(2026, 11, 18), 15_000L),
        DailyTotal(LocalDate.of(2026, 11, 19), 3_000L),
    ).inOrder()
}
@Test fun `throws when the repository read fails`() // shouldThrow = true → first() throws
```

The "category not in `Category.entries`" case (Review Focus 3) can't be built through the domain `Expense` (its `category` is the enum), so it is pinned by the SQL having no `WHERE`; the reviewer checks the DAO query text.

- [ ] **Step 2: Run it, expect a compile failure** — `./gradlew testDebugUnitTest --tests "*ObserveAllDailyTotalsUseCaseTest"`
- [ ] **Step 3: Implement** the DAO query, repository method (map like `getDailyTotals`, `flowOn(Dispatchers.IO)`), fake (`getDailyTotals` minus the category filter, honouring `throwIfRequested()`), use case, `ExpenseUseCases` field, Koin `factoryOf(::ObserveAllDailyTotalsUseCase)`, and the five test builders.
- [ ] **Step 4: Run all unit tests** — `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL
- [ ] **Step 5: Commit** — `feat(F14): unfiltered daily totals read`

---

### Task 2: `MonthOutlook` model

**Files:**
- Create: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/model/MonthOutlook.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/screen/utils/Utils.kt` (add `toWholePeso`)
- Test: `app/src/test/java/com/luna/budgetapp/presentation/screen/budget/MonthOutlookTest.kt`

**Interfaces:**
- Consumes: `DailyTotal(date: LocalDate, total: Long)` (domain model).
- Produces:

```kotlin
enum class DayKind { Past, Today, Future }
data class OutlookDay(val date: LocalDate, val spent: Long, val kind: DayKind, val intensity: Float?) // intensity null = no fill
data class MonthOutlook(
    val today: LocalDate,
    val budgetTotal: Long,
    val days: List<OutlookDay>,      // exactly lengthOfMonth entries, day 1 first
    val spentSoFar: Long,
    val remaining: Long,              // budgetTotal - spentSoFar
    val daysElapsed: Int,
    val daysInMonth: Int,
    val daysLeft: Int,
    val projected: Long?,             // null when daysElapsed < 3
    val avgPerDay: Long,
    val highest: OutlookDay?,         // max spent among Past/Today, earliest on tie; null if nothing spent
    val leadingBlanks: Int,           // Sunday-first: SUNDAY=0 … SATURDAY=6 for day 1
    val chartMax: Long,               // max(budgetTotal, projected ?: 0, spentSoFar)
) {
    val hasBudgets: Boolean get() = budgetTotal > 0
    fun runningTotal(date: LocalDate): Long   // Σ spent day 1..date (date clamped to today)
    companion object { fun from(today: LocalDate, budgetTotal: Long, dailyTotals: List<DailyTotal>): MonthOutlook }
}
fun Long.toWholePeso(): String  // cents → "₱15,700", half-up; negative → "₱1,234" of the absolute value (caller words "over")
```

Rounding for `projected` and `avgPerDay`: `BigDecimal` division, `RoundingMode.HALF_UP`, in cents.

- [ ] **Step 1: Write the failing tests** in `MonthOutlookTest` (build input with a helper `day(d: Int, pesos: Int) = DailyTotal(LocalDate.of(2026, 11, d), pesos * 100L)`):

```kotlin
@Test fun `mockup figures`()  // today 2026-11-26, budget 2_800_000, spend totalling 1_230_000 over days 1..26
    // remaining == 1_570_000; projected == 1_419_231; avgPerDay == 47_308; daysLeft == 4; daysInMonth == 30
    // projected!!.toWholePeso() == "₱14,192"; remaining.toWholePeso() == "₱15,700"
@Test fun `projected is null on day 1 and 2 and present on day 3`()
@Test fun `intensity is 1 for the biggest day, at least 0_38 for the smallest spend day, null for zero days`()
@Test fun `future and previous-month totals are ignored`()   // a 2026-11-28 and a 2026-10-31 total → not in spentSoFar, days[27].spent == 0, kind Future
@Test fun `running total includes the given day`()           // runningTotal(18 Nov) == Σ days 1..18
@Test fun `leading blanks are Sunday-first`()                // Nov 2026 (starts Sunday) → 0; Feb 2026 (starts Sunday) → 0; Dec 2026 (Tuesday) → 2
@Test fun `month lengths`()                                   // Feb 2026 → 28 days, Dec 2026 → 31
@Test fun `over budget gives negative remaining and chartMax covers projected`() // budget 100_000, spent 150_000 by day 10 of 30 → remaining == -50_000, chartMax == projected == 450_000
@Test fun `no budgets`()                                      // budgetTotal 0 → hasBudgets false, chartMax == max(projected, spentSoFar)
@Test fun `highest tie picks the earliest day and is null with no spend`()
@Test fun `toWholePeso rounds half up and drops the sign`()    // 1_419_231 → "₱14,192"; 50 → "₱1"; -123_400 → "₱1,234"
```

- [ ] **Step 2: Run, expect failure** — `./gradlew testDebugUnitTest --tests "*MonthOutlookTest"`
- [ ] **Step 3: Implement** `MonthOutlook.from` and `toWholePeso` to the signatures above.
- [ ] **Step 4: Run** — same command → PASS; then `./gradlew testDebugUnitTest` → BUILD SUCCESSFUL
- [ ] **Step 5: Commit** — `feat(F14): MonthOutlook model`

---

### Task 3: ViewModel exposes `MonthOutlook`

**Files:**
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/BudgetViewModel.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/BudgetStateEvents.kt`
- Modify: `app/src/test/java/com/luna/budgetapp/presentation/screen/budget/BudgetViewModelTest.kt`

**Interfaces:**
- Consumes: `expenseUseCases.observeAllDailyTotals()` (Task 1), `MonthOutlook.from` (Task 2).
- Produces: `UiState.Success(budgets, expenses, outlook: MonthOutlook, dialog)` — `monthlyOutlook: OutlookDetails` is **removed** from `Success`; `_totalAmount` and `_monthlyOutlook` are deleted. Keep `OutlookDetails.kt` and its test (still imported by `BudgetDetailsCard`). `budgetTotal = budgets.sumOf { it.frequency.projectAmountToMonth(it.limit) }`.

- [ ] **Step 1: Rewrite the outlook tests** in `BudgetViewModelTest` (replace the three that read `monthlyOutlook`; wire `observeAllDailyTotals = ObserveAllDailyTotalsUseCase(expenseRepo)`):

```kotlin
@Test fun `success carries the month outlook from all spending`()       // two expenses in different categories this month → outlook.spentSoFar == their sum
@Test fun `outlook switches month at midnight`()                         // clock 2026-03-31T23:59Z with March spend; advance 2 min + runCurrent → outlook.today == 2026-04-01, spentSoFar == 0, daysInMonth == 30
@Test fun `budget total is the monthly projection of all budgets`()      // one MONTHLY ₱1,000 budget → outlook.budgetTotal == 100_000
```

Keep the existing Error/Retry test passing (the daily-totals fake must honour `shouldThrow`).

- [ ] **Step 2: Run, expect failure** — `./gradlew testDebugUnitTest --tests "*BudgetViewModelTest"`
- [ ] **Step 3: Implement** — `_outlook = combine(_budgets, expenseUseCases.observeAllDailyTotals(), _today) { … MonthOutlook.from(...) }`, swap into `_successState`. To keep the app building, delete the `BudgetOutlookCard(...)` call from `BudgetRoute` and leave a `// F14 Task 4: hero card` placeholder line, so the app builds; the destructuring in `BudgetRoute` must match the new `Success`.
- [ ] **Step 4: Run** — `./gradlew testDebugUnitTest` and `./gradlew assembleDebug` → BUILD SUCCESSFUL
- [ ] **Step 5: Commit** — `feat(F14): Budget ViewModel exposes MonthOutlook`

---

### Task 4: Hero card with heat grid and day popover

**Files:**
- Create: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/components/BudgetHeatGridCard.kt`
- Delete: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/components/BudgetOutlookCard.kt` (and its now-unused strings `outlook_spent_of`, `outlook_cd`; keep `outlook_no_budgets`)
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/BudgetRoute.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `MonthOutlook`, `OutlookDay`, `DayKind`, `toWholePeso()` (Task 2).
- Produces:

```kotlin
@Composable fun BudgetHeatGridCard(
    outlook: MonthOutlook,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,   // Route toggles: same day → null, else that day
    onDismissDay: () -> Unit,
    onClick: () -> Unit,               // opens the sheet (Task 5)
    modifier: Modifier = Modifier,
)
```

Route state: `var selectedDay by rememberSaveable(stateSaver = …LocalDate as epochDay Long…) { mutableStateOf<LocalDate?>(null) }`; clear it when the `LazyListState` starts scrolling (`LaunchedEffect(listState.isScrollInProgress)`), when the sheet opens, and when `outlook.today` changes month.

Popover: each Past/Today cell is wrapped in `TooltipBox(positionProvider = TooltipDefaults.rememberRichTooltipPositionProvider(), tooltip = { RichTooltip(caretShape = TooltipDefaults.caretShape(), colors = …, shape = RoundedCornerShape(12.dp), modifier = Modifier.width(220.dp)) { … } }, state = rememberTooltipState(isPersistent = true))`; a `LaunchedEffect(selectedDay)` calls `state.show()` for the selected cell and `state.dismiss()` otherwise; the tooltip's own outside-tap dismissal calls `onDismissDay` (observe `state.isVisible` going false). If the installed M3 has no `caretShape` parameter, use the `caretSize = TooltipDefaults.caretSize` overload — whichever compiles; note it in the report.

Strings to add (exact copy): `outlook_left_this_month` "left this month", `outlook_over_this_month` "over this month", `outlook_hero_cd` "%1$s %2$s. Opens %3$s outlook.", `outlook_day_cd` "%1$s, %2$s spent", `outlook_day_nothing_cd` "%1$s, nothing spent", `outlook_spent` "spent", `outlook_running_total` "Running total", `outlook_projected_month` "Projected month", `outlook_too_early` "Too early". Popover date pattern `"EEE, d MMM"`; TalkBack date `"EEE d MMM"`; month name `"MMMM"`, device locale.

- [ ] **Step 1: Write `@Preview`s** (dark + light) for: mockup month (today 26 Nov, popover closed), popover open on 18 Nov, overspent, no budgets, day 2 ("Too early" in popover). Previews build `MonthOutlook.from(...)` from fixed data.
- [ ] **Step 2: Implement** `BudgetHeatGridCard` per spec §5.2–5.3 (values there), and wire it into `BudgetRoute` in place of the Task 3 placeholder, with the Route state above; `onClick` sets a `showOutlookSheet` saveable flag (consumed in Task 5; until then it only clears `selectedDay`).
- [ ] **Step 3: Verify** — `./gradlew testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL; `grep -rn "BudgetOutlookCard\|outlook_cd\|outlook_spent_of" app/src/main` → no matches.
- [ ] **Step 4: Commit** — `feat(F14): heat grid hero card and day popover`

Device check (user, after Task 5): tap days in the Sunday and Saturday columns; scroll the list with a popover open; TalkBack on a cell.

---

### Task 5: Outlook bottom sheet with chart

**Files:**
- Create: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/components/OutlookSheet.kt` (sheet, tiles, stats)
- Create: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/components/OutlookChart.kt` (Canvas chart)
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/BudgetRoute.kt`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `MonthOutlook` (Task 2), `showOutlookSheet` flag in `BudgetRoute` (Task 4).
- Produces:

```kotlin
@Composable fun OutlookSheet(outlook: MonthOutlook, onDismiss: () -> Unit)   // ModalBottomSheet, skipPartiallyExpanded = true
@Composable fun OutlookChart(outlook: MonthOutlook, modifier: Modifier = Modifier) // fills width, 150.dp tall, clearAndSetSemantics {}
```

Chart geometry (spec §5.4): x for day d (0…daysInMonth) = `d / daysInMonth * width`; y for amount a = `height - a / chartMax * height` (guard `chartMax == 0` → flat baseline only). Spend polyline points: (0,0) then (d, runningTotal(d)) for d = 1…daysElapsed. Pace line from (daysElapsed, spentSoFar) to (daysInMonth, projected), skipped when `projected == null`. Budget line + "Budget ₱X" label (`TextMeasurer`, right-aligned just above the line's end, clamped inside the canvas) only when `hasBudgets`.

Strings (exact copy): `outlook_sheet_title` "%1$s outlook", `outlook_spent_so_far` "Spent so far", `outlook_days_count` "%1$d days" (use a `plurals` with one = "%1$d day"), `outlook_projected` "Projected", `outlook_by_date` "by %1$s", `outlook_too_early_to_project` "Too early to project", `outlook_budget_label` "Budget %1$s", `outlook_avg_day` "avg/day", `outlook_highest_on` "highest · %1$s", `outlook_highest` "highest", `outlook_days_left` "days left". Dates `"d MMM"`.

- [ ] **Step 1: Write `@Preview`s** (dark + light) for `OutlookSheet` content and `OutlookChart`: mockup month (expect the pace line ending well under the labelled budget line), overspent with high pace (Y scale fits projected), no budgets (no budget line), day 2 (no pace line, "—" + "Too early to project"), nothing spent (highest "—").
- [ ] **Step 2: Implement** `OutlookChart` and `OutlookSheet` per spec §5.4; in `BudgetRoute`, show `OutlookSheet` while `showOutlookSheet` is true, `onDismiss` sets it false; opening it clears `selectedDay`.
- [ ] **Step 3: Verify** — `./gradlew testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL
- [ ] **Step 4: Commit** — `feat(F14): outlook bottom sheet with labelled budget chart`

Device check (user): spec §7 device list.

---

### Task 6: Replace the day popover with an expanding card (revised design, 2026-11-26)

Spec §5.2 (revised TalkBack + amount-row-only click) and §5.3 (rewritten) are the requirements; read both. Tasks 1–5 are done.

**Files:**
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/components/BudgetHeatGridCard.kt` (remove TooltipBox/RichTooltip, the `openedOnDown` pointer-input guard and the date-scoped dismiss plumbing; add the details section)
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/screen/budget/BudgetRoute.kt` (drop the list-scroll clearing; keep month-change and sheet-open clearing)
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `MonthOutlook`, `OutlookDay`, `DayKind`, `toWholePeso()` (Task 2).
- Produces:

```kotlin
@Composable fun BudgetHeatGridCard(
    outlook: MonthOutlook,
    selectedDay: LocalDate?,
    onDayClick: (LocalDate) -> Unit,   // Route toggles: same day → null, else that day
    onCollapse: () -> Unit,            // ⌃ button → Route sets null
    onOpenOutlook: () -> Unit,         // amount row only → Route opens the sheet
    modifier: Modifier = Modifier,
)
```

Notch geometry: the notch's centre x = the selected column's centre within the grid (column = `(leadingBlanks + dayOfMonth - 1) % 7`; cell width = (288dp − 6×6dp) / 7 = 36dp; centre = col × 42dp + 18dp), offset by the grid's left inset inside the card content (grid is 288dp, centred). Draw the divider with `Canvas`/`Path` across the full content width.

Strings: remove the now-unused popover strings (`outlook_day_cd`, `outlook_day_nothing_cd`, `outlook_spent` if unused). Add (exact copy): `outlook_cell_cd` "%1$s, %2$s", `outlook_cell_no_spend_cd` "%1$s, no spend", `outlook_cell_upcoming_cd` "%1$s, upcoming", `outlook_cell_today_suffix` ", today", `outlook_cell_selected_suffix` ", selected", `outlook_collapse_day` "Collapse day details". Cell date in TalkBack: pattern `"d MMMM"` ("18 November"). Details date: `"EEE, d MMM"`. Keep `outlook_running_total`, `outlook_projected_month`, `outlook_too_early`.

- [ ] **Step 1: Update `@Preview`s** (dark + light): mockup month collapsed; expanded on 18 Nov (notch centred, middle column); expanded on a Sunday-column day and a Saturday-column day (notch at the edges); expanded on day 2 ("Too early"); no budgets.
- [ ] **Step 2: Implement** per spec §5.2–5.3; selected cell gets `selected = true` semantics; only the amount row is clickable (role button) for the sheet.
- [ ] **Step 3: Verify** — `./gradlew testDebugUnitTest assembleDebug` → BUILD SUCCESSFUL; `grep -rn "TooltipBox\|RichTooltip\|openedOnDown\|isScrollInProgress" app/src/main/java/com/luna/budgetapp/presentation/screen/budget` → no matches.
- [ ] **Step 4: Commit** — `feat(F14): day details expand the card instead of a popover`
