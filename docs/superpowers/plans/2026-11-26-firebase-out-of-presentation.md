# F03 Firebase out of presentation — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make `presentation/` and `domain/` free of Firebase, Google Play services and `data/` imports, with sign-in/sign-out rules in use cases over small domain interfaces.

**Architecture:** Approach A from the spec. `AuthRepository` becomes plain suspend functions that throw a domain `AuthException`; new `CloudSyncRepository` and `LocalDataRepository` interfaces plus guest mode on `SettingsRepository`; use cases (`CompleteSignInUseCase`, `SignOutUseCase`, `IsSignedInUseCase`, `MigrateLocalDataUseCase`) chain the steps; ViewModels inject only use-case aggregates.

**Tech Stack:** Kotlin, Jetpack Compose, Koin, Firebase Auth (`kotlinx.coroutines.tasks.await`, already on the classpath), Room, DataStore; tests with JUnit4, Truth, coroutines-test, `MainDispatcherRule`, hand-written fakes.

**Spec:** `docs/superpowers/specs/2026-11-26-firebase-out-of-presentation-design.md` (approved 2026-11-26). Read its "Design", "Behaviour to keep" and "Clarifications" sections; they are binding.

Paths below are relative to `app/src/main/java/com/luna/budgetapp/` (main) and `app/src/test/java/com/luna/budgetapp/` (test) unless they start with `app/` or `docs/`.

## Global Constraints

- No `com.google.firebase`, `com.google.android.gms` or `com.luna.budgetapp.data` import under `presentation/` or `domain/` when the plan is done (Task 3 checks; Tasks 1–2 may leave the parts Task 3 removes).
- `com.google.android.libraries.identity` and `androidx.credentials` (Credential Manager) stay in presentation.
- Keep `single { FirebaseAuth.getInstance() }` in `di/appModule.kt`; data repositories keep using it.
- JWT members (`refreshJwtToken`, `fetchJwtToken`, `GetTokenUseCase`) unchanged.
- Every `catch (e: Exception)` rethrows `CancellationException` first (project pattern). No `NonCancellable`.
- Error strings: reuse existing `R.string.auth_error_*` ids; no new strings, no visible UI change.
- Tests: hand-written fakes in `test/.../domain/repository/`, no MockK for repositories or data classes in new or rewritten tests. Suite was 43 tests at start; it must only go up.
- Build and test commands (from `android/`): `./gradlew testDebugUnitTest` and `./gradlew assembleDebug`.
- Implementer never commits; the controller commits the files the implementer lists.

## Review Focus

1. **Auto sign-in firing twice.** `AuthRoute` must still pass a one-time value (`viewModel.isUserSignedIn`, a `val`) so recomposition cannot re-trigger `HandleSignInSuccess` (F02 regression 976aa2a). Task 3 test: `isUserSignedIn` is read once at construction (fake flips to false afterwards; property stays true).
2. **Guest loses data on sign-out / account user keeps data.** Task 2 tests both branches of `SignOutUseCase`, and that `signOut()` runs before `clearAll()`.
3. **Sync failure blocks sign-in.** Task 1 test: `CompleteSignInUseCase` sets guest mode when `downloadCloudData` throws; Task 3 test: VM still sets `signedIn` when sync throws.
4. **Weak password shows a new message.** `FirebaseAuthWeakPasswordException` extends `FirebaseAuthInvalidCredentialsException`; it must map to `Unknown` (generic). Task 3 mapping test covers it.
5. **Cancellation swallowed as an auth error.** Task 3 mapping test: `CancellationException` is rethrown, not wrapped in `Unknown`; Task 1 test: `CompleteSignInUseCase` propagates cancellation and does not set guest mode.

---

### Task 1: Sync, local-data and guest-mode plumbing; migration use case

**Files:**
- Create: `domain/repository/CloudSyncRepository.kt`, `domain/repository/LocalDataRepository.kt`
- Create: `data/local/repository/LocalDataRepositoryImpl.kt`
- Modify: `domain/repository/SettingsRepository.kt`, `data/local/repository/SettingsRepositoryImpl.kt`
- Modify: `data/firebase/migration/DataMigrationRepository.kt` (implement `CloudSyncRepository`)
- Modify: `domain/repository/AuthRepository.kt`, `data/local/repository/AuthRepositoryImpl.kt` (add `isSignedIn()` only; nothing else changes here)
- Create: `domain/usecase/auth/CompleteSignInUseCase.kt`, `domain/usecase/auth/IsSignedInUseCase.kt`, `domain/usecase/sync/MigrateLocalDataUseCase.kt`, `domain/usecase/SyncUseCases.kt`
- Modify: `domain/usecase/AuthUseCases.kt` (add `isSignedIn`, `completeSignIn`)
- Modify: `presentation/screen/migration/MigrationViewModel.kt`
- Modify: `di/appModule.kt`
- Create (test): `domain/repository/FakeCloudSyncRepository.kt`, `domain/repository/FakeLocalDataRepository.kt`, `domain/repository/FakeAuthRepository.kt`
- Modify (test): `domain/repository/FakeSettingsRepository.kt`
- Create (test): `domain/usecase/CompleteSignInUseCaseTest.kt`, `domain/usecase/MigrateLocalDataUseCaseTest.kt`
- Modify (test): `presentation/screen/migration/MigrationViewModelTest.kt`

**Interfaces:**
- Produces:
  - `interface CloudSyncRepository { suspend fun uploadLocalData(); suspend fun downloadCloudData() }` — `DataMigrationRepository` implements it (`uploadLocalData` = today's `performMigration`, `downloadCloudData` = today's `syncFromCloud`; rename or delegate, your call, but `AuthViewModel` still calls `syncFromCloud()` until Task 3, so keep it compiling).
  - `interface LocalDataRepository { suspend fun clearAll() }`; `LocalDataRepositoryImpl(db: AppDatabase)` runs `db.clearAllTables()` on `Dispatchers.IO`.
  - `SettingsRepository` gains `val isGuestFlow: Flow<Boolean>` and `suspend fun setGuestMode(isGuest: Boolean)` (delegate to `SettingsDataStore`).
  - `AuthRepository` gains `fun isSignedIn(): Boolean` (`firebaseAuth.currentUser != null`).
  - `class IsSignedInUseCase(repository: AuthRepository) { operator fun invoke(): Boolean }`
  - `class CompleteSignInUseCase(cloudSync: CloudSyncRepository, settings: SettingsRepository) { suspend operator fun invoke(isGuest: Boolean) }` — `downloadCloudData()`; non-cancellation exception → `Log.w` and continue; then `settings.setGuestMode(isGuest)`. Cancellation propagates and skips `setGuestMode`.
  - `class MigrateLocalDataUseCase(cloudSync: CloudSyncRepository, settings: SettingsRepository) { suspend operator fun invoke() }` — `uploadLocalData()`, `downloadCloudData()`, `settings.setMigrationComplete()`, in that order; any failure propagates.
  - `data class SyncUseCases(val migrateLocalData: MigrateLocalDataUseCase)`
  - `AuthUseCases` gains `val isSignedIn: IsSignedInUseCase`, `val completeSignIn: CompleteSignInUseCase` (append after existing members).
  - `MigrationViewModel(syncUseCases: SyncUseCases, settingsUseCases: SettingsUseCases)` — start path calls `syncUseCases.migrateLocalData()`; skip unchanged.
  - Fakes:
    - `FakeCloudSyncRepository(var uploadError: Throwable? = null, var downloadError: Throwable? = null)` with `val calls: MutableList<String>` recording `"upload"` / `"download"`.
    - `FakeLocalDataRepository` with `var clearCount: Int`.
    - `FakeAuthRepository(var signedIn: Boolean = false)` implementing the **current** `AuthRepository` (callback members as no-ops for now; Task 3 replaces them), `isSignedIn()` returns `signedIn`.
    - `FakeSettingsRepository` gains `isGuestFlow` backed by a `MutableStateFlow(initialGuest)` (`initialGuest: Boolean = false` constructor param, defaulted so existing callers compile) and `setGuestMode`; expose `var isMigrated` or read via the flow for assertions.
  - DI: `single<CloudSyncRepository> { get<DataMigrationRepository>() }`, `singleOf(::LocalDataRepositoryImpl) { bind<LocalDataRepository>() }`, `factoryOf` for the three new use cases and `SyncUseCases`.

- [ ] **Step 1: Write the failing tests**

`CompleteSignInUseCaseTest`:
- `setsGuestMode_afterSync` — `invoke(isGuest = true)`; `calls == ["download"]`; `isGuestFlow.first() == true`.
- `syncFails_stillSetsGuestMode` — `downloadError = RuntimeException("boom")`; `invoke(false)` does not throw; `isGuestFlow.first() == false` after starting from `initialGuest = true`.
- `cancellation_propagates_andSkipsGuestMode` — `downloadError = CancellationException()`; `assertThrows`/`runCatching` shows `CancellationException`; guest flag unchanged from `initialGuest = true`.

`MigrateLocalDataUseCaseTest`:
- `runsUploadThenDownload_thenMarksComplete` — `calls == ["upload", "download"]`; `isMigratedFlow.first() == true`.
- `uploadFails_doesNotMarkComplete` — `uploadError = RuntimeException()`; invoke throws; `calls == ["upload"]`; `isMigratedFlow.first() == false`.

`MigrationViewModelTest`: rebuild on `SyncUseCases(MigrateLocalDataUseCase(fakeCloudSync, fakeSettings))` and the real `SettingsUseCases` over `FakeSettingsRepository` (replace MockK of `DataMigrationRepository`). Keep the existing test cases' assertions (done flag, `R.string.migration_error` on failure, skip marks complete).

- [ ] **Step 2: Run the new tests to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*CompleteSignInUseCaseTest" --tests "*MigrateLocalDataUseCaseTest" --tests "*MigrationViewModelTest"`
Expected: FAIL (unresolved references).

- [ ] **Step 3: Implement the interfaces, implementations, use cases, `MigrationViewModel` change and DI per the Interfaces block**

- [ ] **Step 4: Run the full suite and the build**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL, all tests pass (≥ 48).

- [ ] **Step 5: Report files changed; controller commits** `feat(F03): cloud sync, local data and guest mode behind domain interfaces`

---

### Task 2: Sign-out through the domain

**Files:**
- Modify: `domain/usecase/auth/SignOutUseCase.kt`
- Modify: `domain/repository/AuthRepository.kt`, `data/local/repository/AuthRepositoryImpl.kt` (add `signOut()`)
- Modify: `presentation/screen/expensepreset/ExpensePresetStateEvents.kt`, `ExpensePresetViewModel.kt`, `ExpensePresetRoute.kt` (Route, `MainContent`, 3 previews)
- Modify (test): `domain/repository/FakeAuthRepository.kt`
- Create (test): `domain/usecase/SignOutUseCaseTest.kt`
- Modify (test): `presentation/screen/expensepreset/ExpensePresetViewModelTest.kt`

**Interfaces:**
- Consumes (Task 1): `LocalDataRepository`, `FakeLocalDataRepository`, `SettingsRepository.isGuestFlow`, `FakeSettingsRepository(initialGuest)`; (Task 1): `FakeAuthRepository` (add `calls` here).
- Produces:
  - `AuthRepository.signOut(): suspend fun signOut()` — `firebaseAuth.signOut()`.
  - `class SignOutUseCase(auth: AuthRepository, settings: SettingsRepository, localData: LocalDataRepository) { suspend operator fun invoke() }` — `auth.signOut()`, then if `!settings.isGuestFlow.first()` → `localData.clearAll()`. No `AppDatabase`, `SettingsDataStore` or `Dispatchers` import (the impl handles IO).
  - `Event.SignOut` becomes `data object SignOut : Event`.
  - `ExpensePresetViewModel.signOutUser()` takes no argument; failure path unchanged (log, `signedOut` stays false).
  - `FakeAuthRepository.signOut()` appends `"signOut"` to `calls`; `var signOutError: Exception? = null` thrown if set.

- [ ] **Step 1: Write the failing tests**

`SignOutUseCaseTest`:
- `accountUser_signsOutThenClearsLocalData` — `initialGuest = false`; after invoke `authFake.calls == ["signOut"]`, `localFake.clearCount == 1`.
- `guest_signsOut_keepsLocalData` — `initialGuest = true`; `calls == ["signOut"]`, `clearCount == 0`.
- `signOutFails_doesNotClear` — `signOutError = RuntimeException()`; invoke throws; `clearCount == 0`.

`ExpensePresetViewModelTest`: update the sign-out tests to send `Event.SignOut` (no argument) and build `SignOutUseCase` from the fakes; keep the assertions (`signedOut` true on success, false on failure).

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*SignOutUseCaseTest" --tests "*ExpensePresetViewModelTest"`
Expected: FAIL.

- [ ] **Step 3: Implement per the Interfaces block; remove every `FirebaseAuth` from the ExpensePreset files**

- [ ] **Step 4: Check the sign-out slice is clean**

Run: `grep -rn "com.google.firebase\|com.google.android.gms\|com.luna.budgetapp.data" app/src/main/java/com/luna/budgetapp/presentation/screen/expensepreset app/src/main/java/com/luna/budgetapp/domain/usecase/auth/SignOutUseCase.kt`
Expected: no output.

- [ ] **Step 5: Run the full suite and the build**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL, all tests pass.

- [ ] **Step 6: Report files changed; controller commits** `feat(F03): sign-out through the domain`

---

### Task 3: Suspend auth API with domain errors; AuthViewModel and AuthRoute off Firebase; final leak check

**Files:**
- Create: `domain/model/AuthException.kt`
- Modify: `domain/repository/AuthRepository.kt`, `data/local/repository/AuthRepositoryImpl.kt`
- Create: `data/local/repository/AuthErrorMapper.kt` (or a private-but-`internal` function in `AuthRepositoryImpl.kt`; your call)
- Modify: `domain/usecase/auth/SignInGoogleUseCase.kt`, `SignInEmailPasswordUseCase.kt`, `SignUpUseCase.kt`
- Modify: `presentation/screen/auth/AuthViewModel.kt`, `presentation/screen/auth/AuthRoute.kt`
- Modify: `di/appModule.kt` only if constructor changes require it (`viewModelOf` resolves by type)
- Modify (test): `domain/repository/FakeAuthRepository.kt`
- Create (test): `data/AuthErrorMapperTest.kt` (package matching the mapper)
- Modify (test): `presentation/screen/auth/AuthViewModelTest.kt`

**Interfaces:**
- Consumes (Task 1): `AuthUseCases.isSignedIn`, `AuthUseCases.completeSignIn`, `FakeCloudSyncRepository`, `FakeSettingsRepository.isGuestFlow`; (Task 2): `SignOutUseCase(auth, settings, localData)`, `FakeAuthRepository.calls` / `signOutError`, `FakeLocalDataRepository`. Keep `signOut()` in the rewritten `AuthRepository` and `FakeAuthRepository`.
- Produces:
  - `sealed class AuthException : Exception() { object InvalidEmail; object UserNotFound; object InvalidCredentials; class Unknown(override val cause: Throwable?) }` (use `data object` where it compiles).
  - `AuthRepository`: `suspend fun signUp(email: String, password: String)`, `suspend fun signInEmailPassword(email: String, password: String)`, `suspend fun signInGoogle(idToken: String)` — return `Unit`, throw on failure. No `Task`/`AuthResult` imports.
  - `internal fun Exception.toAuthException(): Exception` — the spec's mapping table exactly:
    - `CancellationException` → returned as is (caller rethrows);
    - `IllegalArgumentException` → as is;
    - `FirebaseAuthWeakPasswordException` → `Unknown(e)` (checked before the next line);
    - `FirebaseAuthInvalidCredentialsException`: `errorCode == "ERROR_INVALID_EMAIL"` → `InvalidEmail`, else `InvalidCredentials`;
    - `FirebaseAuthInvalidUserException`: `errorCode == "ERROR_USER_NOT_FOUND"` → `UserNotFound`, else `Unknown(e)`;
    - anything else → `Unknown(e)`.
  - `AuthRepositoryImpl` methods: `try { firebaseAuth.xxx(...).await() } catch (e: Exception) { throw e.toAuthException() }` (rethrow `CancellationException` untouched).
  - Use cases: `suspend operator fun invoke(...)` mirroring the repository signatures.
  - `AuthViewModel(authUseCases: AuthUseCases)`:
    - `val isUserSignedIn: Boolean = authUseCases.isSignedIn()`
    - `handleSignInSuccess(isGuest)` → `authUseCases.completeSignIn(isGuest)` then `_signedIn.value = true`
    - sign-in/up paths call the use case then `handleSignInSuccess(false)`, catching into `showError`
    - `showError` mapping: `NoCredentialException` → `auth_error_no_google_account`; `IllegalArgumentException` → `auth_error_invalid_input`; `InvalidEmail` → `auth_error_invalid_email`; `UserNotFound` → `auth_error_user_not_found`; `InvalidCredentials` → `auth_error_invalid_credentials`; else → `auth_error_generic`. Remove `resolveInvalidAuthMessage`.
    - Google credential unwrapping (`CustomCredential` / `GoogleIdTokenCredential`) stays.
  - `AuthRoute`: remove `FirebaseAuth`; `isUserSignedIn = viewModel.isUserSignedIn`.
  - `FakeAuthRepository`: new suspend members; `var error: Exception? = null` thrown by the sign-in/up members; `val calls: MutableList<String>` (`"signUp"`, `"signInEmail"`, `"signInGoogle"`).

- [ ] **Step 1: Write the failing tests**

`AuthErrorMapperTest` (construct real Firebase exceptions: `FirebaseAuthInvalidCredentialsException(code, msg)`, `FirebaseAuthInvalidUserException(code, msg)`, `FirebaseAuthWeakPasswordException(code, msg, reason)`; if any constructor can't run on the JVM, extract the mapping to a function of `(kind, errorCode)` and test that instead, and say so in the report):
- `invalidEmail` → `ERROR_INVALID_EMAIL` maps to `AuthException.InvalidEmail`
- `invalidCredential` → `ERROR_INVALID_CREDENTIAL` and `ERROR_WRONG_PASSWORD` map to `InvalidCredentials`
- `userNotFound` → `ERROR_USER_NOT_FOUND` maps to `UserNotFound`
- `userDisabled` → `ERROR_USER_DISABLED` maps to `Unknown`
- `weakPassword` → maps to `Unknown`
- `cancellation_passesThrough` → a `CancellationException` comes back as the same instance
- `other` → `RuntimeException` maps to `Unknown` with that cause

`AuthViewModelTest` (rewrite on `AuthUseCases` built from real use cases over `FakeAuthRepository`, `FakeCloudSyncRepository`, `FakeSettingsRepository`, plus `FakeLocalDataRepository` for `SignOutUseCase`; no MockK):
- keep `signInSuccess_setsSignedIn_andSavesGuestMode` and `signInSuccess_whenSyncThrows_stillSetsSignedIn` (assert via fakes)
- `emailSignIn_success_setsSignedIn` — `calls == ["signInEmail"]`, `signedIn` true, guest flag false
- `emailSignIn_invalidCredentials_showsInvalidCredentialsDialog` — `error = AuthException.InvalidCredentials`; `dialogState == DialogState.ErrorMessage(R.string.auth_error_invalid_credentials)`; `signedIn` false
- `emailSignIn_userNotFound_showsUserNotFound` → `R.string.auth_error_user_not_found`
- `emailSignIn_invalidEmail_showsInvalidEmail` → `R.string.auth_error_invalid_email`
- `signUp_unknownError_showsGeneric` — `error = AuthException.Unknown(null)` → `R.string.auth_error_generic`
- `emailSignIn_illegalArgument_showsInvalidInput` → `R.string.auth_error_invalid_input`
- `isUserSignedIn_readOnceAtConstruction` — `FakeAuthRepository(signedIn = true)`; build VM; set `signedIn = false`; `vm.isUserSignedIn` is still `true` (Review Focus 1)

- [ ] **Step 2: Run them to verify they fail**

Run: `./gradlew testDebugUnitTest --tests "*AuthErrorMapperTest" --tests "*AuthViewModelTest"`
Expected: FAIL (unresolved references).

- [ ] **Step 3: Implement per the Interfaces block**

- [ ] **Step 4: Final leak check (spec "Done when")**

Run: `grep -rn "com.google.firebase\|com.google.android.gms\|com.luna.budgetapp.data" app/src/main/java/com/luna/budgetapp/presentation app/src/main/java/com/luna/budgetapp/domain`
Expected: no output.

- [ ] **Step 5: Run the full suite and the build**

Run: `./gradlew testDebugUnitTest assembleDebug`
Expected: BUILD SUCCESSFUL, all tests pass, count above 43.

- [ ] **Step 6: Report files changed and the final test count; controller commits** `feat(F03): suspend auth API with domain AuthException; no Firebase in presentation or domain`

---

## After the tasks (controller)

- Final whole-branch review, then tracker: F03 → Features-done once merged; P26 Next step unblocked.
- Device check for the user (spec "Done when"): sign in with email, Google and as guest; relaunch while signed in (auto sign-in once); sign out as account user (data gone) and as guest (data kept); wrong password shows the invalid-credentials dialog.
