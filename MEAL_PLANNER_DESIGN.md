# Rotating meal planner design

## Goal

Make weekly food planning quick enough to reuse, but flexible enough for real life. A household should know whether it is Week A or Week B, see meals by day, change plans such as eating out, and turn the chosen week into a shopping checklist.

## Behaviour

### Rotation

- The schedule contains Week A and Week B.
- The current week is anchored to Monday and alternates automatically every seven days.
- “Make current” resets the anchor when the real-world rotation needs correcting.

### Planning and reuse

- Each meal has a weekday and one of Breakfast, Lunch, Dinner, or Other.
- A meal can be created, edited, deleted, or copied to the other week.
- “Reuse week” copies all non-matching meals from the other template. A match is the same name, weekday, and meal slot.
- “Eating out” is a quick plan with no required ingredients, so it occupies the schedule without adding shopping noise.

### Shopping-list build

- Saving a meal template does not mutate the shopping list.
- “Build list” gathers the displayed week's ingredients, trims and deduplicates them case-insensitively, adds missing items as one-offs, and reopens matching checked items for review.
- Existing essential or recurring entries with the same name are not duplicated.

This explicit build step avoids stale shopping entries after a meal is edited or removed. Exact ingredient quantities and inventory matching are a future enhancement because ingredients are currently free text.

## Material 3 UI contract

- Dynamic color is used on Android 12+ with a pantry-green light/dark fallback scheme.
- Week selection uses equal-width filter chips; current-week status uses a tonal container.
- Days use filled tonal cards rather than shadow-heavy elevation. Meals are list rows separated with `outlineVariant` dividers.
- “Build list” is the high-emphasis action; reuse and per-meal utilities have lower emphasis.
- Dialogs use outlined fields, filter chips, concise supporting text, and disabled confirmation until a name exists.
- Icon-only actions have content descriptions and standard Material touch targets.
- Content uses a 4/8dp spacing rhythm and theme typography/color roles rather than screen-specific colors.

## Acceptance checks

- Existing version-3 databases migrate without losing meals.
- Setting Week A or B as current makes it current immediately and flips after one week.
- Users can add and edit weekday/slot/name/ingredients.
- Users can represent eating out without ingredients.
- A meal or whole schedule can be reused across weeks without duplicate exact matches.
- Building a shopping list deduplicates ingredients and does not add ingredients from the other week.
