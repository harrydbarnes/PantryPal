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