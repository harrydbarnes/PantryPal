# P2 and P3 follow-up

Base: main after PR #72, commit a386cade525a28f39dbcb2b4de5cb1ccc7272156.

## Data reliability

- `StockOperations` reads the current inventory row inside a Room transaction. Consumption, history and automatic restock commit together. Bulk operations roll back as a unit. A repeated finish on an already consumed batch does nothing.
- Finishing a shopping trip now includes putting purchases away and archiving/resetting the shopping list in the same transaction. Shopping additions use Room transactions rather than an independent lock when a database is available.
- Restock preferences and storage location save together. Inventory quantity/opened edits read the latest row transactionally.
- Manual-add and meal-editor forms stay open until success; failures retain input. Add-another clears only after success. Saves expose working/error state. Inventory, meal and history reads have a shared loading/error/retry banner; action failures no longer escape their coroutine.
- Inventory UI is extracted from MainActivity. Cloud storage and stock operations have their own classes with focused responsibilities.

## Shopping protocol v4

`ShoppingCloudStore` stores records in `households/{id}/shoppingRecords` and retry receipts in `shoppingDevices`. The existing `state/current` document is a small protocol/phase/revision control record. Record document IDs hash the logical key, so names containing slashes are safe.

Migration first freezes v2 in a transaction, retaining its original seed and receipts. It writes deterministic chunks of 100 and marks v3 ready, then v4 backfills a revision on each existing record before enabling incremental reads. A restart or another device can resume either stage. Once ready, rules prevent an older app from overwriting the result. Existing retry receipts transfer with the data, preventing stale replay after a lost acknowledgement. Oversized pre-upgrade batches use separate durable chunk receipts.

New offline batches contain at most 100 records. Writes send changed records and stamp them with the next control revision. Each device persists its last accepted revision after applying the response to Room, and remembers that its one-time protocol migration completed. Later exchanges query only records newer than that cursor; an idle exchange reads no shopping-record documents and skips migration reads. A control revision validates each response against concurrent writes. Same-record edits still use last-server-accepted-write wins; distinct records merge.

Tombstones become eligible for deletion after 30 days. Each device checks for cleanup at most once a day, queries only expired tombstones, then rechecks and removes up to 100, never a record restored since the query. The control document records the oldest safely retained revision. A device behind that watermark receives one authoritative snapshot, including absent records, so it removes stale rows before resuming deltas. Pending offline edits are preserved and may restore a previously deleted item when eventually accepted. Device receipts are retained to preserve retry safety.

**Trade-off:** first sync, v4 migration and recovery after falling behind tombstone retention still read a complete snapshot. Each sync also reads the small control document and runs an expired-tombstone query. Individual record payloads have a 100 KB limit; receipts can grow with new device identities.

## Adaptive layouts and performance

- Meal plans use adaptive day columns. Increasing font scale increases the minimum column width.
- At sufficiently wide content widths, recipe details sit beside the library. Narrow or large-font layouts use a scrollable dialog. Long recipe actions remain reachable.
- Main content can use up to 1200 dp. Phone navigation remains unchanged.
- CI captures tablet recipe screenshots and a focused 200-recipe scrolling run with gfxinfo frame counters and memory data. These are a single debug-emulator sample, not a physical-phone benchmark or proof of improvement over #72.
- `scripts/profile-android.sh SERIAL OUTPUT_DIRECTORY` captures a 30-second shopping flow with Perfetto, gfxinfo and memory on a connected phone. Open shopping first; the script prompts before capture. Compare repeated runs on the same phone/build settings. No physical phone is attached to this workspace.

## Validation and rollout

CI runs JVM tests, debug/shrunk unsigned release builds, Room rollback/concurrency tests, form and adaptive UI tests, Firebase security rules tests and the production Kotlin cloud transport against a demo Auth/Firestore emulator. Test credentials/project IDs are isolated from production. Debug builds allow HTTP for those local emulators; release network policy is unchanged.

Deploy the matching v4 rules and index, then upgrade both phones together after backing up. The rules intentionally reject v2 and v3 publishers after v4 migration begins. Rule/index deployment, signing certificates and real Google sign-in remain separate P1 release work and are not changed by this PR. The household still shares shopping items, sections, week labels and aisle preferences only.

## Remaining checks

Physical-device Google sign-in, simultaneous shopping, camera entry/exit and frame traces still need the phones. CI layout captures cover specific sizes and flows, not every tablet or accessibility configuration. Full shared pantry/recipe/meal-content sync is not introduced. Receipt retention still requires an explicit recovery policy before further scaling.
