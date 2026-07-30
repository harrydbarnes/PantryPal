# Rotating meal planner design

## Goal

Make weekly food planning quick enough to reuse but flexible enough for real life. A household should recognize the current week, use what is at home, change plans, and turn the chosen week into a structured shopping checklist.

## Rotation and reuse

- Four ordered templates, A through D, advance every Monday from a user-set anchor.
- Each template has an editable name and emoji while retaining a stable ID.
- Meals have weekday plus Breakfast, Lunch, Dinner, or Other.
- Meals can be created, edited, deleted, copied, or reused as a whole week without duplicate exact matches.
- **Eating out** occupies a schedule slot without adding shopping noise.
- A saved/discovered recipe can become a meal; recipe ID and servings remain attached.

## Pantry-aware shopping

- Saving a meal does not mutate shopping.
- **Review meal-plan shopping** normalizes and deduplicates ingredients, then reconciles them with aggregate pantry stock.
- Each line is **Need to buy**, **Already at home**, or **Check stock**. Uncertain unit/quantity comparisons are never presented as precise shortages.
- Committing a build replaces only that rotation week's generated **Meal plan** entries.
- Items covered by recurring sections or existing unchecked entries are not duplicated.
- **Every week** and custom recurring sections remain visible through the rotation.
- **Finish shop & put away** adds checked purchases to a compatible pantry batch or creates a new batch in the selected location.

## Recipe ideas

- Existing meal templates bootstrap the recipe book without duplicating normalized titles.
- Local search covers title, ingredient, tag, and source.
- Pantry matching creates **Cook now**, **Use soon**, **Only missing 1–2**, and **Forgotten favourites** shelves.
- Users can search TheMealDB and import public schema.org `Recipe` pages with attribution.
- Missing essential ingredients can be added to the selected week's **Meal plan** section.

## Material 3 contract

- Week/section choices use wrapping chips and semantic tonal states.
- **Review/Build list** is the high-emphasis shopping action.
- Dialogs use outlined fields, concise supporting text, and disabled confirmation until inputs are valid.
- Icon-only actions include content descriptions and standard touch targets.
- Content follows the semantic theme and 8dp spacing rhythm.

## Acceptance checks

- Version-4 and version-5 databases migrate without losing meals, shopping, or inventory.
- Changing the current template updates the anchor and advances correctly after one week.
- Meal and whole-week reuse avoid duplicate exact matches.
- Shopping generation never includes another rotation week.
- Pantry-owned ingredients are omitted; uncertain matches remain visible as checks.
- A recipe can become a linked meal and its missing ingredients can become shopping entries.
- Checked shopping can be put away into inventory.
