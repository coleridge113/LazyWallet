# Learnings

Gotchas not yet in the project instructions; delete them here once folded in.

- Figma Starter plan: MCP tool calls are rate-limited (hit after ~15 file calls plus screenshots), and variable collections allow only 1 mode, so Dark and Light are separate collections ("Theme Dark", "Theme Light").
- Figma: a fill opacity on a variable-bound paint is lost on component instances. Use a background rectangle with layer opacity instead (done in PresetItem, ExpenseItem, BudgetCard).
- Figma: components created with `layoutMode` default to a fixed 100px height; set `counterAxisSizingMode = "AUTO"` or items come out too tall.
- Figma: resizing an icon component instance (24px SVG) to 12px distorts the icon; build a small icon variant instead.
- Use `get_screenshot` then curl the URL to a scratchpad file and Read it to actually see a frame.
- ViewModel `uiState` uses `WhileSubscribed`, so tests must `launch { viewModel.uiState.collect {} }` before reading `.value`, otherwise it is still `UiState.Loading`.
- Form-dialog amount strings (`Event.ConfirmExpenseFormDialog`, `AddExpense` custom amount) are pesos and `parseAmountExpression` converts to cents ("10" -> 1000L). Fakes' category filters take `Category.name` strings.
- `Expense` has no `remoteId`; the domain model only carries a nullable `id`.
- An exception in an upstream flow of a VM-scoped `stateIn` (no `.catch` before it) crashes `viewModelScope`, not just the flow; put `.catch` before inner `stateIn`s too (ExpenseList/Analysis).
- `FakeExpenseRepository.shouldThrow = true` makes date-range, category and total-amount read flows throw; set it before collecting `uiState`.
- macOS `sed -i` needs `sed -i ''`.
- `ProfileUseCases` is built by hand in ExpenseList and ExpensePreset VM tests; adding a field there means updating both tests (named args).
- A `todayFlow`-style ticker (infinite `delay` loop) makes `advanceUntilIdle()` hang while a collector is active; use `runCurrent()` / `advanceTimeBy(...)` in those tests and `job.cancel()` after.
- With `launch { vm.uiState.collect {} }` under runTest, call `runCurrent()` before reading `.value`/sending events (the launch is queued; `uiState` is still Loading).
- `BudgetRepository` has `updateBudgetWithHistory`; `FakeBudgetRepository` implements it. `MutableClock` (test root) is the controllable `java.time.Clock`.
- navigation-compose 2.10.x pulls lifecycle 2.11.0 transitively, which needs compileSdk 37 and AGP ≥ 9.2.0; pinning lifecycle to 2.10.0 in the catalog does not prevent it. Bump them together with the build tools. `checkDebugAarMetadata` is where it fails.
- Don't rewrite Material3 `Typography` roles as plain `TextStyle(...)` to "match defaults": M3's defaults carry extra settings (line-height style, platform style) a plain TextStyle lacks, so text shifts. Override only what you actually change, e.g. `Typography().bodyLarge.copy(...)`.
- `~/.claude-personal/skills/*` are symlinks into `~/.agents/skills/` (not version-controlled). Back up with `cp -L` (plain `cp -R` copies the link, not the file). Originals of the 2026-11-25 tracker refactor are in `~/.claude-personal/backups/2026-11-25-feature-tracker/`.
- During subagent-driven builds, task-runner implementers never commit; the controller commits per task. Files outside git get reviewed as `diff -u snapshot live` files.
- 2026-11-26: the P30 task-runner run left `tracker/Polish.md` empty (all open rows lost; committed in baf7a79, restored from 16858f7) and `check_tracker.py` still printed OK on the empty file. Before committing a task-runner result, check `git diff --stat tracker/` for unexpected deletions.
- Retry pattern: put `.catch` inside `_retryTrigger.flatMapLatest { ... }`, not after it; a catch outside lets the outer flow complete and Retry then does nothing. A VM-scoped hot `stateIn` shared with paging (ExpenseList) can't carry the error, so the uiState pipeline uses a cold copy.
- `strings.xml` apostrophes must be escaped (`Couldn\'t`), else aapt fails with "Invalid unicode escape sequence".
- `viewModelOf` (Koin) supplies `SavedStateHandle` automatically for any VM that declares it as a constructor parameter, so no appModule change is needed. VM tests pass `SavedStateHandle(mapOf(key to value))`. The DataStore date filter (`getActiveDateFilter`) is not read by any VM; screen date filters live only in VM state (now SavedStateHandle).
- Commits made on a feature branch after its PR merged never reach `master` (2026-11-26: a `.gitignore` commit on merged `feat/analysis-history`). Start new work from a fresh `master`; cherry-pick strays.
- Old feature branches can be far behind `master` (`feat/improved-outlook-card` was 62 commits behind, missing tracker/ and docs/). Port the idea onto a fresh branch instead of building on it.
- Compose semantics: `clickable` merges child text, so adding `semantics { contentDescription }` makes TalkBack read twice; use `clearAndSetSemantics { ... }` when you supply the full description.
- Material3 `Text(autoSize = TextAutoSize.StepBased(maxFontSize = ...))` (import `androidx.compose.foundation.text.TextAutoSize`) is available with BOM 2026.04.01; use it for single-line amounts in fixed-size boxes.
- `ModalBottomSheet` is its own window: a screen-level `SnackbarHost` shows *under* an open sheet. Render the host inside the sheet while it is shown (one `SnackbarHostState`, one visible host).
- Bottom-tab navigation keeps a screen's back-stack entry (and ViewModel) alive (`saveState`/`restoreState`), so one-off UI state in the VM (a pending snackbar/undo) re-fires on return. Clear it from the Route with `DisposableEffect(Unit) { onDispose { ... } }`; don't clear it in a `finally` of the `LaunchedEffect(message)`, or a newer message restarting the effect gets wiped.
- M3 `SnackbarHost` runs the auto-dismiss timer inside the host (`LaunchedEffect(currentSnackbarData)`), so composing a new host (e.g. moving it into a bottom sheet) restarts the timer. If a snackbar moves between hosts, show it with `SnackbarDuration.Indefinite` and own the timeout in the caller.
- `sh.calvin.reorderable` `longPressDraggableHandle` on a parent doesn't stop child `clickable`s: on a still long-press the finger-up reaches the children first and fires their onClick. Its `onDragStarted` runs synchronously at long-press detection, so a guard flag set there (and cleared shortly after `onDragStopped`) can swallow those taps; every clickable under the handle needs it. Don't add `combinedClickable(onLongClick)` on the child: it consumes the gesture and starves the drag.
- Room: a column added with `ADD COLUMN ... DEFAULT 0` needs `@ColumnInfo(defaultValue = "0")` on the entity, or schema validation fails on open.
- M3 1.4 `TooltipBox` can make its popup focusable (touch-modal): a tap outside only dismisses it and never reaches what's underneath (switching between tooltip anchors takes two taps). Pass `focusable = false` for pass-through taps; M3 still forces focus when TalkBack is on.
- M3 `RichTooltip`'s text slot already adds 16dp horizontal padding (plus a small vertical one); don't add the mockup's full padding again inside it.
- A `role` set on a non-clickable child doesn't merge into a clickable parent; put `role = Role.Button` on the `clickable` itself.
