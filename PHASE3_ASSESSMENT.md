# Phase 3 assessment (14 October 2026)

Assessed current main `13e72ee`, fetched before implementation. The original local checkout was `a3ba791`. Working tree was clean. This is a source-backed assessment; no current screenshots or running Android device are available.

## App assessment

The app already has a substantial local-first shopping platform: Room source of truth, unified manual/voice/Keep shopping table, durable operation receipts, Keep API contribution ledger, transactional completion and receipt reconciliation, stock operations, preparation drafts, configurable aisles, segmented rows, recurring/week/store scope, Firebase protocol v4, stable IDs, trigger journal, persistent batches, acknowledgements, field overlays, tombstone compaction and authoritative refresh. Preserve these implementations.

Concrete gaps found:

- Keep shares call additive upsert even for a fresh re-share of identical content. Feedback counts parsed lines rather than added records.
- Keep API unit changes add the whole contribution again, even for equivalent litres/millilitres.
- Empty-batch replacement ignores other batches and increments an existing request.
- Cloud exchange accepts stale base payloads over retained deletion markers. Local field overlay alone cannot protect cloud existence.
- Sync publishes success before checking for another pending batch. Room invalidation also schedules network work for applied remote rows.
- Onboarding has no household fast-path; starter setup is the only initial journey besides Skip.
- Quick entry sits in scrolling content, completion is an unguarded FAB, the existing saving flag does not cover these shopping actions, row and checkbox expose separate toggle actions, store/aisle actions add excessive row height, and list motion uses low-bouncy springs everywhere.

## Plan assessment and recommended scope

The attached plan correctly prioritises correctness before style, preserves the live sync pipeline and differentiates share imports from API contributions. Treat it as a verification matrix, not evidence that its guarantees already hold. Implement three separated phases:

1. Extend the existing import DAO with share-only maximum semantics, strict whitespace/case identity, compatible-unit duplicate consolidation, structured counts and accurate resource-based feedback. Convert API contribution deltas; reject incompatible unit changes without changing the receipt. Respect remaining stock and existing replacement requests.
2. Protect retained cloud deletions against stale edits, drive status from the durable queue, avoid remote-application upload loops, reconcile on foreground, and expose existing sign-in/invite joining during onboarding without discarding setup progress.
3. Bottom-dock quick entry with IME handling, bounded suggestions from products/history, guarded submission, scoped screen-on mode, one checkbox action per row, overflow secondary actions and restrained shared motion.

No new sync manager, shared-file sync, schema version, dependency upgrade or raised minimum SDK is required. Preserve phone bar/tablet rail and existing edge-to-edge entry point. Avoid a navigation framework migration solely for predictive animation.

## Boundaries and deferred work

Completion Undo is excluded: a safe inverse must compensate stock, purchase and archive changes while detecting subsequent edits; restoring rows alone is unsafe. Inventory remains device-local. Two devices may each restock their local pantry when completing a shared item before receiving its deletion; no household-wide exactly-once inventory guarantee is made.

Barcode candidate selection is a separate interaction/data change, not a cosmetic fix: barcode uniqueness, brand/variant evidence and a reviewed candidate-selection UI need dedicated device coverage. Existing barcode acquisition is preserved.

Tombstone compaction and stale unsent base creation need a protocol-level distinction between a new record and an old resurrected identity; retained-tombstone protection alone cannot guarantee that case. Do not claim exhaustive cross-device correctness from Room/unit tests. Keep current retention and document the remaining case.

Predictive gesture animation, foldable posture, TalkBack, 200% text, light/dark appearance and screenshots require emulator/device evidence. Existing custom back/navigation behaviour is preserved pending that evidence.
