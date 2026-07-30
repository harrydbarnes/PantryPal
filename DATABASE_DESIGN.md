# PantryPal database design

## Core entities

### Item and inventory

- `items`: product definition, barcode, unit, category, dietary flags, always-stocked flag, optional low-stock threshold, and image URL.
- `inventory`: batches linked to items, including quantity/unit, storage location, opened state, added date, and optional expiry.
- `consumption_history`: finished or wasted events linked to items.

### Meals and rotation

- `meal_weeks`: stable `weekId` primary key, editable name, emoji, and sort order.
- `meals`: meal ID, name, week ID, JSON ingredient list, ISO weekday, meal slot, optional recipe ID, and servings.

Week IDs A–D are stable references. Renaming a week or changing its emoji does not rewrite meals or preferences.

### Shopping

- `shopping_sections`: section ID, name, order, recurring flag, and optional protected system key.
- `shopping_list`: name, quantity/unit, checked state, creation time, legacy frequency, section ID, and nullable week ID.
- `shopping_history`: normalized name primary key, display name, and last-used timestamp.

Default sections are **Every week**, **Baby stuff**, **Meal plan**, and **The rest**. Recurring section items have no week ID. Generated meal ingredients and non-recurring additions are tagged with their rotation week.

### Recipes

- `recipes`: normalized unique title, source/attribution, external ID, image, yield/servings, timings, JSON instructions/tags, rating, favourite state, cooked timestamp, and audit timestamps.
- `recipe_ingredients`: ordered parsed lines with normalized name, optional quantity/unit, optional flag, and nullable pantry-item link.

Deleting a recipe cascades to its ingredients. Deleting a linked pantry item sets the optional ingredient link to null without deleting the recipe.

### Prices and budgets

- `price_history`: optional item link, normalized/display name, total price in minor units, quantity/unit, retailer, purchase time, ISO currency, and source.
- `weekly_budgets`: Monday epoch-day primary key, target in minor units, ISO currency, and update time.

Money uses minor units to avoid floating-point rounding. Quantities remain decimal because pantry and receipt units can be fractional.

## Relationships and ownership

- One item definition can have many inventory batches and consumption events.
- One meal week can have many meal templates.
- One shopping section owns many active entries.
- One recipe owns many ordered ingredients; an ingredient can optionally point at one pantry item definition.
- Meals remain reusable templates. Ingredients synchronize into the selected week's **Meal plan** section only when the user commits a reviewed build.
- Shopping history is separate from active entries, so clearing a checklist does not erase quick-add memory.

## Version 5 migration

Migration 4→5 creates meal-week, shopping-section, and shopping-history tables; adds section/week ownership to shopping entries; maps legacy frequency values; seeds missing system structure; and seeds the example meal schedule only when meals are empty.

## Version 6 migration

Migration 5→6:

1. Adds nullable `items.lowStockThreshold`.
2. Adds `inventory.storageLocation` (Pantry) and `inventory.isOpened` (false).
3. Adds nullable `meals.recipeId` and `meals.servings` (4).
4. Creates recipe, recipe-ingredient, price-history, and weekly-budget tables and indexes.

Room column defaults are declared on the entities and in migration SQL so migration validation sees the same schema.

## Backup and household scope

`BackupDocument` is a versioned JSON contract containing every table plus preferences. Import validates identifiers, references, quantities, currencies, and recipe ratings before a dependency-ordered transactional replace. The user chooses files through Android's Storage Access Framework; PantryPal does not request broad storage permission.

`HouseholdSnapshotEnvelope` wraps the same backup with revision metadata and a SHA-256 integrity checksum. The checksum detects accidental change but is not encryption or proof of sender identity. Record versions, tombstones, deterministic merge rules, and an isolated transport interface define a future real-time path without coupling Room data to a backend.
