# F13 · ExpensePreset redesign (category tiles + Recently added sheet): design

Status: approved 2026-11-26. Branch `feat/home-redesign`.
Source designs: `docs/screens/redesign/expensepreset/` (git-ignored, local only). It holds the handoff note `expensepreset-screen-redesign-handoff.md` and two mockup zips: `-a` = `HomeAltB.dc.html` (ExpensePreset screen; the file name is the design tool's), `-b` = `HomeAltBRecentSheet.dc.html` (sheet). The mockups are reference values (sizes, colours, type), not code to copy.

## Intent

This is the second step of the screen-by-screen UI rebuild (F12 Budget went first). The user designed and accepted a new ExpensePreset screen ("Alt B · Category tiles" plus "Alt B · Recently added sheet"), and this feature builds it.

Today, the ExpensePreset screen has these problems:
- It spends a quarter of the screen on one number.
- It hides Add and Undo behind a ☰ FAB that reads like a navigation drawer.
- Undo needs a confirm dialog.

The redesign aims to:
- make logging a preset one tap on a big, colour-coded tile;
- show what was just logged, with a one-tap undo;
- give a glance at the last 7 days.

## Goals

1. **Hero row.**
   - Left: a "Today" label (labelLarge, onSurfaceVariant) over today's total (28sp bold; "₱0.00" when nothing has been logged).
   - Right: a 7-day sparkline, 120×40dp, primary colour, 2dp line, round joins and caps. It shows daily totals for today and the 6 days before, with days that have no spend counted as 0, and a 3.5dp dot on today.
2. **Last-logged strip.**
   - It sits between the hero and the grid and shows only when today has at least one expense.
   - Shape: 52dp tall, 24dp corners, primary at 10%.
   - It has two separate tap targets:
     - **Strip body:** a 32dp category-coloured icon circle, "<name> ₱<amount>", a relative time ("· just now"), and an `expand_less` icon. Tapping it opens the Recently added sheet.
     - **"Undo" text button** (primary colour): removes that expense immediately, with no dialog, and shows the snackbar (goal 6).
   - The strip shows today's most recently **added** expense: among today's expenses, the one with the highest `id`.
3. **Preset grid.**
   - A 3-column grid of square tiles with 8dp gaps, filling the width (≈118dp on a 412dp phone) and scrolling when there are many presets.
   - Each tile:
     - 20dp corners, 12dp padding, background = the category colour at 18%;
     - a 36dp icon circle (category colour at 35%, 20dp icon in the category colour) at the top left;
     - the name (labelLarge 14sp/500) and amount (bodySmall, onSurfaceVariant, `toCurrency()` with no ₱) at the bottom left.
   - Behaviours that stay the same as today:
     - tap the tile → log the preset now (`Event.AddExpense`);
     - tap the icon circle → "Add Custom Expense" dialog (unchanged).
   - Edit and Delete move from swipe to **long-press → a small menu (Edit, Delete)**. Edit opens "Edit Expense Preset" (unchanged). Delete opens the existing "Delete this item?" confirm (kept, because a preset is not one tap to recreate).
   - The last cell is always a **"New preset" tile**: 1.5dp dashed onSurfaceVariant border, `add` icon, "New preset" text. It opens "Add Expense Preset". With no presets, the grid shows only this tile.
4. **Recently added sheet** (a Material 3 `ModalBottomSheet`).
   - Opens from the strip body. Swiping down or tapping the scrim closes it (M3 default scrim).
   - Container: primary at 11% over surface, 28dp top corners, drag handle.
   - Header: "Recently added" (titleLarge) with "Today · ₱<total>" (bodyMedium, onSurfaceVariant) on the baseline at the right.
   - List: today's expenses, newest added first (`id` descending). The rows are separated by 1dp dividers. Each row has:
     - a 40dp category-coloured icon circle;
     - the name (bodyLarge);
     - the time, "6:05 PM", plus " · just now" (or the relative time) on the newest row only;
     - the amount;
     - a 48dp red (`error`) `undo` icon button.
   - The newest row has a primary-10% highlight with 12dp corners.
   - Remove button → deletes that expense and shows the snackbar. The sheet stays open, and it closes itself when the list becomes empty.
   - "See all expenses →" text button at the bottom right: closes the sheet and switches to the Expenses tab.
5. **No FAB on the ExpensePreset screen.** The ☰ FAB, its Add/Undo menu, and the "Delete the last expense?" dialog are removed. The top bar (Sign-out only) and the bottom nav are unchanged.
6. **"Removed · Undo" snackbar.**
   - Every removal from the strip or the sheet shows a snackbar "Removed <name> ₱<amount>" with an "Undo" action.
   - Undo adds the expense back with the same category, name, amount and date. It gets a new local `id` and a new Firestore `remoteId`. Nothing visible depends on either.
   - A new removal replaces the current snackbar. Only the latest removal can be undone.
7. **Category colours, used everywhere.** These go in `CategoryOptions.chartColor`, so the Expenses donut and legend change too. That is intended.

| Category | Colour | Change |
|---|---|---|
| Food | #ea6962 | same |
| Beverage | #7daea3 | same |
| Date | #d3869b | same |
| Commute | #e78a4e | same (equals primary; accepted for now) |
| Grocery | #a9b665 | same |
| Bills | #d8a657 | was #945e80 |
| Fitness | #89b482 | was #d8a657 |
| House | #4c7a5d | was #89b482 (clashed with Fitness) |
| Personal | #945e80 | was #45707a |
| Leisure | #45707a | was #4c7a5d |
| Others | #928374 | same |

## Non-goals

- The Budget redesign in the same handoff (month heat grid, outlook sheet, day popover). It becomes a separate feature, F14.
- Expenses, Analysis, Sign in, the dialogs' visual style, the palette clean-up of the M3 baseline roles, and light-theme tuning (F04).
- Reordering presets or adding new preset fields.
- Snackbars anywhere other than the ExpensePreset screen (F11 decides the app-wide pattern; this feature should not block it).

## Anti-goals

- Undo must never remove anything other than the expense the strip shows. The old `deleteLatestExpense` (`MAX(id)` over all expenses) can remove a back-dated expense that is not even in today's list, so the ExpensePreset screen must not use it any more.
- A mis-tap must not lose data silently: every removal on the ExpensePreset screen is undoable from the snackbar.
- Don't regress logging speed: tapping a tile logs at once, with no dialog and no navigation.

## Constraints and facts

- **Data: the ExpensePreset screen ignores the category filter** (user, 2026-11-26, option A). The ExpensePreset screen means "everything I logged today", so the hero total, strip, sheet and sparkline all use **every category**. The filter stays an Expenses-screen tool, and a tile you tap always shows up on the ExpensePreset screen.
  - Before this, the ExpensePreset screen's total followed the active category profile. Users with a filter set will see its total change, which is intended.
  - The ViewModel stops observing the category profile for its data: `getExpensesByDateRange` and the daily totals get all `Category` names, or an unfiltered variant if the plan finds that cleaner.
  - `CategoryProfileState` leaves the ExpensePreset `UiState`.
  - `profileUseCases.initializeCategoryProfile()` stays on start, because the Expenses screen relies on it.
- **Expense model.** `Expense` has `id`, `type`, `amount` (cents), `category`, `date` (`LocalDateTime`) and no `remoteId`. `AddExpenseUseCase(category: String, type, amount, date)` keeps the date and makes a new `remoteId`. `DeleteExpenseUseCase(id)` deletes locally and from Firestore.
- **Today rolls over at midnight.** `todayFlow(clock)` (already in the VM) drives this. See Learnings: no `advanceUntilIdle()` while it is collected.
- **One state stream.** One `uiState` built with `combine`, and input through `onEvent`. The snackbar is state, not a one-off event: `UiState.Success.userMessage` is shown by the UI and acknowledged with `Event.UserMessageShown`, per the Android UI-layer guidance on transient messages. This also fits F11.
- **Strings.** New strings go in `res/values/strings.xml`, with apostrophes escaped.
- **Money.** Amounts are formatted with `toPeso()` (strip, sheet, snackbar, hero) or `toCurrency()` (tiles).
- **Icons.** `getIconForCategory(category)` (the app's own vectors) is used for tiles, strip and sheet.
- **Tests.** ViewModel tests use the hand-written fakes (`FakeExpenseRepository` already has `getDailyTotals` and `deleteExpenseById`) and `MutableClock`.

## Approaches considered

**How Undo works**
- **Delete now, re-add on Undo (chosen).** It uses the existing delete and add paths with no new data-layer code. The cost is a new `id`/`remoteId` for the restored expense, which is invisible.
- **Hide now, delete when the snackbar times out.** Rejected. Other tabs would show the expense for a few seconds. If the ViewModel is cleared mid-timeout, the delete has to finish outside it, which is easy to get wrong.

The user picked "A: snackbar 'Removed · Undo'" over a confirm for older entries (B) or no protection (C).

**Where the state lives**
- **Extend `ExpensePresetViewModel` (chosen).** It already loads presets, today's expenses and the clock. New fields: the 7-day totals, the last-logged expense, the sheet's open state and the user message.
- **A separate "recent" ViewModel or a shared sheet component.** Rejected: only the ExpensePreset screen uses it.

**Edit/Delete gesture on a 3-column grid**
- **Long-press menu (chosen, as the handoff suggests).**
- **Keep the swipe.** Rejected: a 96dp reveal doesn't fit a 118dp tile.

## Decided details (Claude's calls, confirmed by the user: "go with your proposals")

- **"Last logged"** = highest `id` among today's expenses (all categories).
- **Relative time:**
  - "just now" under 1 minute;
  - "N min ago" under 60 minutes;
  - "N h ago" under 24 hours;
  - otherwise just the clock time.
  - The strip and the newest sheet row use it.
- **Sparkline:**
  - Computed in the ViewModel from `getDailyTotals(categories)`. It keeps the last 7 days ending today and fills missing days with 0.
  - It is scaled to the max of those 7 days, with a flat line at the bottom when every day is 0.
  - The dot is always on today. It is decorative: the hero's semantics read only "Today, ₱450.00".
- **Snackbar data** (Claude's call, after the builder check):
  - The ViewModel keeps the removed expense in a private `lastRemoved` field.
  - `UiState.Success.userMessage` carries only what the snackbar shows: name, amount and an action flag.
  - `Event.UndoRemove` has no payload; it re-adds `lastRemoved` and clears it.
  - A second removal overwrites `lastRemoved`, so only the latest can be undone.
  - The Route calls `Event.UserMessageShown` when `showSnackbar` returns, whether it was dismissed or acted on.
  - If the user leaves the ExpensePreset screen, the snackbar goes with it and that Undo is lost (accepted).
  - Opening or closing the Recently added sheet dismisses the current snackbar, and that Undo is lost (user, 2026-11-26, after device testing). A removal made inside the open sheet shows its snackbar in the sheet.
- **Undo across midnight** (Claude's call): Undo restores the original date without clamping. A removal undone after midnight comes back as yesterday's expense and leaves the ExpensePreset screen's "today" views (accepted).
- **Day boundaries** (Claude's call): days in the sparkline are local calendar dates, the same as `ObserveSpendingHistoryUseCase`. The ViewModel reads the repository's daily totals through a small new use case (or the existing one with `Granularity.DAY`, whichever the plan finds simpler) and windows them on `todayFlow(clock)`.
- **Date filter** (answered by the code): the ExpensePreset `DateState` is always `DateFilter.Daily` and the screen has no date-filter control, so "today's expenses" = the existing `expenses` list.
- **Light theme** (Claude's call; tuning stays with F04): dark theme is the design target. The category colours stay fixed in both themes. In light theme they only need to be legible, and the builder may raise the 18% / 35% tints there if they are not.
- **"See all expenses"** switches to the Expenses tab the same way the bottom bar does (a new `onOpenExpenses` callback from `NavGraph` into `ExpensePresetRoute`).
- **The snackbar** lives on the ExpensePreset screen: a `SnackbarHostState` in the Route, anchored at the bottom of the ExpensePreset content, above the bottom bar.
- **Removed code:**
  - `DialogState.ConfirmDeleteExpense`;
  - `Event.ShowDeleteConfirmationDialog`;
  - `Event.DeleteLatestExpense`;
  - the FAB code;
  - `SwipeableTableItem` usage on the ExpensePreset screen (the component stays, because Expenses uses it);
  - `ExpenseAmountDisplay`, if nothing else uses it.

  `DeleteLatestExpenseUseCase` stays in the codebase but is no longer used by the ExpensePreset screen.

## Accessibility

- **Tile:** one merged node: "Log <name>, ₱<amount>" (click). The icon circle is its own button, "Add custom <name> expense". The long-press label is "Edit or delete".
- **Strip:** body = "Last added: <name> ₱<amount>, <relative time>. Show recently added". Undo = "Undo <name> ₱<amount>".
- **Sheet rows:** the remove button reads "Remove <name> ₱<amount>".
- **Sizes:** all touch targets are at least 48dp. The tile name and amount keep one line, ellipsized.

## Left to the builder

- Exact composable split and file names under `expensepreset/components/`.
- How the long-press menu is anchored (a `DropdownMenu` on the tile is fine).
- Snackbar duration (M3 default `Short` with an action is fine) and how the message string is assembled.
- Whether the sparkline is a `Canvas` or a `Path`.
- Previews for each new composable.

## Done when

- The ExpensePreset screen matches the two mockups at 412dp in dark theme: hero, strip, 3-column tiles, the "New preset" tile, and no FAB. The sheet opens from the strip.
- Behaviour:
  - tap a tile → logs;
  - tap its icon → custom dialog;
  - long-press → Edit / Delete;
  - strip Undo and sheet remove → expense gone + snackbar;
  - snackbar Undo → expense back with the same date;
  - "See all expenses" → Expenses tab.
- The strip hides when today has no expenses. Hero, strip, sheet and sparkline count every category whatever the Expenses filter is, and roll over at midnight.
- The Expenses donut uses the new category colours.
- `./gradlew testDebugUnitTest` passes, with new VM tests for:
  - the sparkline (zero-filled, 7 points, rollover);
  - last-logged selection (highest id, today only, unaffected by the category filter);
  - Undo deleting that exact id;
  - remove + snackbar Undo re-adding the same fields;
  - `UserMessageShown` clearing the message.
- `./gradlew assembleDebug` succeeds.
- Device check: dark and light theme, largest font size (tile text ellipsizes), TalkBack labels above.
