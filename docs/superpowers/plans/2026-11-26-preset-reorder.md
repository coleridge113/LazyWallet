# F15 Preset Reorder Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Long-press a preset tile on the ExpensePreset screen and drag it to a new place; the order persists in Room and mirrors to Firestore.

**Architecture:** A `position: Int` is added to the preset at every layer (Room column via `MIGRATION_5_6`, domain model, Firestore doc). Reads order by `position, id`. The grid keeps a local copy of the list during a drag (via `sh.calvin.reorderable`) and on release sends the full id order to the VM → `ReorderExpensePresetsUseCase` → repository, which writes only changed positions in one Room transaction and best-effort to Firestore.

**Tech Stack:** Kotlin 2.3.21, Compose BOM 2026.04.01, Room 2.8.4, Koin, Firestore, `sh.calvin.reorderable:reorderable`.

**Spec:** `docs/superpowers/specs/2026-11-26-preset-reorder-design.md`

**Branch:** work in place on `feat/home-redesign` (F13, unmerged; `PresetTileGrid` exists only there). No worktree.

## Global Constraints

- Tap = log instantly, icon = custom dialog, ⋮ = Edit/Delete: unchanged. A long-press must never log an expense.
- The dashed New preset tile (`key = "new_preset"`) stays last; not draggable, not a drop target.
- New presets get `position = max + 1` (end of grid). Editing keeps the position.
- Order is saved once, on release (not per hovered cell).
- Firestore failures are logged (`Log.e("Sync", ...)`) and swallowed, like the other preset writes.
- Name things "ExpensePreset"/"preset", never "Home". Never commit `docs/screens/`.
- Money stays in cents; untouched here.

## Review Focus

1. **Long-press then release without moving** must not log an expense and must not write anything (the order is unchanged). Pinned by `reorderedPositions` returning empty for an unchanged order (Task 1) and by the device check (Task 3).
2. **Room emits during/after the drop**: the grid's local list must re-sync from `presets` when it changes, but a drag in progress must not snap back. Device check in Task 3.
3. **Edit after reorder**: saving the edit form builds a fresh `ExpensePreset` (position 0 by default); the repository must keep the stored position. Pinned in Task 1 (fake) and Task 2 (`edit keeps preset position`).
4. **Cloud restore with old docs** (no `position`): they go after positioned ones, by `createdAt`. Pinned by `normalizeCloudPresetOrder` tests (Task 1).
5. **Upgrade with existing presets**: first launch after the update shows the same order as before. Verified on device by installing over the current F13 build (Task 3), since `exportSchema = false` rules out `MigrationTestHelper`.

---

### Task 1: Persist preset position (data layer)

**Files:**
- Modify: `app/src/main/java/com/luna/budgetapp/data/local/entity/ExpensePresetEntity.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/domain/model/ExpensePreset.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/data/firebase/models/ExpensePreset.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/data/mapper/ExpensePresetMapper.kt`, `app/src/main/java/com/luna/budgetapp/data/firebase/Mappers.kt`
- Create: `app/src/main/java/com/luna/budgetapp/data/local/migrations/Migration5to6.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/data/local/AppDatabase.kt` (version 5 → 6), `app/src/main/java/com/luna/budgetapp/di/appModule.kt:144` (`addMigrations(..., MIGRATION_5_6)`)
- Modify: `app/src/main/java/com/luna/budgetapp/data/local/dao/ExpensePresetDao.kt`
- Create: `app/src/main/java/com/luna/budgetapp/data/mapper/PresetOrdering.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/domain/repository/ExpensePresetRepository.kt`, `app/src/main/java/com/luna/budgetapp/data/local/repository/ExpensePresetRepositoryImpl.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/data/firebase/migration/DataMigrationRepository.kt` (`syncFromCloud`)
- Modify: `app/src/test/java/com/luna/budgetapp/domain/repository/FakeExpensePresetRepository.kt`
- Test: `app/src/test/java/com/luna/budgetapp/data/mapper/PresetOrderingTest.kt`

**Interfaces:**
- Produces:
  - `ExpensePresetEntity.position: Int` — `@ColumnInfo(name = "position", defaultValue = "0") val position: Int = 0`. The `defaultValue = "0"` is required: the migration adds the column with `DEFAULT 0`, and Room's schema validation fails on open if the entity doesn't declare the same default.
  - `ExpensePreset.position: Int = 0` (domain), carried both ways by the mappers (the Retrofit DTO mapper leaves it at 0).
  - Firestore `ExpensePreset.position: Int? = null`; `ExpensePresetEntity.toFirestoreModel()` writes it.
  - `MIGRATION_5_6`: `ALTER TABLE expense_presets ADD COLUMN position INTEGER NOT NULL DEFAULT 0`, then backfill `position` = the row's rank by `id` ascending, starting at 0 (`UPDATE expense_presets SET position = (SELECT COUNT(*) FROM expense_presets p2 WHERE p2.id < expense_presets.id)`).
  - DAO: both `getAllExpensePresets()` and `getAllExpensePresetsOnce()` read `ORDER BY position, id`; `suspend fun getMaxPosition(): Int?` (`SELECT MAX(position) ...`); `suspend fun updatePosition(id: Long, position: Int)`; `@Transaction suspend fun updatePositions(positions: Map<Long, Int>)` (default interface method looping `updatePosition`).
  - `PresetOrdering.kt` (pure, no Android):
    - `fun reorderedPositions(orderedIds: List<Long>, current: Map<Long, Int>): Map<Long, Int>` — new position = index in `orderedIds`; returns only ids whose position changes; ignores ids not in `current`.
    - `fun normalizeCloudPresetOrder(presets: List<com.luna.budgetapp.data.firebase.models.ExpensePreset>): List<ExpensePresetEntity>` — sort by positioned first (by `position`), then unpositioned by `createdAt`; map with `toEntity()` and set `position` = final index (0-based).
  - `ExpensePresetRepository.reorderExpensePresets(orderedIds: List<Long>)` (suspend).
  - Impl:
    - `addExpensePreset` sets `position = (dao.getMaxPosition() ?: -1) + 1`.
    - `updateExpensePreset` keeps `existingEntity.position` (falls back to the incoming one if not found).
    - `reorderExpensePresets` reads `getAllExpensePresetsOnce()`, computes `reorderedPositions`, returns early if empty, else `dao.updatePositions(changes)`, then, if signed in, one Firestore `batch()` that does `set(mapOf("position" to p), SetOptions.merge())` on `users/{uid}/expense_presets/{remoteId}` for each changed preset with a `remoteId`, `commit()` inside try/catch, logged and swallowed.
  - `syncFromCloud` inserts `normalizeCloudPresetOrder(presets)` instead of `presets.map { it.toEntity() }`.
  - Fake:
    - `addExpensePreset` assigns `position = (max ?: -1) + 1`.
    - `updateExpensePreset` keeps the stored position.
    - `reorderExpensePresets` reorders the flow's list to `orderedIds` and sets each `position` to its index; ids not listed keep their relative order after the listed ones.

- [ ] **Step 1: Write the failing tests** in `PresetOrderingTest`:

```kotlin
@Test fun `unchanged order returns no changes`() {
    assertThat(reorderedPositions(listOf(1L, 2L, 3L), mapOf(1L to 0, 2L to 1, 3L to 2))).isEmpty()
}
@Test fun `moving last to first returns only changed positions`() {
    assertThat(reorderedPositions(listOf(3L, 1L, 2L), mapOf(1L to 0, 2L to 1, 3L to 2)))
        .containsExactly(3L, 0, 1L, 1, 2L, 2)
}
@Test fun `gaps from deletes are compacted only where order changes`() {
    // 1 at 0, 3 at 5 (gap); order unchanged -> 3 moves to 1
    assertThat(reorderedPositions(listOf(1L, 3L), mapOf(1L to 0, 3L to 5))).containsExactly(3L, 1)
}
@Test fun `unknown ids are ignored`() {
    assertThat(reorderedPositions(listOf(9L, 1L), mapOf(1L to 0))).containsExactly(1L, 1)
}
@Test fun `cloud presets without position go last by createdAt`() {
    // a(position 1), b(null, created Jan 2), c(position 0), d(null, created Jan 1)
    val result = normalizeCloudPresetOrder(listOf(a, b, c, d))
    assertThat(result.map { it.type }).containsExactly("c", "a", "d", "b").inOrder()
    assertThat(result.map { it.position }).containsExactly(0, 1, 2, 3).inOrder()
}
```

(Build `a`..`d` as Firestore `ExpensePreset(id = "...", type = "a", category = "FOOD", position = ..., createdAt = Date(...))`; use a valid `Category` name from `domain/model/Category.kt`.)

- [ ] **Step 2: Run, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*PresetOrderingTest"`
Expected: compile failure (functions not defined).

- [ ] **Step 3: Implement everything in the Interfaces block.**

- [ ] **Step 4: Run tests and build**

Run: `./gradlew testDebugUnitTest && ./gradlew assembleDebug`
Expected: all tests pass (129 + 5), build succeeds (KSP accepts the `@Transaction` default DAO method).

- [ ] **Step 5: Commit** (controller)

```bash
git add <files above>
git commit -m "feat(F15): persist preset position (Room 5->6, Firestore, reorder in repository)"
```

---

### Task 2: Reorder use case and ViewModel event

**Files:**
- Create: `app/src/main/java/com/luna/budgetapp/domain/usecase/expensepreset/ReorderExpensePresetsUseCase.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/domain/usecase/PresetUseCases.kt`, `app/src/main/java/com/luna/budgetapp/di/appModule.kt` (`factoryOf(::ReorderExpensePresetsUseCase)`)
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/screen/expensepreset/ExpensePresetStateEvents.kt`, `ExpensePresetViewModel.kt`
- Test: `app/src/test/java/com/luna/budgetapp/presentation/screen/expensepreset/ExpensePresetViewModelTest.kt` (also add the new use case to its hand-built `PresetUseCases(...)`, named args; see Learnings)

**Interfaces:**
- Consumes: `ExpensePresetRepository.reorderExpensePresets(orderedIds: List<Long>)`, `ExpensePreset.position` (Task 1).
- Produces:
  - `class ReorderExpensePresetsUseCase(repository: ExpensePresetRepository) { suspend operator fun invoke(orderedIds: List<Long>) }`.
  - `PresetUseCases.reorderExpensePresets: ReorderExpensePresetsUseCase`.
  - `Event.ReorderPresets(val orderedIds: List<Long>)`: the VM launches the use case in `viewModelScope`, try/catch, `Log.e` on failure (no UI feedback; that's F11).

- [ ] **Step 1: Write the failing tests** in `ExpensePresetViewModelTest` (follow the file's existing `launch { uiState.collect {} }` + `runCurrent()` pattern):
  - `reorder presets updates the preset order`: seed presets ids 1, 2, 3 → `onEvent(Event.ReorderPresets(listOf(3L, 1L, 2L)))` → `runCurrent()` → `uiState.value` (Success) `expensePresets.map { it.id }` == `[3, 1, 2]`.
  - `new preset is added last`: after the reorder above, add a preset through the existing `ConfirmExpenseFormDialog` ADD path → it is last in `expensePresets`.
  - `edit keeps preset position`: after the reorder, edit preset 1 through the EDIT path → order still `[3, 1, 2]`.

- [ ] **Step 2: Run, verify fail**

Run: `./gradlew testDebugUnitTest --tests "*ExpensePresetViewModelTest"`
Expected: compile failure (`Event.ReorderPresets` not defined).

- [ ] **Step 3: Implement the Interfaces block.**

- [ ] **Step 4: Run all tests**

Run: `./gradlew testDebugUnitTest`
Expected: all pass.

- [ ] **Step 5: Commit** (controller)

```bash
git commit -m "feat(F15): ReorderPresets event and use case"
```

---

### Task 3: Long-press drag in the preset grid

**Files:**
- Modify: `gradle/libs.versions.toml` (add `reorderable` = latest stable `sh.calvin.reorderable:reorderable` on Maven Central that works with Compose BOM 2026.04.01; note the version in the report), `app/build.gradle.kts`
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/screen/expensepreset/components/PresetTileGrid.kt`
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/screen/expensepreset/ExpensePresetRoute.kt:205-213`
- Modify: `app/src/main/res/values/strings.xml`

**Interfaces:**
- Consumes: `Event.ReorderPresets(orderedIds: List<Long>)` (Task 2).
- Produces: `PresetTileGrid(..., onReorder: (List<Long>) -> Unit, ...)`, which the Route wires to `onEvent(Event.ReorderPresets(it))`.

- [ ] **Step 1: Add the dependency** and run `./gradlew assembleDebug`. Expected: succeeds.

- [ ] **Step 2: Implement the drag in `PresetTileGrid`:**
  - Hold `rememberLazyGridState()` and a local `var items by remember { mutableStateOf(presets) }`. Re-sync it with `LaunchedEffect(presets) { if (!dragging) items = presets }`, so a Room emission never snaps a tile back mid-drag.
  - Create `rememberReorderableLazyGridState(gridState) { from, to -> ... }`. It moves the item by **key** (not index) in `items`, and ignores moves where either key is `"new_preset"`.
  - Wrap each preset in `ReorderableItem(state, key = preset.id ?: preset.hashCode())`. Don't wrap the New preset tile: that keeps it out of drag and drop.
  - Apply `Modifier.longPressDraggableHandle(onDragStarted = { haptic LongPress; dragging = true }, onDragStopped = { dragging = false; onReorder(items.mapNotNull { it.id }) })` to the tile's outer `Box`. Keep the inner `clickable` (log), the icon button (custom) and ⋮ exactly as they are.
  - While dragging, lift the tile: `shadowElevation`/`Surface` tonal elevation of about 8.dp and a scale of about 1.05f. Pick these within M3 conventions.
  - Add TalkBack custom actions inside the tile's existing `clearAndSetSemantics { ... }`. Add "Move earlier" (not on the first tile) and "Move later" (not on the last preset). Each swaps with its neighbour and calls `onReorder` with the new order.
  - New strings: `cd_move_preset_earlier` = "Move earlier", `cd_move_preset_later` = "Move later".
  - Update the `@Preview` calls to pass `onReorder = {}`.

- [ ] **Step 3: Wire the Route** with `onReorder = { onEvent(Event.ReorderPresets(it)) }`.

- [ ] **Step 4: Verify**

Run: `./gradlew testDebugUnitTest && ./gradlew assembleDebug`
Expected: all pass, build succeeds.

- [ ] **Step 5: Device check** (user, via `./gradlew installDebug` over the current F13 build, so the 5→6 migration runs on real data):
  1. The first launch shows the same preset order as before the update.
  2. Long-press a tile: it lifts with a buzz. Drag it to first place, release, kill the app and reopen it: the order is kept.
  3. Long-press and release without moving: nothing is logged, the order is unchanged.
  4. Tap still logs, the icon still opens the custom dialog, ⋮ still opens Edit/Delete.
  5. The New preset tile can't be dragged, and tiles can't be dropped after it.
  6. Edit a moved preset: it stays in place. Add a new preset: it appears last.
  7. TalkBack: the "Move earlier" / "Move later" actions are offered and work.

- [ ] **Step 6: Commit** (controller)

```bash
git commit -m "feat(F15): long-press drag to reorder preset tiles"
```
