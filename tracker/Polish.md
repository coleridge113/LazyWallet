# Polish

Cosmetic fixes. Sessions work on these only when the user asks. One sitting, no design decision, a few files. Bigger → a feature. Fixed/Won't fix rows move to Polish-done.md.

| ID | Issue | Area | Status | Found | Notes |
|---|---|---|---|---|---|
| P01 | `res/xml/data_extraction_rules.xml` still has the template TODO about backup include/exclude | Manifest/backup | Open | 2026-11-25 survey | Decide what Room/DataStore data should be backed up |
| P06 | Bump lifecycle 2.10.0 → 2.11.1 and navigation-compose 2.9.8 → 2.10.2 (latest stable as of 2026-11-25) after reading release notes | `gradle/libs.versions.toml` | Open | 2026-11-25, MVVM research | Blocked by P22: (was status 'Blocked (paging done)'). Paging 3.4.2 → 3.5.3 done 2026-11-25 (27/27 tests, build ok). Navigation 2.10.2 and lifecycle 2.11.x blocked: navigation 2.10.2 pulls lifecycle 2.11.0 transitively, and both need compileSdk 37 + AGP ≥ 9.2.0 (`checkDebugAarMetadata` fails on 36). Unblock with a build-tools upgrade ticket (user to decide) |
| P22 | Build tools upgrade so blocked library bumps can land: AGP 9.1.1 → ≥ 9.2.0 and compileSdk 36 → 37 (install platform 37), then navigation-compose 2.9.8 → 2.10.2 and lifecycle 2.10.0 → 2.11.1 | `gradle/libs.versions.toml`, `app/build.gradle.kts`, Gradle wrapper if AGP requires | Open | 2026-11-25, P06 session | User: do later. Unblocks the rest of P06. Have android-researcher check AGP 9.2 release notes (Gradle/JDK/R8 requirements) first; Needs OK: build-tooling change |
| P31 | Budget outlook card: "₱X of ₱Y" sits below the 200dp gauge, leaving ~50dp empty inside the arc's open bottom; pull it up into the gap if it looks off on device | `budget/components/BudgetOutlookCard.kt` | Open | 2026-11-26, F12 final review | Needs a shorter box / circle drawn from width; watch large font scale. Decide after the F12 device check |
