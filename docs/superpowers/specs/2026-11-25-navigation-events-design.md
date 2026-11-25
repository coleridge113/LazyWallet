# F02 Navigation events — design

Date: 2026-11-25 · Status: approved 2026-11-25 · Owner: Jose Luna · Branch: `feat/navigation-events` · Absorbs Polish ticket P02 (seed notes folded in here)

## Intent and why

ViewModels tell the UI to change screens by sending objects on a `Channel` (`_navigation.send(...)`, collected in a `LaunchedEffect` in each Route). Google's UI-layer events guidance (developer.android.com/topic/architecture/ui-layer/events) says ViewModel-originated one-off events can be lost and should instead be **UI state**; UI-originated navigation is **UI logic** and belongs in lambdas hoisted to the caller. F02 moves Lazy Wallet to that pattern so navigation can't be dropped and ViewModels stop knowing about navigation.

## Goals

- No `Channel`/`receiveAsFlow` navigation left in `presentation/`.
- Click navigation = lambdas passed from `NavGraph.kt`, guarded with `dropUnlessResumed`.
- Result navigation (sign-in finished, sign-out finished, migration finished) = a state flag the Route reacts to, then clears.
- Routes stop taking `NavController`; only `NavGraph.kt` touches it.
- Behaviour of every working path is unchanged.

## Non-goals

- Reviving paths that are intentionally unreachable today: Analysis entry (→ F10), Cloud Sync screen registration and guest upgrade (→ F09), Analysis tab (→ F05/P17).
- Moving Firebase out of the UI layer (→ F03), though F02 must not make F03 harder.
- Changing what any screen looks like.

## Anti-goals

- A navigation that fires twice (double tap, recomposition, returning to the screen) or not at all.
- A flag that, left set, re-navigates when the user comes back to the screen.

## Current state (code, 2026-11-25)

| Path | Today | Kind |
|---|---|---|
| Sign-in success → ExpensePreset (pop Auth) | `AuthViewModel.handleSignInSuccess` sends `Navigation.GotoAddExpenseRoute` on a rendezvous `Channel` in a `finally` after `syncFromCloud()`; `AuthRoute` collects and navigates. Also triggered on launch when `FirebaseAuth.currentUser != null` via `LaunchedEffect` in `AuthContent`. | result |
| Logout → Auth (pop everything) | `ExpensePresetViewModel.signOutUser` sends `Navigation.Logout` after `authUseCases.signOut(auth)`. | result |
| Budget card → BudgetDetails(budgetId) | `Event.GotoBudgetDetails` → VM sends `Navigation.GotoBudgetDetails`. | click |
| BudgetDetails back | `navController.navigateUp()` inside the Route. | click |
| Migration done/skip → ExpensePreset (pop Auth) | `MigrationViewModel` sends `MigrationNavigation.GotoAddExpenseRoute`. Route exists but isn't registered in `NavGraph` (unreachable; F09 will register it). | result |
| Dead | ExpensePreset `Event.GotoExpenseRoute`/`GotoAnalysisRoute` + `Navigation.GotoExpenseRoute`/`GotoAnalysisRoute` (Route ignores them with `else -> {}`; home-total `singleClick` sends one); ExpenseList `Event.GotoBarGraph` + `Navigation.GotoAnalysisRoute` (no sender, no collector); `analysis/AnalysisStateEvents.kt` empty `sealed interface Navigation`. | dead |

## Approaches considered

1. **Google's pattern: lambdas for clicks, state flags for results** (chosen by the user). ViewModels lose all navigation code; Routes receive `onXxx` lambdas; results are a flag in state plus an "I navigated" callback.
2. Keep events, collect them safely (`Channel(BUFFERED)` + `repeatOnLifecycle`/`Dispatchers.Main.immediate`). Smaller diff but is the pattern the guidance moves away from.
3. (Dropped) ViewModel calls a navigation callback — couples the ViewModel to the UI.

## The design

### Click navigation
- `BudgetRoute(viewModel, onOpenBudgetDetails: (BudgetId) -> Unit)`; the card's `onClick` calls it directly. Remove `Event.GotoBudgetDetails`, `Navigation.GotoBudgetDetails`, `gotoBudgetDetails`, the Channel.
- `BudgetDetailsRoute(viewModel, onBack: () -> Unit)`; the back icon calls `onBack`.
- In `NavGraph.kt`, each lambda wraps the navigate call with `dropUnlessResumed { ... }` (lifecycle-runtime-compose, `LocalLifecycleOwner` of the back-stack entry) so a tap during a transition or a double tap is ignored.

### Result navigation (state flag + consume)
Per ViewModel, a single navigation flag exposed as `StateFlow` with a consume function:
- `AuthViewModel`: `val signedIn: StateFlow<Boolean>`; set `true` where it sent `GotoAddExpenseRoute` (still after `syncFromCloud` + `setGuestMode`); `fun onSignedInHandled()` resets to `false`.
- `ExpensePresetViewModel`: `val signedOut: StateFlow<Boolean>`; set `true` after `authUseCases.signOut(auth)`; `fun onSignedOutHandled()`.
- `MigrationViewModel`: add `val isDone: Boolean = false` to the existing `MigrationUiState`; set in `startMigration` success and `skipMigration`; `fun onDoneHandled()`.

Route side, the same shape everywhere:
```kotlin
val signedIn by viewModel.signedIn.collectAsStateWithLifecycle()
LaunchedEffect(signedIn) {
    if (signedIn) { onSignedIn(); viewModel.onSignedInHandled() }
}
```
`onSignedIn` / `onSignedOut` / `onMigrationDone` are lambdas from `NavGraph.kt` holding today's exact `navigate { popUpTo ... }` options. Routes no longer take `navController`.

Why a separate `StateFlow<Boolean>` instead of a field in `UiState`: Auth has no UiState (removed in P05) and ExpensePreset's `UiState` is a sealed Loading/Error/Success — a sign-out flag must work in any of them. Migration already has a plain data class state, so it uses a field. (Builder's call if a cleaner single shape emerges, as long as it's state + consume.)

### Remove dead navigation
Delete the dead events/objects in the table above, the home-total `singleClick { onEvent(Event.GotoExpenseRoute) }` (the total stays visible, just not tappable), ExpenseList's `gotoAnalysisRoute` and Channel, and `analysis/AnalysisStateEvents.kt`'s empty `Navigation`. F10 adds Analysis entry later with this pattern.

### Tests
- `BudgetViewModelTest`, `ExpensePresetViewModelTest`, `ExpenseListViewModelTest`: update for removed events; add `ExpensePresetViewModelTest`: sign-out sets `signedOut` true, `onSignedOutHandled()` resets it (MockK the sign-out use case / FirebaseAuth as needed).
- New `MigrationViewModelTest`: skip → `isDone`; start success → `isDone`; start failure → `isDone` stays false, `error` set (MockK `DataMigrationRepository`, which is a concrete class).
- `AuthViewModel`: test `handleSignInSuccess` sets `signedIn` true even when `syncFromCloud` throws (MockK its deps). Google/credential paths are out of scope.

## Builder's calls (settled after review)

- **Result lambdas are not wrapped in `dropUnlessResumed`.** It skips (doesn't defer) its body when the entry isn't RESUMED, so a guarded result navigation could be dropped while the flag is cleared: the "not at all" anti-goal. Results are already protected by the flag: `collectAsStateWithLifecycle` only delivers while the Route is at least STARTED, the `LaunchedEffect(flag)` runs once per `true`, and the flag is cleared right after the navigate call. Only click lambdas get `dropUnlessResumed`.
- **Migration isn't registered in `NavGraph.kt`.** `MigrationRoute` gets its new `onMigrationDone: () -> Unit` parameter and the ViewModel its `isDone` flag; nothing calls it yet. F09 registers the route and supplies the lambda.

## Constraints

- Work in place on `feat/navigation-events`; no worktree (gitignored build files).
- Keep `navigate` options byte-for-byte (pop-up targets, `inclusive`, `launchSingleTop`).
- `dropUnlessResumed` comes from `androidx.lifecycle:lifecycle-runtime-compose` (lifecycle 2.10.0 is current; it's available since 2.7) — no version bump.
- Koin: ViewModel constructors don't change.
- Checks: `./gradlew testDebugUnitTest assembleDebug` (27 passing today).

## Noted for other features (not F02)

- On every launch with a signed-in Firebase user, Auth calls `syncFromCloud()`, which wipes and reloads all local tables → F09.
- A guest relaunching the app sees the sign-in screen again (guest flag isn't used to skip it) → F09.

## Left to the builder

Lambda names beyond those above; whether `dropUnlessResumed` lives in `NavGraph` or a tiny helper; test file locations (follow existing `presentation/screen/<feature>/`); order of the per-screen changes.
