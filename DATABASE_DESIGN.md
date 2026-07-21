# PantryPal database design

## Core entities

### Item and inventory

- `items`: reusable product definition, barcode, unit, category, dietary flags, usual-item flag, and image URL.
- `inventory`: quantities or batches linked to items, including added and optional expiry dates.
- `consumption_history`: finished or wasted events linked to items.

### Meals and rotation

- `meal_weeks`: stable `weekId` primary key, editable name, emoji, and sort order.
- `meals`: meal ID, name, week ID, JSON ingredient list, ISO weekday (Monday = 1), and meal slot.

Week IDs A–D are stable references. Renaming a week or changing its emoji therefore does not rewrite meals or preferences.

### Shopping

- `shopping_sections`: section ID, name, sort order, recurring-every-week flag, and optional protected system key.
- `shopping_list`: item ID, name, quantity, unit, checked state, creation time, legacy frequency, section ID, and nullable week ID.
- `shopping_history`: normalised item-name primary key, display name, and last-used timestamp.

The default sections are **Every week**, **Baby stuff**, **Meal plan**, and **The rest**. Recurring section items have no week ID. Generated meal ingredients and non-recurring additions are tagged with their rotation week.

## Relationships and ownership

- One item definition can have many inventory batches and consumption events.
- One meal week can have many meal templates.
- One shopping section owns many active shopping entries.
- Meals remain reusable templates. Their ingredients are synchronised into the selected week's **Meal plan** section only when the user chooses **Build list**.
- Shopping history is deliberately separate from active entries, so clearing a checklist does not erase quick-add memory.

## Version 5 migration

The 4→5 migration:

1. Creates `meal_weeks`, `shopping_sections`, and `shopping_history`.
2. Adds `sectionId` and nullable `weekId` to existing shopping items.
3. Maps legacy essentials to **Every week** and legacy Week A/B entries to the corresponding week.
4. Copies existing item names into shopping history.
5. Inserts missing default week metadata, sections, and recurring base entries.
6. Seeds the four-week example meal schedule only when the meals table is empty.

Default inserts use fixed system IDs or existence checks so upgrades do not duplicate equivalent base entries.

## Backup scope

The current export path serialises items, inventory, and consumption history. Meal plans, shopping sections, and shopping history remain local but should be added if the export format becomes a full-device backup contract.
