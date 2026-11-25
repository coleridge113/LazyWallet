# F02 Navigation Events Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace Channel-based navigation with hoisted click lambdas and state-flag result navigation, so no navigation can be dropped and ViewModels/Routes stop touching `NavController`.

**Architecture:** `NavGraph.kt` owns every `navController` call. Routes take `onXxx` lambdas. Click lambdas are guarded by a resumed check; result navigation is a `StateFlow<Boolean>` (or a `MigrationUiState` field) the Route reacts to with `LaunchedEffect(flag)`, then clears via `onXxxHandled()`.

**Tech Stack:** Kotlin, Jetpack Compose, Navigation Compose 2.9.8 (type-safe routes), lifecycle 2.10.0, Koin, JUnit4 + MockK + Truth + coroutines-test (`MainDispatcherRule`).

**Spec:** `docs/superpowers/specs/2026-11-25-navigation-events-design.md`

## Global Constraints

- Work in place on `feat/navigation-events`; never create a worktree.
- Implementers (`task-runner`) never commit; the controller commits after each task review, staging only the listed files.
- Keep every `navigate` option exactly as today (pop-up targets, `inclusive`, `launchSingleTop`, `saveState`).
- No dependency/version changes. Koin ViewModel constructors unchanged.
- Only click lambdas are resume-guarded; result lambdas are not.
- Routes must not take a `NavController` parameter when this plan is done.
- `CancellationException` is always rethrown from any new `catch`.
- Checks per task: `./gradlew testDebugUnitTest assembleDebug` from `android/` (27 tests pass at start).

## Review Focus

1. **Coming back to a screen after a result navigation** (e.g. back to Auth via logout): the old flag must not re-navigate → each flag test asserts `onXxxHandled()` resets it to `false` (Tasks 2, 3, 4).
2. **Cloud sync throwing during sign-in**: user still lands on home → Task 3 test `signInSuccess_whenSyncThrows_stillSetsSignedIn`.
3. **Migration failing**: user stays on the Cloud Sync page with the error, no navigation → Task 4 test `startMigration_failure_doesNotSetDone`.
4. **Sign-out throwing** (Firebase/DB error): no crash, no navigation → Task 2 test `signOut_whenSignOutThrows_doesNotSetSignedOut`.
5. **Double tap on a budget card / back icon during a transition**: one navigation only → resumed guard in Task 1 (not unit-testable; reviewer checks the guard wraps both click lambdas).

---

### Task 1: Click lambdas for Budget and Budget Details

**Files:**
- Modify: `app/src/main/java/com/luna/budgetapp/presentation/nav/NavGraph.kt` (BudgetRoute and BudgetDetailsRoute `composable` blocks; add helper)
- Modify: `.../presentation/screen/budget/BudgetRoute.kt`, `BudgetViewModel.kt`, `BudgetStateEvents.kt`
- Modify: `.../presentation/screen/budgetdetails/BudgetDetailsRoute.kt`

**Interfaces:**
- Produces: `private inline fun NavBackStackEntry.ifResumed(block: () -> Unit)` in `NavGraph.kt` — runs `block` only when `lifecycle.currentState == Lifecycle.State.RESUMED` (same rule as `dropUnlessResumed`, but usable inside lambdas with parameters). Later tasks don't use it for results.
- Produces: `fun BudgetRoute(viewModel: BudgetViewModel, onOpenBudgetDetails: (BudgetId) -> Unit)`; `fun BudgetDetailsRoute(viewModel: BudgetDetailsViewModel, onBack: () -> Unit)` (keep any other existing params except `navController`).

- [ ] **Step 1:** Remove `Event.GotoBudgetDetails`, `sealed interface Navigation`, `_navigation`/`navigation`, `gotoBudgetDetails()` from Budget; the budget card's `onClick` calls `onOpenBudgetDetails(budget.id)`; delete the Route's navigation `LaunchedEffect` and its `navController` param.
- [ ] **Step 2:** BudgetDetails back icon calls `onBack()`; drop `navController` param.
- [ ] **Step 3:** In `NavGraph.kt` pass `onOpenBudgetDetails = { id -> backStackEntry.ifResumed { navController.navigate(Routes.BudgetDetailsRoute(id)) } }` and `onBack = { backStackEntry.ifResumed { navController.navigateUp() } }` (use the `composable { backStackEntry -> }` parameter).
- [ ] **Step 4: Verify** `./gradlew testDebugUnitTest assembleDebug` → 27 passing, build OK; `grep -n "Channel\|navController" app/src/main/java/com/luna/budgetapp/presentation/screen/budget app/src/main/java/com/luna/budgetapp/presentation/screen/budgetdetails -r` → no hits.
- [ ] **Step 5 (controller): Commit** — "refactor: budget navigation through hoisted lambdas (F02)".

### Task 2: ExpensePreset sign-out flag and dead events

**Files:**
- Modify: `.../screen/expensepreset/ExpensePresetViewModel.kt`, `ExpensePresetStateEvents.kt`, `ExpensePresetRoute.kt`, `nav/NavGraph.kt`
- Test: `app/src/test/java/com/luna/budgetapp/presentation/screen/expensepreset/ExpensePresetViewModelTest.kt`

**Interfaces:**
- Produces: `val signedOut: StateFlow<Boolean>` (initial `false`), `fun onSignedOutHandled()` on `ExpensePresetViewModel`; `fun ExpensePresetRoute(viewModel: ExpensePresetViewModel, onSignedOut: () -> Unit)`.

- [ ] **Step 1: Failing tests** in `ExpensePresetViewModelTest` (build `AuthUseCases` with `signOut = mockk<SignOutUseCase>()`):
  - `signOut_setsSignedOut_andHandledResetsIt`: `coEvery { signOut(any()) } just Runs`; `onEvent(Event.SignOut(mockk(relaxed = true)))`; `runCurrent()`; `assertThat(vm.signedOut.value).isTrue()`; `vm.onSignedOutHandled()`; `assertThat(vm.signedOut.value).isFalse()`.
  - `signOut_whenSignOutThrows_doesNotSetSignedOut`: `coEvery { signOut(any()) } throws RuntimeException("boom")`; same event; `assertThat(vm.signedOut.value).isFalse()` (and no crash).
- [ ] **Step 2:** Run `./gradlew testDebugUnitTest --tests "*ExpensePresetViewModelTest*"` → FAIL (unresolved `signedOut`).
- [ ] **Step 3:** Implement: `signOutUser` sets `_signedOut.value = true` after `authUseCases.signOut(auth)`; wrap in try/catch (rethrow `CancellationException`, `Log.e`, flag stays false). Remove `Navigation` (all three objects), `Event.GotoExpenseRoute`, `Event.GotoAnalysisRoute`, `gotoExpenseRoute()`, the Channel, and the home total's `.singleClick { onEvent(Event.GotoExpenseRoute) }` (total stays displayed). Route: replace the navigation `LaunchedEffect` with `LaunchedEffect(signedOut) { if (signedOut) { onSignedOut(); viewModel.onSignedOutHandled() } }` collected via `collectAsStateWithLifecycle()`; drop `navController`. NavGraph: `onSignedOut` holds today's `navigate(Routes.AuthRoute) { popUpTo(navController.graph.startDestinationId) { inclusive = true }; launchSingleTop = true }`, unguarded.
- [ ] **Step 4:** Tests pass; full `./gradlew testDebugUnitTest assembleDebug` → 29 passing.
- [ ] **Step 5 (controller): Commit** — "refactor: sign-out navigation as state flag; removed dead preset events (F02)".

### Task 3: Auth sign-in flag

**Files:**
- Modify: `.../screen/auth/AuthViewModel.kt`, `AuthStateEvents.kt`, `AuthRoute.kt`, `nav/NavGraph.kt`
- Create test: `app/src/test/java/com/luna/budgetapp/presentation/screen/auth/AuthViewModelTest.kt`

**Interfaces:**
- Produces: `val signedIn: StateFlow<Boolean>` (initial `false`), `fun onSignedInHandled()` on `AuthViewModel`; `fun AuthRoute(viewModel: AuthViewModel, onSignedIn: () -> Unit)`.

- [ ] **Step 1: Failing tests** (`AuthViewModel(authUseCases = mockk(relaxed = true), migrationRepository = mockk(), settings = mockk())`, `MainDispatcherRule`):
  - `signInSuccess_setsSignedIn_andSavesGuestMode`: `coEvery { migrationRepository.syncFromCloud() } just Runs`; `coEvery { settings.setGuestMode(true) } just Runs`; `onEvent(Event.HandleSignInSuccess(isGuest = true))`; `advanceUntilIdle()`; `assertThat(vm.signedIn.value).isTrue()`; `coVerify { settings.setGuestMode(true) }`; then `vm.onSignedInHandled()` → `false`.
  - `signInSuccess_whenSyncThrows_stillSetsSignedIn`: `syncFromCloud()` throws `RuntimeException`; `assertThat(vm.signedIn.value).isTrue()`.
- [ ] **Step 2:** Run `--tests "*AuthViewModelTest*"` → FAIL.
- [ ] **Step 3:** Implement: in `handleSignInSuccess`'s `finally`, set `_signedIn.value = true` instead of sending; remove `Navigation`, the Channel. Route: `LaunchedEffect(signedIn) { if (signedIn) { onSignedIn(); viewModel.onSignedInHandled() } }`; drop `navController`. NavGraph `onSignedIn` keeps `navigate(Routes.ExpensePresetRoute) { popUpTo(Routes.AuthRoute) { inclusive = true } }`, unguarded. The existing auto sign-in `LaunchedEffect` in `AuthContent` stays as is.
- [ ] **Step 4:** Full checks → 31 passing.
- [ ] **Step 5 (controller): Commit** — "refactor: sign-in navigation as state flag (F02)".

### Task 4: Migration done flag

**Files:**
- Modify: `.../screen/migration/MigrationViewModel.kt`, `MigrationScreen.kt`
- Create test: `app/src/test/java/com/luna/budgetapp/presentation/screen/migration/MigrationViewModelTest.kt`

**Interfaces:**
- Produces: `MigrationUiState(isLoading: Boolean = false, error: String? = null, isDone: Boolean = false)`; `fun onDoneHandled()`; `fun MigrationRoute(viewModel: MigrationViewModel, onMigrationDone: () -> Unit)`. Not registered in `NavGraph.kt` (F09 does that).

- [ ] **Step 1: Failing tests** (`MigrationViewModel(migrationRepository = mockk(), settingsUseCases = SettingsUseCases(..., setMigrationComplete = mockk()))` with `coEvery { setMigrationComplete() } just Runs`):
  - `skip_setsDone`: `onEvent(MigrationEvent.SkipMigration)` → `uiState.value.isDone` true; `onDoneHandled()` → false.
  - `startMigration_success_setsDone`: `performMigration()` and `syncFromCloud()` `just Runs` → `isDone` true, `isLoading` false.
  - `startMigration_failure_doesNotSetDone`: `performMigration()` throws `RuntimeException("offline")` → `isDone` false, `error == "offline"`, `isLoading` false.
- [ ] **Step 2:** Run `--tests "*MigrationViewModelTest*"` → FAIL.
- [ ] **Step 3:** Implement: replace both `_navigation.send(...)` with `_uiState.update { it.copy(isLoading = false, isDone = true) }`; remove `MigrationNavigation` and the Channel; Route reacts with `LaunchedEffect(state.isDone)`; drop `navController`.
- [ ] **Step 4:** Full checks → 34 passing.
- [ ] **Step 5 (controller): Commit** — "refactor: migration completion as state flag (F02)".

### Task 5: Remove remaining dead navigation and sweep

**Files:**
- Modify: `.../screen/expenselist/ExpenseListViewModel.kt`, `ExpenseListStateEvents.kt`, `.../screen/analysis/AnalysisStateEvents.kt`
- Test: `app/src/test/java/com/luna/budgetapp/expenselist/ExpenseListViewModelTest.kt` (only if it references removed symbols)

- [ ] **Step 1:** Remove ExpenseList `Event.GotoBarGraph`, `Navigation`, `gotoAnalysisRoute()`, Channel; Analysis's empty `sealed interface Navigation`.
- [ ] **Step 2: Sweep** — `grep -rnE "Channel|receiveAsFlow|navController" app/src/main/java/com/luna/budgetapp/presentation/screen` → no hits; `grep -rn "navController" app/src/main/java/com/luna/budgetapp/presentation/nav` → only `NavGraph.kt`.
- [ ] **Step 3:** Full checks → 34 passing, build OK.
- [ ] **Step 4 (controller): Commit** — "refactor: removed dead navigation events (F02)".
