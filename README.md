# PantryPal

PantryPal is a local-first Android kitchen companion for pantry stock, meal planning, recipes, shopping lists, receipts, and budgets.

## What it does

- Keeps pantry inventory, expiry dates, low-stock prompts, and consumption history.
- Plans meals in a four-week rotation and turns meals into shopping items.
- Provides an in-store shopping checklist with quick add, history suggestions, sections, budgets, receipt review, and price history.
- Supports barcode scanning, recipe discovery/import, and on-device receipt text recognition.
- Lets two people share one live shopping list: each signs in with their own Google account, then creates or joins a household using a QR/invite.

Household sync is opt-in and shopping-list only. Pantry, meal plans, recipes, receipts, budgets, and backups remain local to each device.

## Project documentation

- [App overview](APP_OVERVIEW.md): user journeys, architecture, data model, Firebase sync and release status.
- [Database design](DATABASE_DESIGN.md)
- [Meal planner design](MEAL_PLANNER_DESIGN.md)
- [Recipe and data tools design](RECIPE_AND_DATA_TOOLS_DESIGN.md)
- [Firestore security rules](firebase/firestore.rules)

## Build and release

The debug workflow builds an APK on pushes. **Release Android Bundle** creates a signed Android App Bundle and signed APK artifact.

Release signing is configured through GitHub Actions secrets. Never commit the keystore or its passwords. Google sign-in in a release build also requires the release signing SHA-1 to be registered in Firebase.

## Technology

Kotlin, Jetpack Compose, Material 3, Room, WorkManager, CameraX, ML Kit, Firebase Authentication, Cloud Firestore, and Credential Manager.

## Current scope

The app is designed for a private household. Real-time syncing is intentionally limited to shopping changes, with local-first queuing and retry behaviour for intermittent connectivity.
## Keep and voice shopping

In Google Keep, open a shopping checklist and use **Send → PantryPal**. PantryPal imports unticked lines into the selected shopping week, accepts `Milk x2`, `Bread (3)` and measured quantities, and ignores ticked checkboxes. Sharing recipe URLs still opens recipe import. Each incoming share is committed once across recreation or retry.

**Settings → Google Keep & voice shopping** offers optional read-only official Keep API imports. This requires an eligible Workspace account, administrator-approved Keep scopes, the Keep API enabled in Google Cloud, and an Android OAuth client for the app's package and signing certificate. A personal Gmail account cannot gain enterprise API access just by signing into PantryPal. Use the Share Target instead. Access tokens are obtained through Google Play services, used only in memory and never stored in preferences or WorkManager input. Imports run hourly and on request; denied access produces a settings status instead of repeated prompts. Notes containing “Shopping” in their title are paginated, and unchanged lines are not added repeatedly. PantryPal does not modify or tick Keep notes.

Android App Actions declares `CREATE_ITEM_LIST` and `GET_ITEM_LIST` in `shortcuts.xml`. Direct insertion accepts `voice_item_names` as a string, string array or string list. It also supports `com.example.pantrypal.OPEN_SHOPPING`. Google Assistant availability requires Play distribution and App Actions approval; declaring capabilities does not guarantee that Google or a Nest speaker will invoke them.

The Android 16+ AppFunctions entry point is KSP-generated from `integration.PantryPalVoiceFunctions`, with `addShoppingItems(items, store)` and a permission-protected generated `PantryPalAgentService`. It accepts Tesco, Aldi or Either, and inherits saved store preferences if omitted. The service is disabled on older Android versions. Gemini discovery still depends on Google's agent rollout and account eligibility. To inspect registered functions on a supported device, use `adb shell cmd app_function list-app-functions` and find `com.example.pantrypal`.

### Shopping data compatibility

Room v10 extends the existing shopping table, retaining shopping IDs, sync IDs, weeks, sections, units and purchase tracking. `ShoppingListEntity` is the canonical entity; `ShoppingItemEntity` remains a Kotlin compatibility alias. Its stock foreign key references the existing `inventory.inventoryId` and uses `SET_NULL` on deletion. Product names/categories remain in the existing `items` catalogue; store preferences live both on stock batches and catalogue products so they survive an empty batch. Names are indexed and transactionally deduplicated amongst compatible unticked entries in the active week. A global unique name constraint would destroy valid entries in different weeks, sections or measures, so it is deliberately not applied.

**Complete Shop & Restock** completes only ticked purchases in the selected week/store view, archives them, updates local stock once (including receipt-linked purchases), and removes them. **Remove Ticked Only** retains the archive without adding stock. Either-store items appear in both Tesco and Aldi views. Swipe a pantry row towards the left to mark its batch empty, log consumption and add it to shopping; Undo restores all three changes atomically and refuses to overwrite subsequent edits.

Build requirements: Gradle 9.3.1, AGP 9.1.1 and compile SDK 37 support the current AppFunctions and Material 3 Expressive artefacts. AGP provides Kotlin support; the existing Kotlin/Compose compiler version is retained. Minimum SDK 26 and target SDK 35 are unchanged. CI exercises Android 15 and Android 16, including share/voice activity recreation and permission-protected generated service metadata.
