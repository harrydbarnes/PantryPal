# PantryPal app overview

PantryPal is a local-first Android kitchen companion built with Kotlin, Jetpack Compose, Material 3, Room, WorkManager, CameraX, Retrofit, and ML Kit. It connects pantry inventory, a four-week meal rotation, recipes, shopping, receipt capture, and household-safe data portability.

## User journeys

- **Dashboard:** see expired/due-soon stock and aggregate low-stock suggestions, then add a restock item directly.
- **First run:** move through onboarding covering pantry tracking, scanning, the four-week rotation, shopping-list generation, privacy, and permissions.
- **Add an item:** enter only a name and quantity to add quickly. Unit, location and category start from the most recently used values; expand **Item details** for location, opened state, category and expiry, or **Preferences and restock** for dietary flags and low-stock reminders. The sticky action bar keeps **Add item** reachable and offers **Save and add another** for a run of entries.
- **Kitchen cupboard:** search, filter, sort and stocktake batches; track storage location, opened state, expiry, always-stocked status, and low-stock thresholds.
- **Scan in / scan out:** identify products by barcode and add or consume a whole or partial quantity.
- **Meal plan:** maintain four rotating weeks, rename and reorder the weekly rhythm, reuse meals or whole weeks, open the recipe book, and preview a pantry-aware shopping build.
- **Recipes:** reuse meals as saved recipes, search locally, see cook-now/use-soon/missing-one-or-two ideas, search TheMealDB, import schema.org recipe links, favourite/rate recipes, add a recipe to the plan, and send missing ingredients to shopping.
- **Shopping:** plan any rotation week, distinguish buy/home/check-stock ingredients, maintain custom or recurring sections, and finish a shop into pantry inventory.
- **Receipts and budget:** select an image for on-device text recognition or paste receipt text, correct uncertain names, quantities and prices, add purchases to pantry and price history, compare unit-price changes, and track weekly spending against a target.
- **Past items:** review consumption history.
- **Settings and data:** control appearance/reminders, export or restore a complete backup, exchange checksummed household snapshots, replay onboarding, and view build information.

## Architecture

The app remains a compact single Android module:

1. Compose screens render state and send user events to `MainViewModel` or `PantryFeaturesViewModel`.
2. `MainViewModel` owns the core pantry, meal-plan, and shopping workflows.
3. `PantryFeaturesViewModel` owns recipe discovery, receipt review, budgets, backups, and household snapshot state.
4. `KitchenRepository` is the core boundary around Room and Open Food Facts. `PantryFeaturesRepository` coordinates the linked feature workflows.
5. `KitchenDatabase` stores the complete local kitchen and exposes a dependency-aware backup DAO.
6. `ExpirationWorker` performs scheduled expiry checks.

The app uses manual screen state in `MainActivity` rather than Navigation Compose. Data is stored on-device. Network requests occur only for an unknown scanned barcode, an explicit online recipe search, or an explicitly imported recipe URL.

## Implementation guardrails

- Keep historical or other large derived calculations in Room queries where practical. For example, `ConsumptionDao.getRestockCandidates` derives the average consumption interval in SQL, so the app does not need to load and filter the full history in memory.
- Do not place a clickable overlay over a text field without explicit semantics. If that interaction is necessary, expose the field label and current value through a `contentDescription` so TalkBack users retain the same context.
- The add-item flow retains its barcode and prefilled-item paths. Only name and a positive quantity are required; expandable sections use button semantics and the expiry control announces its current value to TalkBack.

## Material 3 design system

PantryPal uses a warm garden-pantry Material 3 theme with complete light/dark semantic roles, dynamic colour on Android 12+, an emphasized type scale, rounded shapes, 48dp touch targets, and an 8dp spacing rhythm.

Reusable building blocks live in `ui/components/ExpressiveComponents.kt`:

- `ExpressiveHero` gives each main journey a clear opening moment.
- `StatusPill` communicates small counts and states without relying on colour alone.
- `SectionHeading` establishes consistent hierarchy.
- `FriendlyEmptyState` turns empty data into useful guidance.
- `PantryPalSpacing` keeps screen rhythm consistent.

Compact windows use four primary destinations: Home, Pantry, Plan, and Shop. Windows at 600dp and above use a navigation rail with the same hierarchy. Scan in/out and the Past Items Log remain contextual Pantry actions; Recipes are reached from Plan; receipt review and Budget & Prices are reached from Shop; and Data & Sharing is reached from Settings. Settings is always a direct app-bar action with an explicit accessibility label, while Home stays focused on stock and restock guidance rather than a second menu. Secondary screens retain their parent-aware back behaviour.

## Data model

- `ItemEntity`: reusable product definition, dietary/category metadata, always-stocked state, and optional low-stock threshold.
- `InventoryEntity`: a quantity or batch with storage location, opened state, and optional expiry.
- `ConsumptionEntity`: finished or wasted history.
- `ShoppingSectionEntity`: ordered recurring or week-specific shopping group.
- `ShoppingItemEntity`: checklist entry assigned to a section and optional rotation week.
- `ShoppingHistoryEntity`: item-name memory for quick-add suggestions.
- `MealWeekEntity`: editable week name, emoji, and rotation position.
- `MealEntity`: meal, rotation week, weekday/slot, servings, ingredients, and optional recipe link.
- `RecipeEntity` / `RecipeIngredientEntity`: reusable instructions, attribution, timings, personal state, parsed ingredients, and optional pantry-item links.
- `PriceHistoryEntity`: local receipt/manual purchase observation.
- `BudgetWeeklyEntity`: Monday-anchored weekly spending target.

Room schema version 6 adds inventory stock settings, meal recipe/serving fields, recipes, price history, and weekly budgets. Migration 5→6 preserves existing batches, meals, and shopping data while assigning safe defaults.

## Current constraints

- Imported ingredient quantities and pantry quantities do not always share comparable units. PantryPal labels uncertain matches **Check stock** instead of claiming a precise shortage.
- The default rotation is four weeks (A–D). Choosing **Make current** anchors that template to the current Monday; the rotation advances each Monday.
- TheMealDB key `1` is suitable for development/education. A public store release needs a production/supporter key.
- Household sharing currently exchanges complete checksummed snapshots. The transport boundary and conflict model are ready, but real-time multi-device sync still needs an opt-in backend and account/security design.
- Receipt recognition is review-first: users confirm names, quantities, and prices before anything is stored.
