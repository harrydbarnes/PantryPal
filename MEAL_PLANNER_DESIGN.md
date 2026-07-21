# Rotating meal planner design

## Goal

Make weekly food planning quick enough to reuse, but flexible enough for real life. A household should recognise the current themed week, see meals by day, change plans such as eating out, and turn the chosen week into a structured shopping checklist.

## Behaviour

### Rotation and themes

- The default schedule contains four ordered templates, A through D.
- Each template has an editable name and emoji without changing its stable rotation ID.
- The current week is anchored to Monday and advances through all four templates every seven days.
- **Make current** resets the anchor when the real-world rotation needs correcting.

### Planning and reuse

- Each meal has a weekday and one of Breakfast, Lunch, Dinner, or Other.
- A meal can be created, edited, deleted, or copied to any other week.
- **Reuse week** chooses any source template and copies its non-matching meals. A match is the same name, weekday, and meal slot.
- **Eating out** is a quick plan with no required ingredients, so it occupies the schedule without adding shopping noise.

### Structured shopping

- Saving a meal template does not mutate the shopping list.
- **Build list** replaces the displayed week's generated **Meal plan** entries with its current ingredients, trimmed and deduplicated case-insensitively.
- Items already covered by recurring sections or **The rest** are not duplicated.
- **Every week** and any custom recurring sections remain visible through the rotation. **The rest** holds quick additions for only the current template week.
- Item names are remembered independently of the active checklist and offered as quick-add suggestions later.
- Clearing checked items removes completed week-specific entries and resets recurring entries for the next shop.

## Default examples

Fresh databases receive the supplied Week A–D dinner schedule. Ingredients are normalised into editable shopping prompts; repeated meals reuse the same prompts. The recurring base list includes Bananas, 3 Eggs, Grapes, Raspberries, Broccoli, and Mozzarella. A recurring **Baby stuff** section includes Formula milk, Size 5 nappies pull ups, and Sensitive wipes with starting quantities of zero for stock checks.

Existing databases receive the week and shopping-section structure, but the example meals are inserted only if their meals table is empty.

## Material 3 UI contract

- Dynamic color is used on Android 12+ with a pantry-green light/dark fallback scheme.
- Week selection uses wrapping name-and-emoji filter chips; current-week status uses a tonal container.
- Days and shopping sections use filled tonal cards rather than shadow-heavy elevation.
- **Build list** is the high-emphasis action; reuse and row utilities have lower emphasis.
- Dialogs use outlined fields, concise supporting text, filter/suggestion chips, and disabled confirmation until required names exist.
- Icon-only actions have content descriptions and standard Material touch targets.
- Content uses a 4/8dp spacing rhythm and semantic theme color roles.

## Acceptance checks

- Existing version-4 databases migrate without losing meals or shopping entries.
- Setting any template as current makes it current immediately and advances to the next ordered template after one week.
- Users can name weeks, associate emojis, and edit weekday, slot, meal name, and ingredients.
- A meal or whole schedule can be reused across any of the four weeks without duplicate exact matches.
- Building a shopping list deduplicates ingredients and does not add ingredients from another week.
- Custom shopping sections can recur every week, and cleared item names remain available as suggestions.
