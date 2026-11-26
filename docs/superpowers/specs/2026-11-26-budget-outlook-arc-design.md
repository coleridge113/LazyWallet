# F12 Budget outlook arc — design (short)

Date: 2026-11-26 · Status: approved 2026-11-26 · Owner: Jose Luna · Branch: `feat/outlook-arc` (from `master`) · First screen of the screen-by-screen UI rebuild

## Intent and why

The `BudgetOutlookCard` at the top of the Budget screen is text-heavy on `master`: "Total Remaining", then "Projected" and "Actual" amounts side by side. Jose wants to see at a glance **where actual spending is against projected**, where projected is the month's total target spend. The card should look like the `BudgetCard`s, **as an arc** instead of a bar.

An earlier attempt on `feat/improved-outlook-card` had two concentric arcs, both scaled to whichever value was bigger. That only showed which number was larger, not progress toward the target. It also used hard-coded strings, pixel-offset `drawText`, `Color.Black` and direct Gruvbox colours. That branch is 62 commits behind `master` and is used as a reference only. It stays as it is and isn't deleted.

## Goals

- One gauge arc showing `actualSpend / projectedSpend` for the current month.
- A **pace marker** (Jose, 2026-11-26): a tick at the share of the month that has passed (day of month ÷ days in month), so you can see at a glance whether you're ahead of or behind an even pace.
- The same visual language as `BudgetCard`:
  - colour thresholds `primary` < 75%, `tertiary` 75% to under 90%, `error` ≥ 90%;
  - track = the same colour at 20% alpha;
  - round caps;
  - animated fill;
  - "left" / "overspent" wording.

## Non-goals

- Income: the field stays in `OutlookDetails` but isn't shown. There's no hidden placeholder row.
- Changing what "actual" or "projected" mean. Actual stays all expenses this month, as `getTotalAmountByDateRange` already computes it. Projected stays the sum of `frequency.projectAmountToMonth(limit)`.
- The rest of the Budget screen (the BudgetCards, the list, top bar P16). These come later in the rebuild.

## Anti-goals

- A gauge that looks "fine" when you're overspent, or "full" when you're well under.
- Text that ignores font scale or theme, or that TalkBack can't read.

## Approaches (Jose picked A, 2026-11-26)

- **A. Custom gauge (chosen).** A `Canvas` draws only shapes: the track, the fill and the pace tick. The amounts are ordinary Compose `Text` centred over the arc in a `Box`. That keeps typography, font scaling, `strings.xml` and semantics working.
- **B. M3 `CircularProgressIndicator`.** Rejected: it's a full circle from the top, with no partial gauge and no way to place a marker.
- **C. Chart library.** Rejected: a dependency for one arc.

## Design

**Arc geometry (my call):** a 240° gauge open at the bottom like a speedometer (start angle 150°, sweep 240°). The stroke is about 12dp, with round caps. The gauge is roughly 200dp wide, centred in the card.

**Values (pure function, unit-tested).** Extend `OutlookDetails`, or add a small `OutlookProgress` computed in the ViewModel (builder's call), with:

- `usedFraction = actual / projected`, unclamped (it can exceed 1 when overspent). The arc draws `min(used, 1)`, and colour uses the unclamped value.
- `paceFraction = dayOfMonth / lengthOfMonth`, from `todayFlow(clock)`, which the BudgetViewModel already has. On 2026-11-26 that's 26/30 ≈ 0.867.
- When `projected == 0` (no budgets): `usedFraction = null`.

**Colour:** extract `budgetProgressColor(progress: Float): Color` from `BudgetProgressBar` (a `@Composable` that reads `MaterialTheme`). Both the bar and the arc use it, so the thresholds live in one place.

**Pace tick:** a short radial line across the stroke at `150° + 240° × paceFraction`, in `onSurface`. It's thin, about 2dp, and sits a little beyond the stroke on both sides so it reads against both the track and the fill.

**Text inside the arc:**

- Line 1 (`titleLarge` or larger): the remaining amount (`abs(projected − actual).toPeso()`).
- Line 2 (`bodyMedium`): `budget_suffix_left` or `budget_suffix_overspent` (existing strings).
- Below the arc, in the gap: "₱12,300 of ₱28,000", a new string `outlook_spent_of` = "%1$s of %2$s".

**No budgets (`projected == 0`):** show the empty track, no tick, and a new string `outlook_no_budgets` = "No budgets yet" in the centre.

**Accessibility:** the card is one semantics node with a `contentDescription` built from strings. For example, a new `outlook_cd` = "Spent %1$s of %2$s this month, %3$s %4$s" (amount + left/overspent), plus a pace phrase if it fits naturally (builder's call). The `Canvas` has no semantics of its own.

**Strings:** all new text goes in `strings.xml`. Remove `outlook_total_remaining`, `outlook_projected` and `outlook_actual` if nothing else uses them.

```
          ╭────────────────╮
       ╭─╯    ₱15,700       ╰─╮
      │       left          ┃  │     ← fill 44% in primary; ┃ = pace tick at 87%
       ╲                      ╱
           ₱12,300 of ₱28,000
```

## Tests

- Pure function: `usedFraction` under, exactly at, and over target, and with `projected == 0`. `paceFraction` on the 1st (≈ 0.033), the 15th of a 30-day month (0.5), the last day (1.0), and in February of a leap year (29 days).
- `BudgetViewModelTest`: the outlook's pace comes from the injected `MutableClock`, and the existing actual-spend tests still pass.
- The arc itself is checked visually in `@Preview`s (under pace, over pace, overspent, no budgets; light and dark) and on device.

## Done when

- The card shows the arc, tick, amounts and wording above. The colours match the BudgetCards at the same percentages.
- Unit tests pass (count up from 100). `assembleDebug` OK.
- Device check: compare with a BudgetCard at a similar percentage; overspend; no budgets; large font scale; TalkBack reads one sentence.

## Left to the builder

- Exact sizes and stroke widths, within the ranges above.
- Whether the fractions live on `OutlookDetails` or a new type.
- Whether the pace phrase goes into the TalkBack sentence.
