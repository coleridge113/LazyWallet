# F15 — Reorder presets by long-press drag (ExpensePreset screen)

Date: 2026-11-26. Status: approved 2026-11-26. Builds on F13 (`feat/home-redesign`, unmerged).

## Intent
The user wants their most-used presets at the start of the grid. They arrange the order themselves (not auto-sorted), and it stays put.

## Goals
- Long-press a preset tile on the ExpensePreset screen, drag it to a new cell, release: the grid keeps that order.
- Order persists across app restarts and follows the account (Firestore), so a reinstall or second phone sees the same order.
- New presets are added at the end.

## Non-goals
- Auto-sorting by usage.
- Reordering anywhere else (Expenses list, category filters).
- An explicit "edit mode" with handles or a Done button.

## Anti-goals
- A long-press must never log an expense; a plain tap still logs instantly.
- Dragging must not open the ⋮ menu or the custom-amount dialog.
- A failed sync must not lose or scramble the local order.

## Approaches
1. **`sh.calvin.reorderable` library (picked by the user).** Supports `LazyVerticalGrid`, gives lift/slide animations, edge auto-scroll and accessibility move actions. Costs one dependency.
2. Hand-rolled `detectDragGesturesAfterLongPress` + grid hit-testing. No dependency, ~150 lines of gesture code, edge-scroll and animation are bug-prone.
3. Auto-sort by usage count. Cheapest, but the order shifts on its own (breaks muscle memory) and isn't the requested control. Dropped.

## Decided
- Interaction: long-press anywhere on the tile body lifts it (haptic feedback on lift); drag; release saves. Tap = log, icon = custom dialog, ⋮ = Edit/Delete, all unchanged.
- The dashed New preset tile is pinned last and is not draggable or a drop target.
- Storage: new `position: Int` on the preset (Room column `position`, Firestore field `position`). Room `MIGRATION_5_6` adds the column and backfills it from the current row order (`id` ascending), so the first launch after update looks the same as before.
- Reads order by `position, id`.
- The order is saved once on release (not on every hovered cell): the VM gets the full new id order and the repository writes all changed positions to Room in one transaction, then best-effort to Firestore (batch), errors logged and swallowed like other preset writes.
- Editing a preset keeps its position; deleting leaves gaps (fine, order is relative).
- Cloud restore: Firestore docs without `position` (written before F15) fall back to `createdAt` order after those that have one.

## Left to the builder
- Library version (latest stable compatible with the project's Compose BOM) and the exact list-to-grid wiring.
- Whether the UI holds a local copy of the list during the drag (needed so tiles move smoothly before Room emits) — expected yes.
- Lift visual (scale/elevation) within M3 conventions.
- TalkBack: expose the library's move actions ("Move earlier" / "Move later") with string resources.

## Done when
- Unit tests: reorder computes only changed positions, cloud restore ordering, VM reorder event, new preset lands last, edit keeps position.
- On device: install over the F13 build, first launch keeps the old order (migration backfill; `exportSchema = false` rules out `MigrationTestHelper`); drag a tile to first place, kill and reopen the app, order kept; tap still logs; long-press never logs; ⋮ still works; New preset tile can't move.
