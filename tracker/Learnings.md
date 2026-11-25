# Learnings

Gotchas not yet in the project instructions; delete them here once folded in.

- Figma Starter plan: MCP tool calls are rate-limited (hit after ~15 file calls plus screenshots), and variable collections allow only 1 mode, so Dark and Light are separate collections ("Theme Dark", "Theme Light").
- Figma: a fill opacity on a variable-bound paint is lost on component instances. Use a background rectangle with layer opacity instead (done in PresetItem, ExpenseItem, BudgetCard).
- Figma: components created with `layoutMode` default to a fixed 100px height; set `counterAxisSizingMode = "AUTO"` or items come out too tall.
- Figma: resizing an icon component instance (24px SVG) to 12px distorts the icon; build a small icon variant instead.
- Use `get_screenshot` then curl the URL to a scratchpad file and Read it to actually see a frame.
