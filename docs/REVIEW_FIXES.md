# Repository review fixes, September 2026

## Implemented
- Shopping-only live sync no longer calls full backup restore. Shopping records, sections, week labels and aisle settings merge transactionally in Firestore.
- SQLite triggers record each shopping change in the same transaction as the data. A persistent in-flight batch and server receipt prevent a lost acknowledgement replaying an old edit. Deletes have tombstones. Remote application preserves newer pending local edits and translates shared section identities to local database IDs.
- Different-item edits are preserved; for simultaneous same-record changes, the last server-accepted write wins. This is not field-level conflict resolution.
- Errors, pending state, manual retry, sign-out and device disconnect are visible. Same-Google-account devices are not suppressed. Device identity and household selection are excluded from Android backup/transfer.
- Join explicitly replaces only shopping data after saving a full pre-join backup. Export that safety file from Household and restore it through Data management if required.
- The shopping screen shows real sync state. Empty sections retain add/manage controls, and items can move between planning sections.
- My Aldi has an editable aisle order and name-based assignments independent of recurrence/week sections. New items with a remembered name inherit the assignment. Unknown or removed aisle assignments appear under Unassigned. The initial order is an example, not an actual branch map.
- Shopping rows are lazy and keyed, with placement animation. QR pixels and receipt image loading moved off the main thread; recipe ranking and inventory grouping use background dispatchers.
- Navigation survives Activity recreation. Household setup is scrollable and keyboard-aware. Barcode scanner owns and disposes its camera use cases, scanner and executor.
- CI builds debug and shrunk unsigned release variants, reports APK bytes, and runs JVM, Android Room and Firebase rules tests.

## Rollout requirements
1. Export a complete backup on both devices before upgrading.
2. Verify the Firestore rules tests and deploy firebase/firestore.rules to the intended existing project. These rules block old snapshot publishers, permit only household-member state access, and validate invitation redemption without letting a joiner replace the owner. No production rules were changed as part of authoring this PR.
3. Upgrade both devices together. The first v2 sync converts the shopping portion of a legacy cloud snapshot and removes the cloud snapshot field. Old apps are incompatible with v2 and must not keep publishing.
4. Use a consistently signed APK with its certificate fingerprint registered in Firebase. A debug/unsigned release build does not prove Google sign-in is correctly configured.
5. Confirm both devices show Shopping list synced; test concurrent different-item checks, offline changes/reconnect and a newly created section before relying on it for a real shop.

Rules test setup follows the [Firebase Emulator documentation](https://firebase.google.com/docs/rules/unit-tests). Run `cd firebase && npm install && npm test`; this uses a demo project and does not deploy production rules. Run Android tests with `./gradlew :app:connectedDebugAndroidTest` on an API 35 emulator, and JVM/build checks with `./gradlew :app:testDebugUnitTest :app:assembleDebug :app:assembleRelease`.

## Remaining priorities
### P1: release/configuration and real account verification
The latest pre-review release run failed at Prepare release signing and listed all four signing secrets as missing. That historical result does not establish whether secrets have since been supplied. Production rule deployment and a two-device Google-login exercise still require access to the actual Firebase configuration. Device disconnect is local, not server-side membership revocation.

### P2: extend the sync model only where needed
Pantry, meals, recipes, archives and device settings are deliberately excluded from automatic sync. Full kitchen copying remains an explicit backup operation. If shared pantry/planning is wanted, add stable identities, conflict policies and durable mutations for those entities rather than reintroducing automatic full restore.

### P2: scale and conflict policy
To preserve compatibility with the current household state path, individual records and tombstones remain in one transactionally updated Firestore document. Only changed records are logically merged, but the wire document is still sent as a whole. There is an 850,000-byte guard. Move to separate record documents and bounded tombstone retention before history approaches that guard. Last-accepted same-record edits can supersede one another; field-level policies would improve simultaneous quantity/name/check edits.

### P2: data and UI reliability outside shopping
Move stock consumption/logging/restock into a single repository transaction. Standardise initial-loading and save/error states across inventory, meal planning and manual add; retain forms on failed saves. Add migration coverage for older database versions. Break up MainActivity and larger screen files incrementally.

### P3: adaptive layouts and profiling
The navigation rail and width cap remain; recipe list/detail panes and wider meal-planning layouts are still opportunities. No frame-time/FPS improvement is claimed without a physical-device trace. Test large fonts, landscape, scanner exit and two-device live use before release.
