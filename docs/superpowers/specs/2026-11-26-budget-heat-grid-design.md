# F14 · Budget outlook: heat grid, outlook sheet, day popover — design

Date: 2026-11-26 · Feature: F14 · Branch: `feat/budget-heat-grid`

## 1. Intent

Replace the F12 gauge arc at the top of the Budget screen with a design the user accepted from a design-tool handoff (git-ignored, local only: `docs/screens/redesign/budget/*.zip`):

- **Alt B · Month heat grid**: the top card shows "₱X left this month" over a calendar grid of the current month, where each day's colour strength shows how much was spent.
- **Dialog 2 · Bottom sheet with chart**: tapping the card opens a "<Month> outlook" sheet with spent-so-far, a pace projection, a cumulative-spend chart against the budget, and three quick stats.
- **Dialog 3 · Day tap popover**: tapping a day shows that day's spend, the running total and the projected month.

Why: the gauge says "how much is left" but not *when* the money went. The grid shows the month's rhythm at a glance; the sheet answers "where will I end up?"; the popover answers "what happened that day?".

## 2. Goals, non-goals, anti-goals

Goals
- Faithful to the mockups' values (sizes, radii, colours, type) using the app's theme roles.
- All figures count **all spending** in every category (user choice A), matching the hero's existing "left" maths.
- The budget total is visible in the sheet: the dashed line is labelled "Budget ₱X" (user decision; the mockup left it unlabelled).
- Unit-tested calculations; TalkBack usable.

Non-goals
- No month navigation (current month only).
- No change to BudgetCards, the FAB, budget dialogs, BudgetDetails or the bottom bar (the mockup's 3-tab bar is a mockup artefact).
- No database schema changes (one new read query is added, see 5.1).

Anti-goals (a failure even if it works)
- Hero, grid, sheet and popover disagreeing about how much was spent this month.
- A projection shown on day 1 that screams "₱60,000 by month end" from one purchase.
- Heat cells so faint that a small spend day looks like a no-spend day.

## 3. Current state (what this touches)

- `BudgetRoute.kt`: a `Column` with `BudgetOutlookCard` then a `LazyColumn` of `BudgetCard`s; FAB opens the budget form.
- `BudgetViewModel.kt`: `_monthlyOutlook` = `OutlookDetails(projectedSpend = Σ budgets' monthly-projected limits, actualSpend = getTotalAmountByDateRange(month start..now) across all categories, paceFraction)`. `_today = todayFlow(clock)` rolls at midnight.
- `model/OutlookDetails.kt`: `remaining = projectedSpend - actualSpend`, `usedFraction`.
- `components/BudgetOutlookCard.kt`: the F12 gauge — **removed** by this feature (and its strings `outlook_spent_of`, `outlook_cd` if unused afterwards; `outlook_no_budgets`, `budget_suffix_left`, `budget_suffix_overspent` are reused).
- Daily totals exist only filtered by category: `ExpenseDao.getDailyTotals(categories)` (`WHERE category IN (:categories)`, `GROUP BY` local day) → `ExpenseRepository.getDailyTotals` → `observeSpendingHistory` (rolled up, stops at today). The hero's current total (`getTotalAmountByDateRange`) has **no** category filter. Expense `category` is a free string in Room, so a legacy or renamed category could exist and would drop out of a `Category.entries`-filtered sum.
- Tests: `BudgetViewModelTest`, `OutlookDetailsTest`, `FakeExpenseRepository` (already implements `getDailyTotals`), `MutableClock`.

## 4. Approaches considered (for the day popover)

| | Approach | Trade-off |
|---|---|---|
| **A (picked)** | M3 `TooltipBox` + `RichTooltip`, shown programmatically on tap | Positioning, edge flipping, caret, outside-tap dismiss and accessibility come free; caret/shadow are Material's, may differ a pixel or two from the mockup. One small `TooltipBox` per day cell. |
| B | Custom `Popup` + own `PopupPositionProvider` + drawn caret | Pixel-exact, one popup; we own positioning, edge cases, dismissal, a11y. More code and device testing. |
| C | Inline detail row under the grid | Simplest, never covers anything; not the accepted design, makes the hero taller. |

User picked **A**. Fallback (builder may take it only if A's visuals are clearly off on device, and must say so): B for visuals only, same behaviour.

Everything else has one sensible way: hand-drawn Compose (`Canvas`, like the F12 gauge) for the grid cells and chart; `ModalBottomSheet` (like F13's Recently added sheet) for the outlook; one pure calculation model (like `ExpensePresetSummary`) for all numbers.

## 5. Design

### 5.1 Data and calculations

**New read query (my call, from the builder check):** an unfiltered daily-totals query (same SQL as `getDailyTotals` without the `WHERE`, optionally bounded to a date range) exposed as a new `ExpenseRepository` method and a new use case added to `ExpenseUseCases` (+ `appModule.kt`, + `FakeExpenseRepository`). Reason: every expense counts (user choice A), exactly like today's unfiltered hero total, so hero and grid can't disagree. `observeSpendingHistory` is not used here.

New pure model in `presentation/screen/budget/model/` (name is the builder's; e.g. `MonthOutlook`), built from: `today: LocalDate`, `budgetTotal: Long` (= current `projectedSpend`), `dailyTotals: List<DailyTotal>` (raw, unfiltered, may include dates outside the month or after today; the model filters). All amounts in cents. **`today` comes from the ViewModel's `_today` only** (single clock source).

- `days`: one entry per day of `today`'s month: `date`, `spent` (0 if none), `kind` = Past / Today / Future.
- `spentSoFar` = Σ spent for days ≤ today. **Single source of truth**: the hero's "left" uses this same figure, so hero, grid, sheet and popover always agree (replaces the separate `getTotalAmountByDateRange` call for the outlook; builder's call whether to drop that flow entirely).
- `remaining` = `budgetTotal - spentSoFar` (negative ⇒ over).
- `daysElapsed` = `today.dayOfMonth`; `daysInMonth`; `daysLeft` = `daysInMonth - daysElapsed` (mockup: 26 of 30 → "4 days left").
- `projected` = `spentSoFar / daysElapsed * daysInMonth`, rounded to cents; **null when `daysElapsed < 3`** ("too early").
- `avgPerDay` = `spentSoFar / daysElapsed`.
- `highest` = the day ≤ today with max `spent` (earliest on a tie); null if nothing spent.
- `runningTotal(date)` = Σ spent from day 1 through `date`.
- `intensity(day)` for past/today days with spend: `0.38 + 0.62 * (spent / maxSpent)` (maxSpent = highest day's spend) → maxSpent day is 1.0, smallest still ≥ 0.38. Zero-spend days have no fill.
- Mockup check (Nov 2026, today = 26, budget ₱28,000, spent ₱12,300): left ₱15,700; projected ₱14,192 (12,300/26×30 = 14,192.31); avg ₱473; 4 days left.

The ViewModel combines budgets, `observeSpendingHistory(allCategoryNames, DAY)` and `_today` into this model and exposes it in `UiState.Success` (replacing or alongside `monthlyOutlook`; builder's call, keep `OutlookDetails` only if still used). The selected popover day and sheet visibility are **UI state in the Route** (`rememberSaveable`), not ViewModel state; they are transient and must not re-fire on tab return (see Learnings: tab back stack keeps VM state).

### 5.2 Hero card (replaces `BudgetOutlookCard`)

Mockup values (412dp artboard):
- Card: `primary` at 10% alpha (as BudgetCard), 16dp radius, 4dp outer margin, 16dp padding, full width.
- Row, baseline-aligned, 8dp gap: amount 36sp/44 bold ("₱15,700", whole pesos as in the mockup) + "left this month" 14sp/20, `onSurfaceVariant` (#928374).
- Over budget: amount = overspend, suffix "over this month", amount in the existing overspent colour (`budgetProgressColor` red band).
- No budgets: "No budgets yet" (existing string) in place of amount+suffix; grid still shown; the card still opens the sheet (its no-budget variant: tiles unchanged, since "Spent so far" and the pace "Projected" don't need budgets; chart drops the budget line, see 5.4).
- Whole card is clickable → opens the outlook sheet; TalkBack: "₱15,700 left this month. Opens November outlook." (role button).

Grid:
- 288dp wide, centred, 12dp below the amount row; 7 columns, 6dp gaps; first column **Sunday**.
- Weekday header: S M T W T F S, 11sp/16, letterSpacing 0.5, `onSurfaceVariant`, centred.
- Leading blank cells before day 1 (no outline); no trailing cells after the last day.
- Cell 36dp tall, 8dp radius:
  - Past/today with spend: fill `primary` (#e78a4e) at `intensity` alpha.
  - Past/today with no spend: 1dp border `onSurfaceVariant` @50%, no fill.
  - Today: additionally a 2dp border `onSurface` (#d4be98).
  - Future: 1dp **dashed** border `onSurfaceVariant`, whole cell @40% opacity, not clickable.
  - Selected (popover open): 2dp `onSurface` border (as today's).
- No day numbers in cells (mockup has none).
- TalkBack: each past/today cell focusable, `clearAndSetSemantics` with "Wed 18 Nov, ₱1,150 spent" (or "nothing spent"), role button; future cells hidden from TalkBack. Grid container gets no extra summary label (cells carry it); builder may add a heading "Daily spend, November 2026".

### 5.3 Day popover (M3 `RichTooltip`)

- Opens on tap of any past/today cell (₱0 days included). Tapping another day moves it; tapping the same day, tapping outside, or scrolling the budget list closes it (my call: observe the `LazyListState`; the hero itself doesn't scroll). Opening the sheet closes it.
- Container: surface = `primary` 11% over `surface` (#282828) — same as the sheet; 12dp radius; width 220dp; padding 14dp vertical / 16dp horizontal; caret on; shadow as Material default (mockup: `0 2px 6px rgba(0,0,0,.3)`).
- Content:
  - "Wed, 18 Nov" — 14sp/20 medium, letterSpacing 0.1.
  - Row baseline 6dp gap: "₱1,150" 22sp/28 bold + "spent" 12sp/16 `onSurfaceVariant`.
  - Divider 1dp `outlineVariant` (#49454f), 10dp vertical margin.
  - Row: "Running total" (`onSurfaceVariant`) … "₱9,100" — 14sp/20, space-between.
  - Row (4dp top): "Projected month" … "₱14,192"; when `projected == null`: "Too early".
- Placement: below the cell, caret pointing up; Material flips/clamps near edges.
- Amounts in the popover and sheet: whole pesos (mockup), via the app's peso formatter.

### 5.4 Outlook sheet (`ModalBottomSheet`)

- Opens from hero tap; closes by swipe down, scrim tap, back. Scrim black @32%.
- Container: `primary` 11% over `surface`; top corners 28dp; content padding 0 24dp 24dp; drag handle 32×4dp, 2dp radius, `onSurfaceVariant`, 16dp top. Mockup height 520dp: let content size it (`skipPartiallyExpanded = true`), vertical spacing distributed as in the mockup.
- Title: "<Month> outlook" (e.g. "November outlook") 22sp/28.
- Two tiles (12dp gap, equal width, 12dp radius, 12dp padding, `primary` 10%):
  - "Spent so far" (12sp/16 `onSurfaceVariant`) / "₱12,300" (22sp/28 bold) / "26 days".
  - "Projected" / "₱14,192" / "by 30 Nov". When too early: value "—" and caption "Too early to project".
- Chart (344×150dp in mockup; fill width, keep 150dp height), `Canvas`:
  - Y scale 0 … `max(budgetTotal, projected ?: 0, spentSoFar)` (so an over-budget pace stays on chart); X = day 1 … last day.
  - Baseline: 1dp `onSurface` @30%.
  - Budget line: dashed (5 on, 4 off) 1.5dp `onSurfaceVariant` from (day 0, 0) to (last day, budgetTotal). **Label "Budget ₱28,000"** at its end, 11sp `onSurfaceVariant`, right-aligned just above the line's end (new; not in mockup).
  - Cumulative spend: polyline 2.5dp `primary`, round joins/caps, through running totals for days 1…today (point 0 at day 0 = 0); area under it filled `primary` @15%; dot r=4dp at today.
  - Pace continuation: dotted (1 on, 5 off, round cap) 2dp `primary` from today's point to (last day, projected). Hidden when too early.
  - No budgets: no budget line or label; scale to `max(projected, spentSoFar)`.
  - X labels under the chart: "1 Nov" left, "30 Nov" right, 11sp `onSurfaceVariant`, 4dp top.
  - TalkBack: chart hidden; tiles and stats carry the content.
- Stats row (3 equal columns, 12dp gap), each: icon 20dp `primary` + value 16sp/24 medium, caption 11sp/16 `onSurfaceVariant` 2dp below:
  - `trending_flat` · "₱473" · "avg/day"
  - `arrow_upward` · "₱1,150" · "highest · 18 Nov" (hidden value "—", caption "highest" when nothing spent)
  - `event` · "4" · "days left"
  - Icons: Material Icons Extended (already a dependency): `Icons.AutoMirrored.Filled.TrendingFlat`, `Icons.Filled.ArrowUpward`, `Icons.Filled.Event`.

### 5.5 Strings

All new text in `res/values/strings.xml` with placeholders (escape apostrophes). Month/day names via `java.time` formatting with the device locale (mockup is en: "Wed, 18 Nov", "1 Nov", "November").

## 6. Constraints

- Money is `Long` cents; convert only in UI.
- `todayFlow` drives rollover; tests must use `runCurrent()`/`advanceTimeBy` (not `advanceUntilIdle`) and cancel collectors.
- The new daily-totals use case is registered in `di/appModule.kt` and added to `ExpenseUseCases`; every place that builds `ExpenseUseCases` by hand in tests must be updated.
- ModalBottomSheet is its own window; nothing here shows a snackbar, so no host juggling is needed.
- Don't name anything "Home"; don't commit `docs/screens/`.

## 7. Tests (done when)

Unit (model): mockup numbers (left, projected, avg, days left, highest); projected null for days 1–2, present from day 3; intensity bounds (max = 1.0, min spend ≥ 0.38, zero = none); running total; month with 31 and 28 days; first-of-month leading blanks for a Sunday-first grid; over budget (negative remaining); no budgets (budgetTotal 0); highest tie → earliest; future-dated and previous-month entries in the input are ignored (fed directly to the model).

Repository/DAO: the unfiltered daily-totals query includes an expense whose category isn't in `Category.entries` (fake + DAO-level test if an androidTest harness exists; otherwise fake only).

ViewModel: success state carries the model; midnight rollover moves "today" and starts a new month on the 1st; error path still yields `UiState.Error`.

Device checks (user): grid looks right in dark theme; tap a day → popover with correct figures, near-edge days (Sat/Sun columns) position sensibly; tap card → sheet with labelled budget line; overspent month; month with no spending; TalkBack reads day cells; largest font size doesn't break the hero row.

## 8. Left to the builder

- Composable/file names and splitting; whether `OutlookDetails` and `getTotalAmountByDateRange` stay.
- Exact vertical rhythm inside the sheet (match mockup spacing within reason).
- How `TooltipBox` state is held per cell (e.g., one `TooltipState` per cell vs a keyed one) as long as only one popover shows at a time.
- Removing now-unused F12 strings/previews.
