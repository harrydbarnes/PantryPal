# PantryPal app overview

PantryPal is a local-first Android kitchen companion built with Kotlin, Jetpack Compose, Material 3, Room, WorkManager, CameraX, and ML Kit. It tracks cupboard inventory, barcode-based additions and consumption, expiry warnings, shopping items, consumption history, and a rotating meal schedule.

## User journeys

- **Dashboard:** see expiring stock and restock suggestions, then open the cupboard.
- **Kitchen cupboard:** view inventory and record an item as finished or wasted.
- **Scan in / scan out:** identify products by barcode and add or consume them.
- **Meal plan:** maintain reusable Week A and Week B schedules, change individual days, copy meals or an entire week, and build the week's shopping list.
- **Shopping:** review one-off, essential, and week-specific items; tick or remove items.
- **Past items:** review consumption history.
- **Settings:** view build and version information.

## Architecture

The app uses a compact single-module architecture:

1. Compose screens render state and send user events to `MainViewModel`.
2. `MainViewModel` owns UI-facing state flows, preferences, and application actions.
3. `KitchenRepository` is the boundary around Room DAOs and Open Food Facts.
4. `KitchenDatabase` stores items, inventory batches, consumption, shopping items, and meals.
5. `ExpirationWorker` performs scheduled expiry checks.

The app currently uses manual screen state in `MainActivity` rather than Navigation Compose. Data is stored on-device; Open Food Facts is only contacted for an unknown scanned barcode.

## Data model

- `ItemEntity`: reusable product definition and dietary/category metadata.
- `InventoryEntity`: a quantity/batch of an item, optionally with an expiry date.
- `ConsumptionEntity`: finished/wasted history.
- `ShoppingItemEntity`: checklist entry with quantity, unit, and recurrence label.
- `MealEntity`: named meal assigned to Week A/B, weekday, meal slot, and a list of shopping ingredients.

Room schema version 4 adds `dayOfWeek` and `mealSlot` to meals. The 3→4 migration keeps existing meals and places them on Monday dinner so users can reorganise them in the planner.

## Current constraints

- Meal ingredients are free-text shopping prompts, not quantities linked to inventory products. The shopping-list build therefore creates a review checklist rather than calculating exact stock shortages.
- The rotation is intentionally two weeks (A/B). Choosing “Make current” anchors that week to the current Monday; it then flips automatically each Monday.
- There is no account or cloud sync. Backup/export currently covers core inventory data only.
