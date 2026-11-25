# Plan

Lazy Wallet: a fast, offline-first budgeting app (Android) that syncs to Firebase. One task per fresh session on the task model; setup, planning and reviews on the planning model (models to be confirmed with the user).

## How a session works
1. Read this Plan, the newest 3 rows of Progress.md, and all of Learnings.md. Learnings override the project instructions where they conflict.
2. Take the task the user named, or else the first Ready task whose dependencies are Done. Set it to In progress before starting.
3. Do only that task, inside its Scope. Read the matching part of Spec.md. Where Needs OK says Yes, stop before that step and ask.
4. Check the result against Done when and the checks in `lazy-wallet-instructions.md`.
5. Add one row at the top of Progress.md: date, task ID, what changed, what's not live yet, what's left. Set the task to Done, or to Blocked with the reason.
6. Add any new gotcha to Learnings.md (task sessions don't edit the project instructions).
7. Stop. Tell the user what was done and which task is next.

New work found along the way becomes a new row below, not extra work now.

Status: Ready · In progress · Your call · Blocked · Done · Later.

| ID | Task | Status | Depends on | Scope | Done when | Needs OK |
|---|---|---|---|---|---|---|
| T01 | ~~Your call: finish and merge `feat/expense-search`~~ merged into master via PR #2 (search query + expandable FAB, 2 commits ahead of master, no tests for search yet) | Done | – | Branch `feat/expense-search` | User decides: add tests, then merge or park | Yes: merging |

| T02 | Verify the unchecked Figma screens (Auth sign in, Cloud sync, Expenses search/date menu, Budget create/delete dialogs) with screenshots; fix issues found | Blocked | – | Figma file only (see Spec §5.1); no app code | Each screen screenshotted and matches the Compose source | No |
| T03 | Fix known Figma gaps: Budget Details category icons (misshapen at 12px), BudgetCard missing category chips and bottom space | Blocked | T02 | Figma file only | Screenshots show icons and chips correct | No |
| T04 | Add missing Figma screens: month/year picker, date-range picker, loading/error states, real `lazy_wallet_logo` | Later | T02 | Figma file only | Screens exist, built from components | No |
| T05 | Your call: build a Light theme in Figma (Starter plan = 1 mode per collection; use the separate "Theme Light" collection) | Your call | T02 | Figma file only | User decides whether it's wanted and how to swap collections | No |

T02 and T03 are Blocked: the Figma Starter plan's MCP call limit was hit on 2026-11-25. Resume when it resets or the plan is upgraded.

New work goes in as a new row with the next ID. Tasks to be discussed with the user.
