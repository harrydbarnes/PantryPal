# PantryPal app overview

PantryPal is a local-first Android kitchen companion built with Kotlin, Jetpack Compose, Material 3, Room, WorkManager, CameraX, and ML Kit. It tracks cupboard inventory, barcode-based additions and consumption, expiry warnings, shopping items, consumption history, and a rotating meal schedule.

## User journeys

- **Dashboard:** see expiring stock and restock suggestions, then open the cupboard.
- **First run:** move through a five-step onboarding that explains pantry tracking, scanning, the four-week meal rotation, shopping-list generation, local storage, and permissions.
- **Kitchen cupboard:** view inventory and record an item as finished or wasted.
- **Scan in / scan out:** identify products by barcode and add or consume them.
- **Meal plan:** maintain a four-week rotating schedule, give each week a name and emoji, change individual days, reuse meals or whole weeks, and build the week's shopping list.
- **Shopping:** review recurring sections, meal-derived ingredients, and extra items for the selected week; create custom sections and reuse remembered item names.
- **Past items:** review consumption history.
- **Settings:** choose system/light/dark appearance, control dynamic colour and expiry reminders, review notification access and local-data behaviour, replay onboarding, and view build/version information.

## Architecture

The app uses a compact single-module architecture:

1. Compose screens render state and send user events to `MainViewModel`.
2. `MainViewModel` owns UI-facing state flows, preferences, and application actions.
3. `KitchenRepository` is the boundary around Room DAOs and Open Food Facts.
4. `KitchenDatabase` stores items, inventory batches, consumption, shopping sections/items/history, week templates, and meals.
5. `ExpirationWorker` performs scheduled expiry checks.

The app currently uses manual screen state in `MainActivity` rather than Navigation Compose. Data is stored on-device; Open Food Facts is only contacted for an unknown scanned barcode.

## Material 3 design system

PantryPal uses a warm "garden pantry" Material 3 theme with complete light and dark semantic color roles, dynamic color on Android 12+, an emphasized type scale, and generous rounded shapes. Reusable UI building blocks live in `ui/components/ExpressiveComponents.kt`:

- `ExpressiveHero` gives each main journey a clear, friendly opening moment.
- `StatusPill` communicates small counts and states without relying on color alone.
- `SectionHeading` establishes a consistent content hierarchy.
- `FriendlyEmptyState` turns empty data into useful, positive guidance.
- `PantryPalSpacing` keeps screen rhythm on the 8dp spacing system.

Compact windows use a four-destination bottom navigation bar for Home, Pantry, Plan, and Shop; windows at 600dp and above switch to a navigation rail with the same stable hierarchy. Scan in and Scan out are contextual Pantry actions rather than destinations, and their focused camera screens use high-contrast guidance without persistent navigation competing for attention.

Onboarding completion is stored in the existing `pantry_prefs` preference file. First-time users see the walkthrough before the normal app shell; skipping or finishing marks it complete. Settings can replay the walkthrough without clearing the completion flag. The Android 13+ notification permission request is deferred until onboarding closes, while camera permission remains contextual to Scan in and Scan out.

Settings preferences use the same local preference file and apply immediately. Theme mode can follow Android or force light/dark, dynamic colour can be disabled independently on supported devices, and turning expiry reminders off cancels the unique daily WorkManager job. The expiration worker also checks the stored preference before sending a notification so an already-running job respects the user's choice.

## Data model

- `ItemEntity`: reusable product definition and dietary/category metadata.
- `InventoryEntity`: a quantity or batch of an item, optionally with an expiry date.
- `ConsumptionEntity`: finished or wasted history.
- `ShoppingSectionEntity`: ordered shopping group that can recur every week or hold week-specific entries.
- `ShoppingItemEntity`: checklist entry assigned to a section and, when relevant, a rotation week.
- `ShoppingHistoryEntity`: item-name memory used for quick-add suggestions even after an active entry is cleared.
- `MealWeekEntity`: editable week name, emoji, and stable rotation position.
- `MealEntity`: named meal assigned to a rotation week, weekday, meal slot, and shopping ingredients.

Room schema version 5 adds rotation-week metadata, shopping sections/history, and section/week ownership on shopping entries. The 4→5 migration preserves existing content, maps legacy essentials and Week A/B items, and seeds missing example data without replacing an existing meal plan.

## Current constraints

- Meal ingredients are editable shopping prompts rather than quantities linked to inventory products. Building a list creates a review checklist instead of calculating stock shortages.
- The default rotation is four weeks (A–D). Choosing **Make current** anchors that template to the current Monday; the ordered rotation advances automatically each Monday.
- There is no account or cloud sync. Backup/export currently covers core inventory data only.
