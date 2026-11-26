# F03 Firebase out of presentation — design

Date: 2026-11-26 · Status: approved 2026-11-26 · Owner: Jose Luna · Branch: `feat/firebase-out-of-presentation` · Absorbs Polish ticket P03 (seed notes folded in here) · Unblocks P26

## Intent and why

Firebase and data-layer classes leak into the screens, the ViewModels and the domain layer. The UI builds `FirebaseAuth` objects and passes them through events; `AuthViewModel` matches on a Firebase exception and injects `DataMigrationRepository` and `SettingsDataStore` directly (so its test has to mock them); the domain `AuthRepository` hands back Firebase `Task<AuthResult>` objects through callbacks; and `SignOutUseCase` takes a `FirebaseAuth` and reaches into `AppDatabase` and `SettingsDataStore`.

F03 makes `presentation/` and `domain/` Firebase-free and data-class-free, so the screens and ViewModels only know app concepts, the sign-in/sign-out rules become plain Kotlin testable with fakes, and P26 (a real Auth `uiState` with a loading flag) can be built on a clean base. This was an MVVM-research finding (2026-11-25) and continues the architecture cleanup of F02/P25/P27.

## Goals

- No `com.google.firebase` or `com.google.android.gms` import under `presentation/` or `domain/`.
- No `com.luna.budgetapp.data` import under `presentation/` or `domain/`.
- `AuthRepository` exposes plain suspend functions: they return on success and throw a domain `AuthException` on failure. No callbacks, no `Task`.
- The sign-in and sign-out rules live in use cases over small domain interfaces (approach A, below), unit-tested with fakes.
- Every working path behaves as it does today (see Behaviour to keep).

## Non-goals

- P26 (Auth `uiState`, loading flag, buttons disabled during sign-in). F03 only makes it possible.
- Removing or rewriting the JWT methods (`refreshJwtToken`, `fetchJwtToken`, `GetTokenUseCase`) for the disconnected backend. They hold no Firebase types; leave them.
- Firebase use inside `data/` repositories (`auth.currentUser?.uid` for Firestore paths). Data is allowed to know Firebase.
- Google Credential Manager (`presentation/screen/utils/LoginUtils.kt`, `GoogleIdTokenCredential` / `CustomCredential` handling in `AuthViewModel`). That is the Android sign-in picker, not Firebase, and it needs an Activity context; it stays in presentation. (`com.google.android.libraries.identity` is not `com.google.android.gms`.)
- Surfacing sign-out failures to the user (→ F11).
- Any visible UI change.

## Anti-goals

- A behaviour change hidden inside a refactor: a guest losing local data on sign-out, a signed-in user's data surviving sign-out, a failed cloud sync blocking sign-in, or an error message changing.
- Error codes from Firebase reaching the ViewModel as strings.

## Current state (code, 2026-11-26)

| Where | Leak |
|---|---|
| `presentation/screen/auth/AuthRoute.kt` | `FirebaseAuth.getInstance()`; `remember { auth.currentUser != null }` drives auto sign-in (`AuthContent` fires `Event.HandleSignInSuccess(false)` when true). |
| `presentation/screen/auth/AuthViewModel.kt` | Injects `DataMigrationRepository`, `SettingsDataStore`. `handleSignInSuccess` calls `migrationRepository.syncFromCloud()` (failure logged, swallowed) then `settings.setGuestMode(isGuest)` in `finally`, then sets `signedIn`. Sign-in/up use cases take `onSuccess(Task)`/`onFailure(Exception)` callbacks; VM checks `task.isSuccessful`. `showError` matches `FirebaseAuthInvalidCredentialsException` and maps `errorCode` (`ERROR_INVALID_EMAIL`, `ERROR_USER_NOT_FOUND`, else invalid credentials). |
| `presentation/screen/expensepreset/` | Route creates `FirebaseAuth` (plus 3 previews), passes it via `MainContent(auth)` into `Event.SignOut(auth: FirebaseAuth)`; VM `signOutUser(auth)` calls `authUseCases.signOut(auth)`. |
| `presentation/screen/migration/MigrationViewModel.kt` | Injects `DataMigrationRepository`; runs `performMigration()`, `syncFromCloud()`, then `settingsUseCases.setMigrationComplete()`. Skip calls only `setMigrationComplete()`. |
| `domain/repository/AuthRepository.kt` | `signUp`/`signInEmailPassword`/`signInGoogle` take `Task<AuthResult>` callbacks. |
| `domain/usecase/auth/SignIn*UseCase`, `SignUpUseCase` | Pass the callbacks through; import Firebase/gms types. |
| `domain/usecase/auth/SignOutUseCase.kt` | `invoke(auth: FirebaseAuth)`: `auth.signOut()`, then on IO, if `!settings.isGuestFlow.first()`, `db.clearAllTables()`. |
| `di/appModule.kt` | `single { FirebaseAuth.getInstance() }` (keep); `singleOf(::DataMigrationRepository)`; `factoryOf(::SignOutUseCase)`. |

`kotlinx.coroutines.tasks.await` is already on the classpath (used in `CategoryFilterRepositoryImpl`).

## Approaches considered

- **A. Use cases orchestrate over small repositories (chosen).** `AuthRepository` only talks to Firebase auth. New domain interfaces cover cloud sync and clearing local data; guest mode joins `SettingsRepository`. Use cases chain the steps, so the rules ("guests keep their data", "sync failure doesn't block sign-in") are plain Kotlin tested with fakes. Matches how the app's other use cases are built. Cost: two new interfaces and their fakes.
- **B. Fat repository.** `AuthRepositoryImpl.signOut()` clears the DB and checks guest mode itself; sign-in syncs inside the repository; use cases pass through. Fewer types, but the rules sit in `data/` and are only testable against Firebase, Room and DataStore, and the use cases stay empty. Rejected.
- **Presentation-only cleanup.** Meets the seed's import check but leaves `Task<AuthResult>` in the domain interface, so the ViewModel still checks `task.isSuccessful`. Rejected by Jose (2026-11-26) in favour of making the domain Firebase-free too.

## Design

### Domain errors

`domain/model/AuthException.kt`: a sealed class extending `Exception`:

- `InvalidEmail`, `UserNotFound`, `InvalidCredentials`, `Unknown(cause: Throwable?)`.

`AuthRepositoryImpl` maps Firebase failures: `FirebaseAuthInvalidCredentialsException` / `FirebaseAuthInvalidUserException` by `errorCode` — `ERROR_INVALID_EMAIL` → `InvalidEmail`, `ERROR_USER_NOT_FOUND` → `UserNotFound`, other invalid-credential codes → `InvalidCredentials`; anything else → `Unknown(e)`. `CancellationException` is rethrown untouched. `IllegalArgumentException` (Firebase throws it for empty email/password) is rethrown as is, so the VM's existing `IllegalArgumentException → auth_error_invalid_input` mapping keeps working.

`AuthViewModel.showError` maps: `NoCredentialException` → `auth_error_no_google_account` (unchanged, Credential Manager), `IllegalArgumentException` → `auth_error_invalid_input`, `AuthException.InvalidEmail` → `auth_error_invalid_email`, `UserNotFound` → `auth_error_user_not_found`, `InvalidCredentials` → `auth_error_invalid_credentials`, else → `auth_error_generic`. `resolveInvalidAuthMessage` goes away.

Today `ERROR_USER_NOT_FOUND` arrives as `FirebaseAuthInvalidUserException`, which the current VM does not match (it falls to generic). Mapping it to `UserNotFound` makes "User not found" reachable as P23 intended. This is the one intended message change. (If the Firebase project has email-enumeration protection on, sign-in returns `ERROR_INVALID_CREDENTIAL` instead, so "User not found" may still not appear in practice; that's fine.)

### Domain interfaces

```kotlin
interface AuthRepository {
    suspend fun refreshJwtToken(): String      // unchanged
    suspend fun fetchJwtToken(): String?       // unchanged
    suspend fun signUp(email: String, password: String)
    suspend fun signInEmailPassword(email: String, password: String)
    suspend fun signInGoogle(idToken: String)
    suspend fun signOut()
    fun isSignedIn(): Boolean
}

interface CloudSyncRepository {          // implemented by DataMigrationRepository
    suspend fun uploadLocalData()        // today's performMigration()
    suspend fun downloadCloudData()      // today's syncFromCloud()
}

interface LocalDataRepository {
    suspend fun clearAll()               // AppDatabase.clearAllTables() on Dispatchers.IO
}

interface SettingsRepository {           // existing; add:
    val isGuestFlow: Flow<Boolean>
    suspend fun setGuestMode(isGuest: Boolean)
}
```

Implementations: `AuthRepositoryImpl` uses `.await()` on the Firebase tasks and maps errors; `signOut()` calls `firebaseAuth.signOut()`; `isSignedIn()` is `firebaseAuth.currentUser != null`. `DataMigrationRepository` implements `CloudSyncRepository` (rename the two methods or delegate; builder's call). `LocalDataRepositoryImpl` in `data/local/repository/` wraps `AppDatabase`. `SettingsRepositoryImpl` delegates the two new members to `SettingsDataStore`.

### Use cases

| Use case | Does | Aggregate |
|---|---|---|
| `SignInGoogleUseCase(idToken)`, `SignInEmailPasswordUseCase(email, password)`, `SignUpUseCase(email, password)` | Call the repository; suspend, throw on failure. | `AuthUseCases` (existing) |
| `SignOutUseCase()` | `auth.signOut()`, then if `!settings.isGuestFlow.first()` → `localData.clearAll()`. Same order as today. | `AuthUseCases` |
| `IsSignedInUseCase()` | `auth.isSignedIn()` | `AuthUseCases` (new member) |
| `CompleteSignInUseCase(isGuest)` | `cloudSync.downloadCloudData()` with failure logged and swallowed (rethrow `CancellationException`), then always `settings.setGuestMode(isGuest)`. | `AuthUseCases` (new member) |
| `MigrateLocalDataUseCase()` | `uploadLocalData()`, `downloadCloudData()`, `settings.setMigrationComplete()` (throws on failure, as today) | `SyncUseCases` (new aggregate: `migrateLocalData`) |

Logging in a domain use case: `CompleteSignInUseCase` needs to log the swallowed sync failure. `android.util.Log` is fine here (domain already runs on Android; no pure-JVM module), but in unit tests `Log` returns defaults (`isReturnDefaultValues = true`), so it won't crash.

### Presentation

- `AuthViewModel(authUseCases)`: no data imports. `val isUserSignedIn: Boolean = authUseCases.isSignedIn()` read once at construction (replaces `remember { auth.currentUser != null }` in the Route; P26 will fold it into `uiState`). Sign-in methods become `try { authUseCases.signInX(...); handleSignInSuccess(false) } catch (CancellationException) { throw } catch (e: Exception) { showError(e) }`. `handleSignInSuccess(isGuest)` = `authUseCases.completeSignIn(isGuest)` then `_signedIn.value = true` (keep the "always navigates even if sync fails" behaviour).
- `AuthRoute`: drop `FirebaseAuth`; pass `viewModel.isUserSignedIn` to `AuthContent`. Keep the `remember`-style one-time read semantics (the F02 fix 976aa2a: the auto sign-in must not re-fire on recomposition).
- `ExpensePreset`: `Event.SignOut` becomes a `data object`; remove `auth` from `ExpensePresetRoute`, `MainContent` and the previews; `signOutUser()` calls `authUseCases.signOut()`; failure handling unchanged (log, stay on screen).
- `MigrationViewModel(syncUseCases, settingsUseCases)`: done path calls `syncUseCases.migrateLocalData()`; skip keeps `settingsUseCases.setMigrationComplete()`.
- `di/appModule.kt`: bind `DataMigrationRepository` to `CloudSyncRepository`, add `LocalDataRepositoryImpl` bound to `LocalDataRepository`, register the new use cases and `SyncUseCases`; keep `single { FirebaseAuth.getInstance() }` for data.

### Tests

- New fakes in `app/src/test/.../domain/repository/`: `FakeAuthRepository` (configurable failure to throw, records calls, `signedIn` flag), `FakeCloudSyncRepository` (can throw), `FakeLocalDataRepository` (records `clearAll`); extend `FakeSettingsRepository` with guest mode.
- Use case tests: `SignOutUseCase` clears data for a non-guest and keeps it for a guest; `CompleteSignInUseCase` sets guest mode even when sync throws and rethrows cancellation; `MigrateLocalDataUseCase` runs the three steps in order and does not mark complete when upload throws.
- `AuthRepositoryImpl` error mapping: a JVM test needs Firebase exception instances; if that's impractical, extract the mapping into a pure function taking the error code and test that. Builder's call.
- Rewrite `AuthViewModelTest` and `MigrationViewModelTest` on the fakes (no MockK of data classes); `AuthViewModel` maps each `AuthException` to its string id; `isUserSignedIn` reflects the repository. Update `ExpensePresetViewModelTest` for `Event.SignOut` without an argument.

### Behaviour to keep

| Path | Must stay |
|---|---|
| App launch, already signed in | Auto sign-in fires once, syncs, navigates. Not again on recomposition. |
| Guest sign-in | Sets guest mode; navigates. |
| Sync fails during sign-in | Logged; guest mode still set; still navigates. |
| Sign-out, account user | Firebase sign-out, local DB cleared, back to Auth with the back stack cleared (P29). |
| Sign-out, guest | Local DB kept. |
| Sign-out throws | Logged; user stays on screen (F11 will add feedback). |
| Wrong password / bad email / no Google account / empty field | Same dialogs and strings as today. |
| Migration done / skip | Same as today (screen still unregistered; F09). |

## Done when

- `grep -rn "com.google.firebase\|com.google.android.gms\|com.luna.budgetapp.data" app/src/main/java/com/luna/budgetapp/presentation app/src/main/java/com/luna/budgetapp/domain` returns nothing.
- Unit tests pass (count goes up from 43), `assembleDebug` succeeds.
- Device check: sign in with email, with Google, as guest; relaunch while signed in (auto sign-in once); sign out as account user (data gone) and as guest (data kept); wrong password shows the invalid-credentials dialog.

## Clarifications (builder check, 2026-11-26)

- **`isUserSignedIn` as a ViewModel field is equivalent to today's `remember`.** `AuthViewModel` is obtained with `koinViewModel()` inside `composable<Routes.AuthRoute>` (`NavGraph.kt:122`), so it is scoped to the Auth back-stack entry. Sign-in pops Auth (`popUpTo(AuthRoute) { inclusive = true }`); sign-out navigates to a fresh Auth entry after clearing the graph (P29). Each visit gets a new ViewModel, so the one-time read is fresh each time.
- **Guests have no Firebase user.** Guest sign-in only calls `HandleSignInSuccess(true)`; there is no Firebase call. `SignOutUseCase` still calls `auth.signOut()` for a guest (a no-op), as today. Guest-to-account upgrade is F09, not F03; nothing about that transition changes here.
- **Settings scope (my call):** add only `isGuestFlow` and `setGuestMode` to `SettingsRepository`; update `SettingsRepositoryImpl` and `FakeSettingsRepository`. Other `SettingsDataStore` users in `data/` stay as they are.
- **Cancellation (my call):** no `NonCancellable`. `CompleteSignInUseCase` catches non-cancellation sync failures (log, continue), then calls `setGuestMode`; a `CancellationException` propagates and skips it. Today's `finally` runs `setGuestMode` on a cancelled coroutine, where the DataStore edit throws immediately, so this is the same effective behaviour. `MigrateLocalDataUseCase` lets every failure propagate (the ViewModel already catches and shows `migration_error`).
- **Error mapping in full (my call):**

  | Firebase exception | Code | `AuthException` |
  |---|---|---|
  | `FirebaseAuthInvalidCredentialsException` | `ERROR_INVALID_EMAIL` | `InvalidEmail` |
  | `FirebaseAuthInvalidCredentialsException` | any other (`ERROR_INVALID_CREDENTIAL`, `ERROR_WRONG_PASSWORD`, unknown) | `InvalidCredentials` |
  | `FirebaseAuthInvalidUserException` | `ERROR_USER_NOT_FOUND` | `UserNotFound` |
  | `FirebaseAuthInvalidUserException` | any other (e.g. `ERROR_USER_DISABLED`) | `Unknown(e)` |
  | anything else (`FirebaseAuthUserCollisionException`, `FirebaseAuthWeakPasswordException`, `FirebaseNetworkException`, …) | — | `Unknown(e)` → generic message, as today |

  `FirebaseAuthWeakPasswordException` extends `FirebaseAuthInvalidCredentialsException`, so check it first and send it to `Unknown` to keep today's generic message. Better messages for collision, weak password and network are out of scope.

## Left to the builder

- Method names on `DataMigrationRepository` (rename vs delegate), file names, where `LocalDataRepositoryImpl` lives within `data/local/repository/`.
- How to test the Firebase error mapping (direct vs extracted function).
- Task split and order in the plan.
