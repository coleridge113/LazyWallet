# Feature-based project tracker — design

Date: 2026-11-25 · Status: final draft, awaiting approval · Owner: Jose Luna

## Intent and why

The tracker (`tracker/Plan.md`, `Polish.md`, `Spec.md`, …) is one task list, one ticket list and one spec that everything gets appended to. At hundreds of rows it can't be scanned, and appending to `Spec.md` is awkward. The user wants:

- one **overarching spec** for the project (rarely changes), and
- **one spec and one plan per feature**, so Opus can be pointed at a single feature ("plan the top app bars") without reading the whole project.

Follow Superpowers' lead (its folders and flow, unmodified) rather than inventing a system. Apply it to **every project** (global skills and agent), not just Lazy Wallet.

## Goals

- Any list the user reads stays short: open items only; finished work moves out.
- Each feature has its own spec + plan, written by Superpowers.
- Small fixes still have a cheap path that doesn't need a spec or plan.
- New projects start in this layout (`project-tracker` skill); `handover` resumes from it.

## Non-goals

- No GitHub-issue tracking, no kanban UI, no new plugin.
- Not changing how Superpowers itself works (no forking its skills).

## Anti-goals (failure even if it "works")

- Two places describing the same feature (tracker *and* `docs/superpowers/`).
- A replacement list that again grows without bound.
- Breaking Lazy Wallet builds (worktrees lose gitignored `google-services.json`, `local.properties`).

## Constraints

- Superpowers 7.x hard-codes `docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md` and `docs/superpowers/plans/YYYY-MM-DD-<feature>.md`.
- `subagent-driven-development` (SDD) dispatches a fresh implementer per plan task; **by default the implementer commits each task**, and the task reviewer reviews the commit range. It keeps its own ledger in git-ignored `.superpowers/sdd/`. It wants an isolated worktree but accepts "work in place" when the user declines.
- The global `task-runner` agent is hook-blocked from commit/push/merge/branch switching.
- Project instructions say commits need the user's OK.

## Approaches considered

1. **Superpowers' folders as-is + slim tracker** (chosen). Specs/plans live where Superpowers writes them; the tracker becomes an index plus project-wide files. Works with plugin updates untouched; cost: a feature's two files are linked by name, not a shared folder.
2. One folder per feature in `tracker/features/<slug>/`. Everything together, but every session must be told to override Superpowers' paths; one forgotten reminder scatters files.
3. (Earlier rejected) Adopting GitHub Spec Kit / CCPM: duplicates Superpowers' role or moves tracking into GitHub and needs worktrees.

Decisions made with the user:
- Small fixes: **a project-wide small-fixes list** (Polish), not mini-features.
- Execution: **Superpowers' SDD**, Opus orchestrating, `task-runner` (Sonnet) as the implementer, review between tasks.
- Layout: **approach 1**.
- Scope: **every project** (global skills + agent).
- Commits during a build: **the Opus controller commits**, task-runner never does (Q1 → B).
- Document trackers (Claude Doc / Notion / Google Doc): **dropped**; this is the user's first AI-assisted project, so nothing relies on them.
- **Superpowers may be dropped someday.** The layout must keep working without it: specs and plans are plain Markdown linked from Features.md, so a future session (or another tool) can write them by hand in the same folders. Nothing in the tracker may depend on Superpowers-only state (the SDD ledger is a convenience, never the record of status).

## The design

### Files

```
docs/superpowers/
  specs/YYYY-MM-DD-<slug>-design.md   feature spec (Superpowers brainstorming)
  plans/YYYY-MM-DD-<slug>.md          feature plan (Superpowers writing-plans)
.superpowers/sdd/                      SDD ledgers (git-ignored, Superpowers-owned)
tracker/
  Spec.md            overarching spec only
  Features.md        index of open features
  Features-done.md   finished features (one line each, newest first)
  Polish.md          open small fixes
  Polish-done.md     fixed / won't-fix tickets
  Progress.md        session log, newest first
  Progress-archive.md  older Progress rows (my call: move rows past the newest 50)
  Learnings.md       unchanged
  HANDOVER.md        written by the handover skill
  <project>-instructions.md
```

`Plan.md` is retired: its tasks become features or Polish tickets.

### Features.md

```
| ID | Feature | Status | Spec | Plan | Next step |
|---|---|---|---|---|---|
| F05 | Screen chrome: top bars, nav labels, undo | Idea | – | – | Your call: P17/P18 decisions, then brainstorm |
```

- IDs F01, F02, … (never reused). Status: **Idea · Designing · Planned · Building · Blocked · Done · Later**. `Your call` is expressed in Next step, not as a status.
- Spec / Plan cells link the Superpowers files once they exist; the slug is the same in both.
- **Seed notes** (my call): an Idea-stage feature may have a short `### F05 seed notes` section below the table (source tickets, research findings). When brainstorming writes the spec, the seed notes are folded into it and deleted from Features.md.
- When a feature is Done (final review clean, merged or explicitly accepted by the user), its row moves to `Features-done.md` as one line: `F05 · title · done date · spec link · plan link · merge commit/PR`.

### Polish.md

Same table as today (`ID | Issue | Area | Status | Found | Notes`, statuses Open · In progress · Fixed · Won't fix). Only **Open / In progress** rows stay; Fixed and Won't fix move to `Polish-done.md` (Won't fix also gets an Accepted-gaps row). Rule for what belongs here: one sitting, no design decision, ≤ a few files. Anything needing a design decision or an Opus plan becomes (or joins) a feature instead.

### Spec.md (overarching)

Sections: 1 What it is & current state (short) · 2 Goal · 3 Architecture pointer (CLAUDE.md) · 4 Project-wide facts (e.g. Figma file URL and page structure) · 5 Constraints and approver · 6 Accepted gaps · 7 Feature specs: "per-feature specs live in `docs/superpowers/specs/`; see Features.md". Feature-specific detail (e.g. Figma known issues) moves into that feature's seed notes/spec.

### Workflows

**Feature (Opus main session):** "plan F05" →
1. `superpowers:brainstorming` → spec in `docs/superpowers/specs/`; Features.md status Designing → link spec.
2. `superpowers:writing-plans` → plan; status Planned → link plan.
3. `superpowers:subagent-driven-development`, working **in place on a feature branch** (no worktree), dispatching `task-runner` on Sonnet as implementer per task, task reviewer per task, final whole-branch review on Opus. Status Building.
4. `superpowers:finishing-a-development-branch` → user decides merge/PR. Status Done → row moves to Features-done.md. One Progress row per milestone (spec approved, plan approved, build finished), not per plan task (the SDD ledger tracks tasks).

**Small fix:** `@task-runner do P13` — today's flow, unchanged: reads the tracker, does one ticket, updates Polish/Progress/Learnings, doesn't commit.

**Research:** researcher reports → main session triages: small items → Polish; larger → new feature row (Idea, with seed notes) or appended to an existing feature's seed notes / a note for its next brainstorm.

### task-runner changes (global agent)

Two modes, detected from the dispatch prompt:
- **Ticket mode** (default, current behaviour): tracker routine, no commits.
- **Plan-task mode** (dispatched by SDD with an implementer brief): follow the brief and plan, not the tracker routine; don't edit tracker files (the controller does); run the checks; **leave changes uncommitted** and report in SDD's format (status, files changed, one-line test summary, concerns), listing exactly which files it changed.

The Opus controller then commits that task (staging only the files the implementer listed) and records BASE..HEAD for the task reviewer; review fixes are committed by the controller too. The guard hook is **unchanged**: task-runner can never commit, push, merge, rebase, switch or delete branches, in either mode.

### Skill changes (global)

- **project-tracker:** Setup creates the layout above (Spec, Features, Polish, Progress, Learnings, the -done/archive files on first use, instructions file). Planning mode adds Feature rows (Idea + seed notes) or Polish tickets; designing and planning a feature is handed to Superpowers. Model rule rewritten: planning/orchestration on the planning model, implementation via task-runner (task model) — sub-agents allowed when orchestrated by SDD. Asks at setup whether worktrees are safe for the project (default "work in place" when gitignored files are needed to build). My call: drop the "Claude Doc / Notion / Google Doc" tracker option — the layout is repo Markdown because Superpowers writes repo files; for a project without a repo, the same folders go in the project folder.
- **handover:** file table matches the real Title-case names (`Features.md`, not `PLAN.md`). Resume reads HANDOVER, Features.md, the newest 3 Progress rows, Learnings, and — for the active feature — its spec, plan and SDD ledger (`.superpowers/sdd/…/progress.md`). Writing a handover updates Features.md status instead of Plan rows. Projects still on the old layout (Plan.md present, no Features.md): handover offers to migrate, otherwise keeps working with the old files.

### Lazy Wallet migration (one-time)

- Done items → archives: P04, P05, P07, P08, P09, P11, P12 → Polish-done.md; T01 → Features-done.md (as a pre-feature task).
- Stay in Polish (small, Sonnet-ready or need only an OK): P01 backup rules, P06 version bumps (needs OK), P13 strings & ₱, P15 budget card, P19 shape/type tokens.
- Become features (Idea, seed notes from the ticket text):
  - F01 Figma design file — T02, T03, T04, T05 (Blocked: Figma rate limit)
  - F02 Navigation events (P02)
  - F03 Firebase out of presentation (P03)
  - F04 Theme, contrast & light theme (P14; links to F01's T05 light-theme Figma work)
  - F05 Screen chrome: top app bars, nav labels/Analysis tab, snackbar undo (P16, P17, P18)
  - F06 Test coverage (P10)
  - F07 Adaptive layouts (P20, Later)
- Polish IDs continue from P21; feature IDs from F08.
- Spec.md trimmed per above; §5.1 Figma known issues → F01 seed notes; Figma URL/page structure stays as a project-wide fact.
- `lazy-wallet-instructions.md` and `../CLAUDE.md`: point at Features.md, state "Superpowers: work in place, never create worktrees (gitignored build files)", describe the two workflows.

## Builder's calls (settled during review; check these)

- **Dispatching task-runner from SDD:** SDD's implementer template names `general-purpose`; the project instructions and the global project-tracker text tell the controller to dispatch `subagent_type: task-runner, model: sonnet` instead, and to override the brief's "commit your work" step with "leave changes uncommitted; list the files you changed". The Agent tool accepts custom agent types, so no Superpowers fork is needed. task-runner's tools (Read, Edit, Write, Grep, Glob, Bash) cover the implementer brief.
- **Plan-task mode pre-flight:** skips the tracker checks ("ID in tracker", "tree must be clean"); keeps "stay on the current branch" and the Never/Ask-first boundaries. Lists every file it changed; the controller stages only those (never `git add -A`), so tracker edits can't leak into a task's commit.
- **Worktrees:** using-git-worktrees honours a declared preference without asking. Lazy Wallet's CLAUDE.md declares "work in place on a feature branch; never create worktrees (gitignored build files)". project-tracker asks this once at setup and records it in the project's CLAUDE.md.
- **Tracker edits during a build:** the Opus controller updates Features.md / Progress.md only at milestones (spec approved, plan approved, build finished), between tasks, and they are committed with the feature at finishing time, not inside implementer commits.
- **When a feature is Done:** at `finishing-a-development-branch`, once the user picks merge/PR/keep. The Features-done.md row is written then; a merge commit or PR link not yet known is filled in by the next `handover` resume (it checks `git log master` / `gh pr list`).
- **Migration normalisation:** non-standard statuses ("Fixed (no retry yet)", "Done …") become `Fixed`, with the extra text kept in Notes. T01 goes to Features-done.md as `T01 (pre-feature task)`. No Polish row is Won't fix today, so no new Accepted-gaps rows.
- **Other existing projects:** handover/project-tracker offer the migration and propose a row-by-row mapping (feature vs Polish vs archive) for the user to confirm before moving anything; never automatic.
- **File-name case:** Title-case (`Features.md`, `Progress.md`) is the only form written from now on; old-layout detection accepts either case.

## Left to the builder

Exact wording of skill text; Features.md seed-note formatting; hook implementation details for "commit allowed only off main/master"; order of migration steps; whether Progress-archive.md is created now or on first need.
