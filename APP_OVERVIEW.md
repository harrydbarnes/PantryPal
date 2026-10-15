# PantryPal app overview

PantryPal is a local-first Android kitchen companion built with Kotlin, Jetpack Compose, Material 3, Room, WorkManager, CameraX, Retrofit, and ML Kit. It connects pantry inventory, a four-week meal rotation, recipes, shopping, receipt capture, and opt-in private household shopping sync.

## User journeys

- **Dashboard:** see expired/due-soon stock and aggregate low-stock suggestions, then add a restock item directly.
- **First run:** set a shopping day, reminder timing and regular essentials; add up to three familiar dinners with shopping ingredients and optionally record a first pantry item and expiry. Kitchen data saves transactionally before setup completes. The app opens the current rotation week; notification permission is requested for enabled reminders, while near-store reminders can be configured later in Settings at the shop. Edit setup preserves saved reminder choices.
- **Add an item:** enter only a name and quantity to add quickly. Unit, location and category start from the most recently used values; expand **Item details** for location, opened state, category and expiry, or **Preferences and restock** for dietary flags and low-stock reminders. The sticky action bar keeps **Add item** reachable and offers **Save and add another** for a run of entries. Manual additions matching an existing product ask whether to reuse its identity or create a distinct product; compatible batches then contribute to stock totals in the product’s default unit.
- **Kitchen cupboard:** search, filter, sort and stocktake batches; track storage location, opened state, expiry, always-stocked status, and low-stock thresholds.
- **Scan in / scan out:** identify products by barcode and add or consume a whole or partial quantity. Review editable amounts in each batch’s unit; weighed and liquid stock require an explicit amount. Draft scans survive recreation and remain available after a failed save. Pantry **Use…** and **Wasted** also accept an amount or the whole batch, separately from stock corrections.
- **Meal plan:** maintain four rotating weeks, rename and reorder the weekly rhythm, reuse meals or whole weeks, open the recipe book, and preview a pantry-aware shopping build. Changing planned servings scales measured ingredients; cooking from Plan opens the same stock-usage review as Recipes.
- **Recipes:** reuse meals as saved recipes, search locally, see cook-now/use-soon/missing-one-or-two ideas, search TheMealDB, import schema.org recipe links, favourite/rate recipes, add a recipe to the plan, and send missing ingredients to shopping. **Cook / record usage** scales portions, proposes deductions from matching in-date batches and allows edits before an atomic stock/history/cooked-record save. Unknown quantities or incompatible units require review; users can instead record cooking without changing stock. A single-recipe check does not reserve stock for other meals; use weekly review when planning several dishes.
- **Shopping:** see the active rotation week, date range and remaining count in a persistent header. Quick add understands entries such as `Milk x2` and `500 g pasta`, marks suggestions already on the list, and offers undo after adding or increasing a quantity. Unchecked items come first within each section or aisle; checked items and the per-row aisle/section controls remain visible. Aisle mode is remembered, with category-based suggestions and separate editable shop profiles (My Aldi is the default). Use **Plan & prepare** for rotation-week choice, receipt and budget tools.
- **Preparing a list:** Plan and Shop open the same scrollable review. Compatible ingredient quantities are totalled across meals, expired pantry stock is excluded, and existing shopping quantities cover only the compatible amount. Review every ingredient with **Have it**, **Need it** and an editable buy quantity. Unresolved checks stay labelled on the shopping row. The review shows additions, changes and removals; purchased items stay ticked and manual edits are protected unless replacement is explicitly selected. If the shared list changes during review, it must be reviewed again before saving.
- **Finishing a shop:** the persistent **Finish shop** action saves checked purchases to history, with optional pantry updates, suggested per-item storage locations and bulk location changes. A purchase review that has become stale is rejected. Completing the same purchases twice does not duplicate stock or history. Finished recurring essentials remain ticked until **Prepare next shop** is selected; that action leaves newly purchased but unfinished items alone.
- **Receipts and budget:** select an image for on-device text recognition or paste receipt text, correct uncertain names, quantities and prices, add purchases to pantry and price history, compare unit-price changes, and track weekly spending against a target.
- **Past items:** review consumption history.
- **Settings and data:** control appearance/reminders, export or restore a complete backup, sign in with Google, create or join a private household shopping list by QR/invite, replay onboarding, and view build information.

## Architecture

The app remains a compact single Android module:

1. Compose screens render state and send user events to `MainViewModel` or `PantryFeaturesViewModel`.
2. `MainViewModel` owns the core pantry, meal-plan, and shopping workflows.
3. `PantryFeaturesViewModel` owns recipe discovery, receipt review, budgets, backups, and household collaboration state.
4. `KitchenRepository` is the core boundary around Room and Open Food Facts. `PantryFeaturesRepository` coordinates the linked feature workflows.
5. `KitchenDatabase` stores the complete local kitchen and exposes a dependency-aware backup DAO.
6. `ExpirationWorker` performs scheduled expiry checks.

The app uses manual screen state in `MainActivity` rather than Navigation Compose. Data is stored on-device first. Network requests occur only for an unknown scanned barcode, an explicit online recipe search, an explicitly imported recipe URL, or opt-in Firebase household shopping sync.

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

Room schema version 11 adds the cooked date on planned meals and optional units on consumption events. Migration 10→11 preserves existing rows with null values; older history displays that its unit was not recorded. Complete backups include both fields.

## Current constraints

- Imported ingredient quantities and pantry quantities do not always share comparable units. PantryPal labels uncertain matches **Check stock** instead of claiming a precise shortage.
- The default rotation is four weeks (A–D). Choosing **Make current** anchors that template to the current Monday; the rotation advances each Monday.
- TheMealDB key `1` is suitable for development/education. A public store release needs a production/supporter key.
- Household sharing is opt-in and currently syncs the shopping list only. Each person signs in with their own Google account, then joins the same household by QR/invite. Meal plans, pantry inventory, receipts, budgets, and backups remain device-local.
- Household sync is local-first: shopping changes queue locally while offline and retry with backoff. The protocol exchanges deltas, retains a cloud revision cursor, and compacts cloud history daily. It is designed for a trusted two-person household, not collaborative editing by a large group.
- Receipt recognition is review-first: users confirm names, quantities, and prices before anything is stored.


## Firebase household sync and release status

### Household sync

Google sign-in uses Android Credential Manager, Firebase Authentication, and Cloud Firestore. A signed-in owner creates a household; a second signed-in person joins through a QR code or complete invite string. Firestore rules restrict household state to those member UIDs.

`FirebaseHouseholdSync` is intentionally shopping-only. It preserves the local Room database as the source of truth, journals shopping edits, sends deltas, listens for remote state, and retries failed transfers. Joining saves a local backup before replacing the device's shopping state.

### Firebase project

- Firebase project: `pantrypal-12e55`
- Android package: `com.example.pantrypal`
- Google provider: enabled in Firebase Authentication
- Firestore rules: `firebase/firestore.rules`
- Android configuration: `app/google-services.json`

### Signed releases

The GitHub Actions **Release Android Bundle** workflow creates a signed AAB and an installable signed APK. It requires these repository secrets:

- `PANTRYPAL_RELEASE_KEYSTORE_BASE64`
- `PANTRYPAL_RELEASE_STORE_PASSWORD`
- `PANTRYPAL_RELEASE_KEY_ALIAS`
- `PANTRYPAL_RELEASE_KEY_PASSWORD`

Keep the keystore and its passwords outside the repository. The release signing SHA-1 must be registered on the Firebase Android app for Google sign-in to work in a release APK. The current signing SHA-1 is `7C:7B:22:1D:FE:78:D1:09:E6:03:A4:1E:01:A7:34:53:11:D7:4F:23`.

The successful release workflow produces both `pantrypal-release-bundle-main` and `pantrypal-release-apk-main` artifacts.
## Kitchen workflow reliability follow-up

Full backup and kitchen-snapshot restoration require this device to be disconnected from live shopping. The guard is enforced in the repository and serialises against in-flight household membership changes. Successful restoration reloads current preferences and reminder scheduling state; backup export reports success only after the chosen file has been written and closed.

Stock totals and low-stock decisions convert compatible batches to the product’s default unit and exclude expired stock. Incompatible remaining measures are labelled **Check stock units**, rather than being summed or treated as zero. Expiry alerts ignore empty batches and use the same calendar-day classification as Pantry. An empty or undated cupboard gets an honest first-use state instead of an assurance that all food is in date.

Household sharing still synchronises shopping only. Receipt/price totals show recorded local spend, not complete automatic checkout accounting. See `docs/KITCHEN_WORKFLOW_RELIABILITY.md` for implementation batches and verification boundaries.
