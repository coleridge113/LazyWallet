# F12 Budget outlook arc — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the text-only `BudgetOutlookCard` with a 240° gauge arc of actual vs projected monthly spend, plus a pace tick.

**Architecture:** `OutlookDetails` gains a `paceFraction` (from the BudgetViewModel's existing midnight-aware `todayFlow`) and a derived `usedFraction`. A shared `budgetProgressColor()` taken out of `BudgetProgressBar` colours both the bar and the arc. The card is a `Box` with a shapes-only `Canvas` (track, fill, tick) and ordinary `Text` on top.

**Tech Stack:** Kotlin, Jetpack Compose (Material 3), coroutines Flow; tests with JUnit4, Truth, coroutines-test, `MutableClock`, fakes.

**Spec:** `docs/superpowers/specs/2026-11-26-budget-outlook-arc-design.md` (approved 2026-11-26).

Paths are relative to `app/src/main/java/com/luna/budgetapp/` (main) and `app/src/test/java/com/luna/budgetapp/` (test).

## Global Constraints

- Colour thresholds (same as `BudgetProgressBar`): `primary` < 0.75, `tertiary` < 0.90, else `error`; track = same colour at 20% alpha; round caps; animated fill.
- Arc: start angle **150°**, sweep **240°**, stroke about **12dp**, gauge about **200dp** wide. The fill draws `min(used, 1)`. The colour uses the **unclamped** used fraction.
- Pace tick: about **2dp**, `onSurface`, at `150° + 240° × paceFraction`, extending slightly beyond both edges of the stroke. No tick when there are no budgets.
- `paceFraction = dayOfMonth / lengthOfMonth` (2026-11-26 → 26/30).
- `usedFraction = actual / projected`, or **null** when `projected == 0`.
- Strings:
  - reuse `budget_suffix_left` / `budget_suffix_overspent`;
  - add `outlook_spent_of` = "%1$s of %2$s", `outlook_no_budgets` = "No budgets yet", `outlook_cd` = "Spent %1$s of %2$s this month, %3$s %4$s";
  - remove `outlook_total_remaining`, `outlook_projected`, `outlook_actual` once unused.
- No hard-coded colours (`Color.Black`, direct `Gruvbox*`). No `drawText`. No pixel offsets. Amounts via `toPeso()`.
- Income: kept in `OutlookDetails`, not shown.
- Tests: fakes plus `MutableClock`. The suite is 100 at the start and only goes up. Commands: `./gradlew testDebugUnitTest`, `./gradlew assembleDebug`. The implementer never commits.

## Review Focus

1. **Overspent still reads as overspent.** At used 1.2 the arc is full **and** `error`-coloured, not clamped to a tamer colour. Task 1 test `usedFraction_overspent_isUnclamped`. Task 2 passes the unclamped value to `budgetProgressColor`.
2. **No budgets.** `projected == 0` → `usedFraction == null`, with no divide-by-zero and no tick. Task 1 test `usedFraction_noBudgets_isNull`. Task 2 preview `NoBudgets`.
3. **Month lengths.** Feb 2028 (leap year) → 29 days. Task 1 test `paceFraction_leapFebruary`.
4. **Midnight rollover.** Pace moves with `todayFlow`. Task 1 VM test `pace_rollsOverAtMidnight`.
5. **Large font scale.** Centre text must not overflow the arc. Task 2: centre text uses `maxLines = 1` with `autoSize` or `TextOverflow.Ellipsis`; device check at the largest font size.

---

### Task 1: Outlook fractions and the shared progress colour

**Files:**
- Modify: `presentation/screen/budget/model/OutlookDetails.kt`
- Modify: `presentation/screen/budget/BudgetViewModel.kt` (`_monthlyOutlook` also combines `_today`)
- Modify: `presentation/screen/budget/components/BudgetCard.kt` (extract the colour helper; `BudgetProgressBar` uses it)
- Create (test): `presentation/screen/budget/OutlookDetailsTest.kt`
- Modify (test): `presentation/screen/budget/BudgetViewModelTest.kt`

**Interfaces:**
- Produces:
  - `data class OutlookDetails(val income: Long, val projectedSpend: Long, val actualSpend: Long, val paceFraction: Float = 0f)` with `val remaining = projectedSpend - actualSpend` (unchanged) and `val usedFraction: Float? = if (projectedSpend > 0) actualSpend.toFloat() / projectedSpend else null`.
  - `fun paceFraction(today: LocalDate): Float = today.dayOfMonth.toFloat() / today.lengthOfMonth()` (top level in `OutlookDetails.kt`).
  - `@Composable fun budgetProgressColor(progress: Float): Color` (public, in `BudgetCard.kt` next to `BudgetProgressBar`), with the thresholds from Global Constraints. `BudgetProgressBar` calls it with its **clamped** progress, so the bar's behaviour is unchanged.
  - `BudgetViewModel._monthlyOutlook = combine(_budgets, _totalAmount, _today) { … OutlookDetails(…, paceFraction = paceFraction(today)) }`.

- [ ] **Step 1: Write the failing tests**

`OutlookDetailsTest`:
- `usedFraction_underTarget` — `OutlookDetails(0, 28_000_00, 12_300_00).usedFraction` ≈ `0.4393f` (tolerance 0.0001)
- `usedFraction_atTarget` — 100 of 100 → `1f`
- `usedFraction_overspent_isUnclamped` — 120 of 100 → `1.2f`
- `usedFraction_noBudgets_isNull` — projected 0, actual 500 → `null`
- `paceFraction_firstOfMonth` — 2026-11-01 → `1f/30`
- `paceFraction_midMonth` — 2026-11-15 → `0.5f`
- `paceFraction_lastDay` — 2026-11-30 → `1f`
- `paceFraction_leapFebruary` — 2028-02-29 → `1f`; 2028-02-15 → `15f/29`

`BudgetViewModelTest` (existing clock `2026-03-31T23:59:00Z`, UTC):
- `pace_rollsOverAtMidnight` — `monthlyOutlook.paceFraction == 1f` (31/31); after `clock.current = Instant.parse("2026-04-01T00:00:30Z")` and `advanceTimeBy(61_000)` it is `1f/30`. Mirror the existing `monthly actual spend rolls over at midnight` test.

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*OutlookDetailsTest" --tests "*BudgetViewModelTest"` — Expected: FAIL (unresolved `usedFraction` / `paceFraction`).

- [ ] **Step 3: Implement per the Interfaces block**

- [ ] **Step 4: Run the full suite and the build**

Run: `./gradlew testDebugUnitTest assembleDebug` — Expected: BUILD SUCCESSFUL, all pass (≥ 109).

- [ ] **Step 5: Report changed files; controller commits** `feat(F12): outlook used and pace fractions; shared budget progress colour`

---

### Task 2: The outlook arc card

**Files:**
- Modify: `presentation/screen/budget/components/BudgetOutlookCard.kt` (rewrite)
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes (Task 1): `OutlookDetails.usedFraction`, `.paceFraction`, `.remaining`, `.actualSpend`, `.projectedSpend`; `budgetProgressColor(progress: Float)`.
- Produces: `@Composable fun BudgetOutlookCard(modifier: Modifier = Modifier, details: OutlookDetails)` (same signature, so `BudgetRoute` is unchanged).
  - Layout: `Surface`, as BudgetCard does (`MaterialTheme.shapes.large`, `primary.copy(alpha = 0.1f)` container to match the cards) → `Column(center)`:
    - `Box(Modifier.size(~200dp).aspectRatio(1f))` holding the gauge `Canvas` and the centre `Column`, which has the remaining amount (`titleLarge` or `headlineSmall`, `maxLines = 1`) and the suffix (`bodyMedium`). For no budgets, it shows `outlook_no_budgets` instead.
    - Below the box (pulled up into the open gap of the arc if it fits): `stringResource(R.string.outlook_spent_of, actual.toPeso(), projected.toPeso())`, `bodyMedium`. Hidden for no budgets.
  - `Canvas`: `drawArc` track (colour at 0.2 alpha) at 150°/240°, then the fill sweeping `240° × min(used, 1)` (`animateFloatAsState`), with `Stroke(width, cap = StrokeCap.Round)`. Then the pace tick as a `drawLine` along the radius at angle `150° + 240° × pace`, from `r - stroke` to `r + stroke` (radians via `Math.toRadians`), `onSurface`, `2.dp.toPx()`, round cap. The colour is `budgetProgressColor(used ?: 0f)` with the **unclamped** value. With no budgets, the track uses `onSurfaceVariant` at 0.2 and there is no fill or tick.
  - Semantics: the card's outer node has `clearAndSetSemantics { contentDescription = stringResource(outlook_cd, actual, projected, abs(remaining).toPeso(), suffix) }` (resolve the strings before the semantics block). For no budgets, use `outlook_no_budgets`.
  - Previews, in light and dark: `UnderPace` (12_300_00 of 28_000_00, pace 0.867), `OverPace` (22_000_00 of 28_000_00, pace 0.4), `Overspent` (31_000_00 of 28_000_00, pace 0.9), `NoBudgets` (0 of 0).
  - Strings: add the three from Global Constraints; remove `outlook_total_remaining`, `outlook_projected`, `outlook_actual` (grep first).

- [ ] **Step 1: Implement per the Interfaces block (UI only; there is no unit-testable logic left after Task 1)**

- [ ] **Step 2: Verify**

Run: `./gradlew testDebugUnitTest assembleDebug` — Expected: BUILD SUCCESSFUL, all pass.
Run: `grep -n "Color.Black\|Gruvbox\|drawText\|outlook_total_remaining" app/src/main/java/com/luna/budgetapp/presentation/screen/budget/components/BudgetOutlookCard.kt` — Expected: no output.

- [ ] **Step 3: Report changed files; controller commits** `feat(F12): Budget outlook as a gauge arc with pace tick`

---

## After the tasks (controller)

- Final whole-branch review, then tracker (F12 → Building; Features-done after merge).
- Device check for the user: compare with a BudgetCard at a similar percentage (same colour); an overspent month (full red arc, "overspent"); delete all budgets ("No budgets yet"); largest font size; TalkBack reads one sentence; light and dark.
