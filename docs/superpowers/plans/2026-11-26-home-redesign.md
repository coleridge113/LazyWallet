# F13 Home Redesign Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the Home (ExpensePreset) screen with the accepted "Alt B" design. Its parts:
- a hero row with today's total and a 7-day sparkline;
- a "Last logged" strip with Undo;
- a 3-column grid of category-coloured tiles plus a "New preset" tile;
- a Recently added bottom sheet;
- a "Removed · Undo" snackbar, with no FAB.

**Architecture:**
- All state stays in `ExpensePresetViewModel`: one `uiState` built with `combine` and input through `onEvent`.
- Home stops reading the category filter.
- The sparkline reuses `ObserveSpendingHistoryUseCase` (DAY granularity, all categories). Small pure helpers in a new `HomeSummary.kt` keep the logic unit-testable.
- The UI is new small composables under `expensepreset/components/`.
- The snackbar is state (`userMessage`) that the Route shows and acknowledges.

**Tech Stack:** Kotlin, Jetpack Compose + Material 3 (`ModalBottomSheet`, `SnackbarHost`, `DropdownMenu`), Koin, coroutines/Flow. Tests use JUnit4, Truth, MockK, coroutines-test and the hand-written fakes.

**Spec:** `docs/superpowers/specs/2026-11-26-home-redesign-design.md` (read it; values below are copied from it). Mockups: `docs/screens/redesign/expensepreset/` (git-ignored, local; unzip to a temp dir to view).

## Global Constraints

- **Paths:** all code is under `app/src/main/java/com/luna/budgetapp/`; tests are under `app/src/test/java/com/luna/budgetapp/`.
- **Money:** amounts are `Long` cents. Display with `toPeso()` (hero, strip, sheet, snackbar) or `toCurrency()` (tile amount, no ₱).
- **Home ignores the category filter.** Hero, strip, sheet and sparkline count every category.
- **"Last logged"** = the expense with the highest non-null `id` among today's expenses.
- **Undo re-adds** with the same `category`, `type`, `amount` and `date` through `AddExpenseUseCase`. The expense gets a new `id`, and the date is never clamped to today.
- **Only the latest removal can be undone.** Leaving Home drops the snackbar and its Undo.
- **Strings** go in `res/values/strings.xml`, with apostrophes escaped (`\'`).
- **Tests:**
  - ViewModel tests must `launch { viewModel.uiState.collect {} }` and then `runCurrent()` before reading `.value`.
  - Never call `advanceUntilIdle()` while `todayFlow` is collected; use `runCurrent()` / `advanceTimeBy(...)` and `job.cancel()` at the end.
- **Commits:** task-runner implementers never commit; the controller commits per task, staging only the files the task lists.
- **Category colours** go in `CategoryOptions.chartColor`:

| Category | Colour |
|---|---|
| Food | #ea6962 |
| Beverage | #7daea3 |
| Date | #d3869b |
| Commute | #e78a4e |
| Grocery | #a9b665 |
| Bills | #d8a657 |
| Fitness | #89b482 |
| House | #4c7a5d |
| Personal | #945e80 |
| Leisure | #45707a |
| Others | #928374 |

## Review Focus

1. **Expenses with a null `id`.** Older and fake data can lack an id. Last-logged must skip them and never crash (test in Task 1).
2. **Removing today's only expense.** The strip must disappear (`lastLogged == null`) and the sheet must close itself (tests in Task 1 and Task 2; UI in Task 4).
3. **Two removals in a row.** Snackbar Undo restores only the second one; the first stays deleted (test in Task 2).
4. **Midnight.** The sparkline window moves by one day, and yesterday's expenses drop out of the strip and the sheet (test in Task 1).
5. **A brand-new user with fewer than 7 days of history, or all zeros.** The sparkline still has 7 points, left-padded with 0, and draws a flat line (test in Task 1).

---

### Task 1: ViewModel data for the hero, sparkline, strip and sheet (Home ignores the filter)

**Files:**
- Create: `presentation/screen/expensepreset/model/HomeSummary.kt`
- Modify: `presentation/screen/expensepreset/ExpensePresetStateEvents.kt`
- Modify: `presentation/screen/expensepreset/ExpensePresetViewModel.kt`
- Modify (test fake): `app/src/test/java/com/luna/budgetapp/domain/repository/FakeExpenseRepository.kt`. `addExpense` gives an expense with `id == null` the id `(max existing id ?: 0) + 1`, so ids behave like Room's auto-generated ones.
- Test: `app/src/test/java/com/luna/budgetapp/presentation/screen/expensepreset/HomeSummaryTest.kt` (new)
- Test: `app/src/test/java/com/luna/budgetapp/presentation/screen/expensepreset/ExpensePresetViewModelTest.kt`

**Interfaces:**
- Produces (`HomeSummary.kt`, package `...presentation.screen.expensepreset.model`):
  - `fun lastSevenDays(periods: List<PeriodTotal>): List<Long>` returns exactly 7 totals, oldest first, ending with the last period. If there are fewer than 7 periods it left-pads with `0L`.
  - `fun lastLogged(todayExpenses: List<Expense>): Expense?` returns the item with the highest non-null `id`, or null.
  - `fun recentFirst(todayExpenses: List<Expense>): List<Expense>` sorts by `id` descending, with null ids last.
  - `sealed interface RelativeTime { data object JustNow; data class Minutes(val n: Long); data class Hours(val n: Long); data object None }`
  - `fun relativeTime(added: LocalDateTime, now: LocalDateTime): RelativeTime` returns:
    - `JustNow` under 60 s (including future times);
    - `Minutes` under 60 min;
    - `Hours` under 24 h;
    - otherwise `None`.
- Produces (`ExpensesState`): new fields
  - `sparkline: List<Long> = List(7) { 0L }`
  - `recent: List<Expense> = emptyList()`
  - `lastLogged: Expense? = null`

  `totalAmount` stays as `expenses.sumOf { it.amount }`.
- Produces: `UiState.Success` loses `categoryProfileState` and `CategoryProfileState` is deleted. `DateState` stays.
- Consumes:
  - `expenseUseCases.getExpensesByDateRange(categories = null, start, end)`: `null` means unfiltered.
  - `expenseUseCases.observeSpendingHistory(Category.entries.map { it.name }, Granularity.DAY)`, which emits `SpendingHistory(today, periods)`.

- [ ] **Step 1: Write the failing pure-helper tests in `HomeSummaryTest`**

  - **`lastSevenDays pads a short history with zeros`**
    - Input: periods with totals `[500, 0, 300]`.
    - Expected: `[0, 0, 0, 0, 500, 0, 300]`.
  - **`lastSevenDays keeps only the newest seven`**
    - Input: 10 periods with totals `1..10`.
    - Expected: `[4, 5, 6, 7, 8, 9, 10]`.
  - **`lastSevenDays of empty is seven zeros`**
  - **`lastLogged picks the highest id and skips null ids`**
    - Input: ids `[3, null, 7, 5]`.
    - Expected: id `7`.
    - Also: an all-null list gives `null`.
  - **`recentFirst orders by id descending`**
    - Input: ids `[3, 7, 5]`.
    - Expected: `[7, 5, 3]`.
  - **`relativeTime buckets`**

| Gap from `now` | Expected |
|---|---|
| 30 s | `JustNow` |
| 59 s | `JustNow` |
| 60 s | `Minutes(1)` |
| 59 min | `Minutes(59)` |
| 60 min | `Hours(1)` |
| 23 h | `Hours(23)` |
| 24 h | `None` |
| `added` 10 s after `now` | `JustNow` |

- [ ] **Step 2: Run them; expect compile failure**

Run: `./gradlew testDebugUnitTest --tests "*HomeSummaryTest"`
Expected: FAIL (unresolved references).

- [ ] **Step 3: Implement `HomeSummary.kt` per the Interfaces block**

- [ ] **Step 4: Write the failing ViewModel tests in `ExpensePresetViewModelTest`**

  Setup changes:
  - Build `observeSpendingHistory = ObserveSpendingHistoryUseCase(fakeExpenseRepo, clock)` instead of `mockk()`.
  - Fix `clock` to a known instant, e.g. 2026-11-26T18:05 in the system zone, so "today" is deterministic.

  Add these tests:
  - **`home total counts categories the active filter excludes`**
    - Make the active profile exclude FOOD; reuse the profile setup pattern from `ExpenseListViewModelTest`.
    - Add a FOOD expense today.
    - Expect `totalAmount` to include it and `lastLogged` to be it.
  - **`lastLogged is today's highest id and ignores yesterday`**
    - Add yesterday's expense, then two of today's.
    - Expect the second of today's.
    - Expect `recent` to hold only today's, in id order descending.
  - **`lastLogged is null when today has no expenses`**
  - **`sparkline has seven days ending today`**
    - Add 600 two days ago and 450 today.
    - Expect `sparkline == [0, 0, 0, 0, 600, 0, 450]`.
  - **`sparkline and strip roll over at midnight`**
    - Same data.
    - Advance the `MutableClock` past midnight and `advanceTimeBy` past the ticker.
    - Expect `sparkline` to end `[600, 0, 450, 0]`.
    - Expect `lastLogged == null` and `recent` empty.
  - Keep the existing tests passing, except `invoking the undo button deletes the latest expense`, which Task 2 replaces. Leave it untouched here.

- [ ] **Step 5: Run; expect failures**

Run: `./gradlew testDebugUnitTest --tests "*ExpensePresetViewModelTest"`
Expected: the new tests FAIL; the existing ones still PASS.

- [ ] **Step 6: Change the ViewModel and the state**

  - **Drop the filter:**
    - Delete `_categoryProfileState` and its use in `combine`.
    - Call `getExpensesByDateRange(categories = null, ...)`.
    - Keep `.onStart { profileUseCases.initializeCategoryProfile() }`.
  - **Sparkline:** combine in `observeSpendingHistory(allCategoryNames, Granularity.DAY).map { lastSevenDays(it.periods) }`.
  - **Today's data:**
    - `expenses` is already today's list (Daily filter).
    - Set `recent = recentFirst(expenses)` and `lastLogged = lastLogged(expenses)`.
  - **Error handling:** any new upstream flow must sit inside the existing `_retryTrigger.flatMapLatest { ... .catch {} }`, so errors still reach `UiState.Error`.
  - **Constructor:** keep the parameters unchanged, so `appModule.kt` needs no edit.

- [ ] **Step 7: Run the whole unit-test suite**

Run: `./gradlew testDebugUnitTest`
Expected: PASS. Fix the compile errors from removing `categoryProfileState` (Route and previews) by deleting those references.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/luna/budgetapp/presentation/screen/expensepreset/ app/src/test/java/com/luna/budgetapp/
git commit -m "feat(F13): Home state for sparkline, last-logged and recent; Home ignores the category filter"
```

---

### Task 2: Removal, snackbar message and Undo in the ViewModel

**Files:**
- Modify: `presentation/screen/expensepreset/ExpensePresetStateEvents.kt`
- Modify: `presentation/screen/expensepreset/ExpensePresetViewModel.kt`
- Modify: `presentation/screen/expensepreset/ExpensePresetRoute.kt` (only to delete the code that the removed events and dialog break; the UI rebuild is Task 3)
- Test: `ExpensePresetViewModelTest.kt`

**Interfaces:**
- Consumes: `ExpensesState.lastLogged` and `recent` (Task 1); `expenseUseCases.deleteExpense(id: Long)`; `expenseUseCases.addExpense(category: String, type: String, amount: Long, date: LocalDateTime)`.
- Produces:
  - `UiState.Success.userMessage: UserMessage? = null`
  - `sealed interface UserMessage { data class Removed(val expenseId: Long, val type: String, val amount: Long) : UserMessage }` (the id makes two removals of identical expenses distinct, so the snackbar re-fires)
  - `Event.RemoveExpense(val expense: Expense)`: used by both the strip's Undo and the sheet's remove button.
  - `Event.UndoRemove`: no payload; re-adds the VM-private `lastRemoved: Expense?` and clears it and `userMessage`.
  - `Event.UserMessageShown`: clears `userMessage` but keeps `lastRemoved` until the next removal, so an Undo tapped while the snackbar is still showing works.
- Removes: `Event.DeleteLatestExpense`, `Event.ShowDeleteConfirmationDialog`, `DialogState.ConfirmDeleteExpense` and the `deleteLatestExpense()` VM function. `DeleteLatestExpenseUseCase` stays in `ExpenseUseCases`, unused by Home.

- [ ] **Step 1: Write the failing tests (replace `invoking the undo button deletes the latest expense`)**

  - **`removing an expense deletes exactly that id and posts a Removed message`**
    - Today has ids 1 (Lunch 25000) and 2 (Jeepney 6000).
    - Send `RemoveExpense(expense 1)`.
    - Expect the repository to still hold id 2 and not id 1.
    - Expect `userMessage == Removed(1, "Lunch", 25000)`.
  - **`undo re-adds the removed expense with the same fields`**
    - Remove Jeepney (date 2026-11-26T18:05), then send `UndoRemove`.
    - Expect a today expense with category COMMUTE, type "Jeepney", amount 6000 and that exact date.
    - Expect `userMessage == null`.
  - **`undo after two removals restores only the second`**
    - Remove A, then B, then send `UndoRemove`.
    - Expect B back and A still gone.
  - **`undo after the message was shown still restores`**
    - Remove, send `UserMessageShown`, then `UndoRemove`.
    - Expect the expense back.
  - **`undo with nothing removed does nothing`**
    - Send `UndoRemove` on a fresh VM.
    - Expect the expense list unchanged and no crash.
  - **`removing the only expense of today clears lastLogged`**

- [ ] **Step 2: Run; expect failures**

Run: `./gradlew testDebugUnitTest --tests "*ExpensePresetViewModelTest"`
Expected: the new tests FAIL (unresolved `RemoveExpense`, ...).

- [ ] **Step 3: Implement the events, `userMessage` and the private `lastRemoved`**

  Add a `_userMessage: MutableStateFlow<UserMessage?>` to the success `combine`. Typed `combine` takes at most 5 flows, so group them, for example combine `_dialogState` and `_userMessage` into a pair first.
  - **`RemoveExpense`:** set `lastRemoved`, call `deleteExpense(id)` in `viewModelScope`, then set `_userMessage`. Ignore an expense whose `id` is null.

- [ ] **Step 4: Run the whole suite**

Run: `./gradlew testDebugUnitTest`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/luna/budgetapp/presentation/screen/expensepreset/ app/src/test/java/com/luna/budgetapp/presentation/screen/expensepreset/ExpensePresetViewModelTest.kt
git commit -m "feat(F13): remove expense with Removed message and snackbar undo; drop delete-latest"
```

---

### Task 3: New Home layout: hero, sparkline, strip, category tiles, New preset tile, no FAB

**Files:**
- Modify: `ui/theme/Color.kt` (the colour table in Global Constraints: retarget `BillsChartColor`, `FitnessChartColor`, `HouseChartColor`, `PersonalChartColor`, `LeisureChartColor`)
- Create: `presentation/screen/expensepreset/components/HomeHero.kt`, which holds `HomeHero(total: Long, sparkline: List<Long>, modifier: Modifier = Modifier)` and a private `Sparkline` (Canvas)
- Create: `presentation/screen/expensepreset/components/LastLoggedStrip.kt`, which holds `LastLoggedStrip(expense: Expense, now: LocalDateTime, onOpen: () -> Unit, onUndo: () -> Unit, modifier: Modifier = Modifier)`
- Create: `presentation/screen/expensepreset/components/PresetTileGrid.kt`, which holds:
  - `PresetTileGrid(presets: List<ExpensePreset>, onLog: (ExpensePreset) -> Unit, onCustom: (ExpensePreset) -> Unit, onEdit: (ExpensePreset) -> Unit, onDelete: (Long) -> Unit, onNewPreset: () -> Unit, modifier: Modifier = Modifier)`
  - `PresetTile`
  - `NewPresetTile`
- Create: `presentation/screen/expensepreset/components/RelativeTimeText.kt`, which holds `@Composable fun RelativeTime.label(): String?` (null for `None`)
- Modify: `presentation/screen/expensepreset/ExpensePresetRoute.kt`
- Delete: `presentation/screen/expensepreset/components/ExpensePresetTable.kt` and `ExpenseAmountDisplay.kt`, if nothing else references them (grep first)
- Modify: `res/values/strings.xml`

**Interfaces:**
- Consumes: Task 1 (`sparkline`, `lastLogged`, `RelativeTime`, `relativeTime`) and Task 2 (`Event.RemoveExpense`).
- Category colour: `CategoryOptions.valueOf(category.name).chartColor`; reuse a helper if `presentation/model/CategoryUi.kt` has one.
- Icon: `getIconForCategory(category)`.
- `now` for the strip: the Route keeps a `LocalDateTime` that it refreshes every 30 s with `LaunchedEffect` + `delay`, and passes it down. The ViewModel stays clock-free for the display.

- [ ] **Step 1: Add the strings**

  | Key | Value |
  |---|---|
  | `home_today` | `Today` |
  | `home_new_preset` | `New preset` |
  | `home_undo` | `Undo` |
  | `home_just_now` | `just now` |
  | `home_minutes_ago` | `%1$d min ago` |
  | `home_hours_ago` | `%1$d h ago` |
  | `cd_home_today_total` | `Today, %1$s` |
  | `cd_log_preset` | `Log %1$s, %2$s` |
  | `cd_custom_preset` | `Add custom %1$s expense` |
  | `cd_edit_or_delete` | `Edit or delete` |
  | `cd_last_added` | `Last added: %1$s %2$s, %3$s. Show recently added` |
  | `cd_undo_expense` | `Undo %1$s %2$s` |

  Reuse `btn_edit` and `btn_delete` for the menu.

- [ ] **Step 2: Build the components to the mockup values (dark theme, 412dp)**

  - **Hero:**
    - 8dp horizontal padding, 20dp bottom margin.
    - "Today" in labelLarge, onSurfaceVariant; the total at 28sp bold.
    - Semantics merged: `cd_home_today_total`.
    - Sparkline:
      - 120×40dp, inset 4dp, primary colour;
      - 2dp stroke with round join and cap;
      - a 3.5dp-radius dot on the last point;
      - scaled to the max of the 7 values, with a flat line at the bottom when the max is 0;
      - no semantics.
  - **Strip:**
    - Container: 52dp tall, 24dp corners, primary at 10%, 8dp bottom margin.
    - Body (clickable, `cd_last_added`):
      - a 32dp circle in the category colour at 35%, holding an 18dp icon in the category colour;
      - "<type> <toPeso>" in bodyLarge;
      - "· <relative label>" in bodyMedium, onSurfaceVariant, omitted when the label is null;
      - a spacer, then `Icons.Default.ExpandLess` in onSurfaceVariant.
    - "Undo": a `TextButton` in primary, 6dp end margin, `cd_undo_expense`.
  - **Grid:**
    - `LazyVerticalGrid(GridCells.Fixed(3))`, 8dp gaps both ways; each tile `aspectRatio(1f)`.
    - Tile:
      - 20dp corners, background = category colour at 18%, 12dp padding;
      - top left: a 36dp icon circle (category colour at 35%, 20dp icon in the category colour). It is its own clickable → `onCustom`, with `cd_custom_preset`;
      - bottom left: the type in labelLarge, then `toCurrency()` in bodySmall, onSurfaceVariant. Both are single-line with ellipsis.
    - The tile body uses `combinedClickable(onClick = onLog, onLongClick = open menu, onLongClickLabel = cd_edit_or_delete)`, with `cd_log_preset` on the tile.
    - The long-press menu is a `DropdownMenu` anchored to the tile: Edit → `onEdit`, Delete → `onDelete(id)`.
    - The last item is always `NewPresetTile`: 1.5dp dashed onSurfaceVariant border (drawn with `drawBehind` + `PathEffect.dashPathEffect`), 20dp corners, an `add` icon and "New preset" in labelLarge, onSurfaceVariant, 6dp gap → `onNewPreset`.
  - Add a dark `@Preview` per component, using the spec's sample data (Coffee 14000 BEVERAGE, Lunch 25000 FOOD, Jeepney 6000 COMMUTE, ...).

- [ ] **Step 3: Rebuild `ExpensePresetContent`**

  - Column order, with 16dp horizontal padding:
    - the existing TopAppBar (Sign-out only);
    - `HomeHero`;
    - `LastLoggedStrip`, only when `lastLogged != null`. `onOpen` is a no-op stub until Task 4. `onUndo` sends `RemoveExpense(lastLogged)`;
    - `PresetTileGrid`, which takes the remaining height.
  - Wire the grid callbacks:

| Callback | Event |
|---|---|
| `onLog` | `AddExpense` |
| `onCustom` | `AddExpensePreset(it, CUSTOM)` |
| `onEdit` | `AddExpensePreset(it, EDIT)` |
| `onDelete` | `ShowConfirmationDialog(id)` |
| `onNewPreset` | `AddExpensePreset(null, ADD)` |

  - Delete the FAB `Box`, `isMenuExpanded` and the `ConfirmDeleteExpense` dialog branch.
  - Keep the other dialogs.
  - Update the previews.

- [ ] **Step 4: Verify it builds and the tests pass**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL. Check that the Expenses donut file (`expenselist/components/ExpenseChart.kt`) needed no change; it reads `chartColor`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/luna/budgetapp/ui/theme/Color.kt app/src/main/java/com/luna/budgetapp/presentation/screen/expensepreset/ app/src/main/res/values/strings.xml
git commit -m "feat(F13): Home hero with sparkline, last-logged strip, category tile grid; FAB removed"
```

---

### Task 4: Recently added sheet, snackbar host and "See all expenses"

**Files:**
- Create: `presentation/screen/expensepreset/components/RecentlyAddedSheet.kt`, which holds:
  - `RecentlyAddedSheet(recent: List<Expense>, total: Long, now: LocalDateTime, onRemove: (Expense) -> Unit, onSeeAll: () -> Unit, onDismiss: () -> Unit)`
  - a private `RecentRow`
- Modify: `presentation/screen/expensepreset/ExpensePresetRoute.kt`
- Modify: `presentation/nav/NavGraph.kt`. Add an `onOpenExpenses` lambda to `ExpensePresetRoute`. It navigates to `Routes.ExpensesRoute` with the same `navigate { popUpTo<Routes.ExpensePresetRoute> { saveState = true }; launchSingleTop = true; restoreState = true }` block the bottom bar uses; extract that block into a local function and call it from both places.
- Modify: `res/values/strings.xml`

**Interfaces:**
- Consumes: `ExpensesState.recent`, `totalAmount` and `lastLogged`; `UiState.Success.userMessage` / `UserMessage.Removed`; `Event.RemoveExpense`, `Event.UndoRemove` and `Event.UserMessageShown` (Tasks 1–2); `RelativeTime.label()` (Task 3).
- New Route signature: `ExpensePresetRoute(onSignedOut: () -> Unit, onOpenExpenses: () -> Unit, viewModel: ExpensePresetViewModel = koinViewModel())`.

- [ ] **Step 1: Add the strings**

  | Key | Value |
  |---|---|
  | `home_recently_added` | `Recently added` |
  | `home_today_total` | `Today · %1$s` |
  | `home_see_all_expenses` | `See all expenses` |
  | `home_removed` | `Removed %1$s %2$s` |
  | `cd_remove_expense` | `Remove %1$s %2$s` |

  Reuse `home_undo` for the snackbar action.

- [ ] **Step 2: Build `RecentlyAddedSheet` to the mockup**

  - **Container:**
    - `ModalBottomSheet` with the default drag handle and scrim;
    - `containerColor` = primary at 11% composited over surface;
    - 28dp top corners (the M3 default);
    - content padding 0/24/24dp.
  - **Header** (16dp top): "Recently added" in titleLarge, with `home_today_total` in bodyMedium, onSurfaceVariant, aligned to its baseline at the end.
  - **List** (12dp top, −12dp horizontal so rows bleed):
    - Each row: padding 8/4/8/12dp; a 40dp circle (category colour at 35%) holding a 20dp icon; 12dp gap.
    - Then a column: the type in bodyLarge, then the time in bodySmall, onSurfaceVariant. The time format is `h:mm a`; on the first row only, add " · <relative label>" when the label is non-null.
    - Then the `toPeso()` amount in bodyLarge, then an `IconButton` (48dp) with `Icons.AutoMirrored.Filled.Undo` tinted `error` and `cd_remove_expense`.
    - The first row has a primary-10% background with 12dp corners.
    - Rows are separated by 1dp dividers (`outlineVariant`) with a 12dp horizontal inset.
  - **Footer** at the bottom end: a `TextButton` "See all expenses" with a trailing 18dp `ArrowForward` → `onSeeAll`.
  - Add a dark preview with the spec's three sample rows.

- [ ] **Step 3: Wire it in the Route**

  - **Sheet state:** `var showRecent by rememberSaveable { mutableStateOf(false) }`.
    - The strip's `onOpen` sets it to true.
    - Show the sheet while `showRecent && recent.isNotEmpty()`. If `recent` becomes empty, set `showRecent = false`.
    - `onSeeAll`: hide the sheet, then call `onOpenExpenses()`.
    - `onRemove`: send `RemoveExpense(it)`.
  - **Snackbar:**
    - Remember a `SnackbarHostState` and put a `SnackbarHost` at the bottom of the Home `Box`.
    - `LaunchedEffect(userMessage)`: when `userMessage` is a `Removed`, call `showSnackbar(message = home_removed(type, amount.toPeso()), actionLabel = home_undo, duration = SnackbarDuration.Short)`.
    - When it returns, send `UndoRemove` on `ActionPerformed`, then always send `UserMessageShown`.
    - Resolve the string with `LocalContext.current.getString(...)`, or with `stringResource` read outside the effect.
  - **Nav:** pass `onOpenExpenses` from `NavGraph`.

- [ ] **Step 4: Verify**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/luna/budgetapp/presentation/screen/expensepreset/ app/src/main/java/com/luna/budgetapp/presentation/nav/NavGraph.kt app/src/main/res/values/strings.xml
git commit -m "feat(F13): Recently added sheet, Removed/Undo snackbar, See all expenses"
```

---

## After the tasks (controller)

- **Final review:** whole branch against the spec's "Done when". Then one fix wave and a re-review.
- **Device check** (user): compare the mockups in dark theme, check light theme is readable, check the largest font size (tile text ellipsizes), check TalkBack labels.
- **Tracker:** F13 → Building while the tasks run; Progress row when the build finishes.
