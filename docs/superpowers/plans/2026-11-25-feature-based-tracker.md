# Feature-based Tracker Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the monolithic `tracker/Plan.md` + growing `Polish.md`/`Spec.md` with a feature index, per-feature Superpowers specs/plans, and archives, in Lazy Wallet and in the global skills/agent so every project uses it.

**Architecture:** A small checker script defines the layout's invariants and is the "test" for every task. Lazy Wallet is migrated first (so the checker has a real tracker to pass), then the global agent and skills are rewritten to read and write the new layout.

**Tech Stack:** Markdown, Python 3 (checker, stdlib only), Bash, Claude Code agents/skills/hooks.

**Spec:** `docs/superpowers/specs/2026-11-25-feature-based-tracker-design.md`

## Global Constraints

- Work in place on branch `polish/presentation-fixes`; never create a worktree (gitignored `google-services.json`, `local.properties`).
- Implementers (`task-runner`, Sonnet) never commit; the Opus controller commits repo files after each task review, staging only the files the implementer listed. Files under `~/.claude-personal/` are not in git: no commit step for them.
- `task-runner-guard.sh` is not modified.
- Title-case file names only: `Spec.md`, `Features.md`, `Features-done.md`, `Polish.md`, `Polish-done.md`, `Progress.md`, `Progress-archive.md`, `Learnings.md`, `HANDOVER.md`.
- Feature statuses: `Idea · Designing · Planned · Building · Blocked · Done · Later`. Polish statuses: `Open · In progress · Fixed · Won't fix`. Only Open / In progress rows stay in `Polish.md`.
- Feature specs/plans: `docs/superpowers/specs/YYYY-MM-DD-<slug>-design.md`, `docs/superpowers/plans/YYYY-MM-DD-<slug>.md`, same slug.
- Nothing may depend on Superpowers-only state; specs/plans are plain Markdown that can be written by hand.
- Absolute dates (2026-11-25), never "today".

## Review Focus

1. **An ID lost or duplicated in migration** (P01–P22, T01–T05): every ID must appear exactly once across Features/Features-done/Polish/Polish-done. → checker `ids` rule, Task 2.
2. **A Features.md link to a spec/plan file that doesn't exist** (slug typo): must be reported. → checker `links` rule, Task 1 test.
3. **A Done/Fixed row left in an open list**: must be reported. → checker `open-only` rule, Task 1 test.
4. **handover resuming a project still on the old layout** (`Plan.md` or `PLAN.md`, no `Features.md`): must keep working and offer migration, not crash or write new files silently. → Task 4 step "old-layout scenario".
5. **task-runner in plan-task mode trying to commit**: still denied. → Task 3 re-runs the guard tests.

---

### Task 1: Tracker checker

**Files:**
- Create: `~/.claude-personal/scripts/check_tracker.py`
- Create (fixtures): `~/.claude-personal/scripts/tests/fixtures/{good,bad}/tracker/*.md`, `~/.claude-personal/scripts/tests/test_check_tracker.py`

**Interfaces:**
- Produces: `python3 ~/.claude-personal/scripts/check_tracker.py <project_root>` → exit 0 and prints `OK` when valid; exit 1 and prints one `RULE: message` line per problem. Rules: `layout` (Features.md, Polish.md, Spec.md, Progress.md, Learnings.md exist; Plan.md/PLAN.md absent), `ids` (each `F\d\d`, `P\d\d`, `T\d\d` row ID appears as a table row ID in exactly one of Features.md, Features-done.md, Polish.md, Polish-done.md), `open-only` (Polish.md statuses ∈ {Open, In progress}; Features.md `F` rows' statuses ∈ feature statuses minus Done; P/T rows inside seed notes are not status-checked), `links` (every `docs/superpowers/...md` path in Features.md / Features-done.md exists relative to project root), `progress-size` (Progress.md has ≤ 50 data rows).
- A table row ID is the first cell of a Markdown table row matching `^\| ([FPT]\d\d) \|`.

- [ ] **Step 1: Write failing tests** — `test_good_fixture_passes` (exit 0, stdout `OK`); `test_bad_fixture_reports_each_rule` asserting stdout contains `layout:` (fixture has Plan.md), `ids:` (P03 in both Polish.md and Polish-done.md), `open-only:` (a `Fixed` row in Polish.md), `links:` (Features.md links `docs/superpowers/specs/2026-01-01-missing-design.md`), `progress-size:` (51 rows). Run with `python3 -m unittest discover ~/.claude-personal/scripts/tests` → FAIL (script missing).
- [ ] **Step 2: Implement `check_tracker.py`** (stdlib only; `main(argv) -> int`).
- [ ] **Step 3: Run tests** → PASS. Then run it on Lazy Wallet: `python3 ~/.claude-personal/scripts/check_tracker.py /Users/joseluna/dev/personal/budget/android` → expected FAIL with `layout:` (Plan.md still present) and `open-only:` lines — this is Task 2's failing test.

### Task 2: Migrate Lazy Wallet's tracker

**Files:**
- Create: `tracker/Features.md`, `tracker/Features-done.md`, `tracker/Polish-done.md`
- Modify: `tracker/Polish.md`, `tracker/Spec.md`, `tracker/Progress.md`
- Delete: `tracker/Plan.md` (`git rm`)

**Interfaces:** Consumes Task 1's checker. Produces the files Tasks 3–5 describe.

- [ ] **Step 1:** Checker on the repo fails (from Task 1 Step 3).
- [ ] **Step 2: Features.md** — lead line, status legend, table `| ID | Feature | Status | Spec | Plan | Next step |`, rows:
  - F01 Figma design file — Blocked — Next: "Figma MCP rate limit; resume T02 when it resets" — seed notes: T02, T03, T04, T05 task text (verbatim, each with its old ID) + Spec §5.1 known-issues bullets.
  - F02 Navigation events — Idea — seed notes: P02 text.
  - F03 Firebase out of presentation — Idea — seed notes: P03 text.
  - F04 Theme, contrast & light theme — Idea — Next: "Your call: does the app need a tuned light theme?" — seed notes: P14 text; link to F01's T05.
  - F05 Screen chrome — Idea — Next: "Your call: P17 labels/Analysis tab, P18 snackbar undo; then brainstorm" — seed notes: P16, P17, P18 text.
  - F06 Test coverage — Idea — seed notes: P10 text.
  - F07 Adaptive layouts — Later — seed notes: P20 text.
  - F08 Feature-based tracker — Building — Spec/Plan: this design and this plan.
  - Footer: "New features get the next F ID. Seed notes are folded into the spec by brainstorming, then deleted here. Done rows move to Features-done.md."
  - Seed-note sections keep the old P/T IDs as table rows (`| P02 | … |`) so the `ids` rule finds them exactly once.
- [ ] **Step 3: Features-done.md** — lead + table `| ID | Feature | Done | Spec | Plan | Merge |` with T01 (pre-feature task, merged via PR #2, 2026-11-25).
- [ ] **Step 4: Polish.md / Polish-done.md** — Polish.md keeps P01, P06 (status `Open`, Notes start "Blocked by P22:"), P15, P19, P21, P22. Polish-done.md gets P04, P05, P07, P08, P09, P11, P12, P13 with status `Fixed` (extra status text moved to Notes). P02, P03, P10, P14, P16, P17, P18, P20 live only in Features.md seed notes. Polish.md lead adds: "One sitting, no design decision, a few files. Bigger → a feature. Fixed/Won't fix rows move to Polish-done.md."
- [ ] **Step 5: Spec.md** — sections per spec "Spec.md (overarching)": 1 What it is & current state (drop the stale "Current branch feat/expense-search" and test-thinness lines that are no longer true; keep rough edges), 2 Goal, 3 Architecture → `../../CLAUDE.md`, 4 Project-wide facts (Figma URL, pages, variable collections, components — not known issues), 5 Constraints and approver, 6 Accepted gaps (unchanged rows, "T05" → "F01"), 7 Feature specs pointer.
- [ ] **Step 6: Progress.md** — add top row `2026-11-25 | F08 | Tracker migrated to feature layout …`. (12 rows: no archive needed yet.)
- [ ] **Step 7:** `git rm tracker/Plan.md`; run checker → `OK`.
- [ ] **Step 8 (controller): Commit** `tracker/` + `docs/superpowers/plans/` — "chore: migrated tracker to feature-based layout (F08)".

### Task 3: task-runner plan-task mode

**Files:** Modify `~/.claude-personal/agents/task-runner.md`.

**Interfaces:** Consumes: SDD implementer brief (from `subagent-driven-development/implementer-prompt.md`). Produces: report fields `Status`, `Files changed` (exact paths), `Tests`, `Concerns`.

- [ ] **Step 1:** description: add "Also the implementer for Superpowers plan tasks (plan-task mode)"; "Never commits" stays.
- [ ] **Step 2:** Section 1 reads `Features.md` (or legacy `Plan.md` if no Features.md) and, for a feature, its spec/plan; ID handling covers F/P IDs; stops on statuses Done/Blocked/Later.
- [ ] **Step 3:** New "Plan-task mode" section, triggered when the prompt contains an implementer brief / a plan task: follow the brief; skip tracker pre-flight (ID lookup, clean-tree); keep branch and boundary rules; don't edit `tracker/`; never commit even if the brief says to — instead end with `Files changed:` listing every path; report in the brief's format.
- [ ] **Step 4: Verify** — re-run the 17 guard cases from the session's test loop (`t task-runner "git commit -m x"` → deny … `t "" "git commit -m x"` → allow) against unchanged `task-runner-guard.sh` → same results; `grep -n "Plan-task mode" task-runner.md` hits.

### Task 4: handover skill

**Files:** Modify `~/.claude-personal/skills/handover/SKILL.md`.

- [ ] **Step 1:** Description: replace "same Plan, Polish, … tabs" with the new files; drop "Local stand-in for the project-tracker skill's Claude Doc".
- [ ] **Step 2:** Files table → Title-case files of the new layout + `docs/superpowers/{specs,plans}/` + `.superpowers/sdd/<plan>/progress.md` (read-only, convenience).
- [ ] **Step 3:** Model rule: planning/orchestration on Opus; implementation via `task-runner` (Sonnet), dispatched by SDD or directly for Polish; sub-agents allowed when orchestrated.
- [ ] **Step 4:** Mode 1 writes Features.md status + Progress milestone rows (not Plan rows); moves Done/Fixed rows to archives; moves Progress rows past 50 to Progress-archive.md; templates replaced (Features.md, Features-done.md, Polish.md, Polish-done.md, Spec.md overarching).
- [ ] **Step 5:** Mode 2 reads HANDOVER, Features.md, newest 3 Progress rows, Learnings, and the active feature's spec/plan/ledger; fills missing merge links in Features-done.md (`git log master --oneline`, `gh pr list --state merged`).
- [ ] **Step 6: Old-layout scenario** — add: "If `Plan.md`/`PLAN.md` exists and `Features.md` doesn't: keep working with the old files, and offer migration: propose a row-by-row mapping (feature / Polish / archive) and move nothing until the user confirms." Also: "Run `check_tracker.py` after any handover write; fix or report its problems."
- [ ] **Step 7: Verify** — `grep -nE "PLAN\.md|Claude Doc" SKILL.md` only hits the old-layout paragraph; `grep -n check_tracker SKILL.md` hits.

### Task 5: project-tracker skill

**Files:** Modify `~/.claude-personal/skills/project-tracker/SKILL.md`.

- [ ] **Step 1:** Description: "Set up a feature-based project tracker (Spec, Features, Polish, Progress, Learnings) and project instructions…".
- [ ] **Step 2:** Drop document-tracker options; tracker is Markdown in `tracker/` (repo or project folder).
- [ ] **Step 3:** Setup gathers also: "Are worktrees safe here? (default: work in place if gitignored files are needed to build)" → recorded in project CLAUDE.md as the Superpowers worktree preference.
- [ ] **Step 4:** Replace Plan tab/T-task sections with: Features.md (statuses, seed notes, Done → Features-done.md), Polish rules, Spec overarching sections, Progress archive at 50 rows.
- [ ] **Step 5:** Workflows section: feature (brainstorming → writing-plans → SDD with `subagent_type: task-runner, model: sonnet`, brief override "leave changes uncommitted; list files changed", controller commits per task) small fix (`@task-runner do Pxx`), and research (researcher reports → controller triages into Polish tickets or a feature's seed notes, with the user's OK); "without Superpowers: write the spec/plan by hand in the same paths".
- [ ] **Step 6:** Planning mode: add Feature rows (Idea + seed notes) or Polish tickets; designing/planning delegated to brainstorming/writing-plans.
- [ ] **Step 7:** Reference template rewritten to the new files; instructions-file template's "Source of truth" lists them and the two workflows.
- [ ] **Step 8: Verify** — run checker against a scratch project created only from the skill's templates (`<scratchpad>/pt-demo/tracker/`) → `OK`.

### Task 6: Lazy Wallet instructions, CLAUDE.md, final sweep

**Files:** Modify `tracker/lazy-wallet-instructions.md`, `/Users/joseluna/dev/personal/budget/CLAUDE.md` (not in git).

- [ ] **Step 1:** Instructions: Source of truth lists new files; Working in sessions describes the two workflows (as Task 5 Step 5); Model rule as Task 4 Step 3.
- [ ] **Step 2:** CLAUDE.md: add a "Project tracking" paragraph (tracker/ files, `docs/superpowers/` specs/plans) and "Superpowers: work in place on a feature branch; never create worktrees (gitignored build files)".
- [ ] **Step 3: Sweep** — `grep -rn "Plan\.md\|PLAN\.md" tracker/ ../CLAUDE.md ~/.claude-personal/agents ~/.claude-personal/skills/{handover,project-tracker}` → only legacy-layout mentions; checker → `OK`. No app code is touched, so Gradle checks aren't needed.
- [ ] **Step 4 (controller): Commit** `tracker/lazy-wallet-instructions.md` — "docs: pointed project instructions at the feature-based tracker".
