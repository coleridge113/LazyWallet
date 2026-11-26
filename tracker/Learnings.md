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
