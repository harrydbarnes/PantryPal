# Recipe, receipt, budget and household tools

## Weekly loop

1. Start with what is at home and what needs using soon.
2. Pick or import recipes and add them to a rotation week.
3. Review a pantry-aware shopping build.
4. Shop, scan the receipt, and put purchases away.
5. Use price history and a weekly target to inform the next shop.

The loop remains local-first. Online recipe/barcode lookups are explicit, and complete data leaves the device only through a file chosen by the user.

## Recipe safety and attribution

- Imported pages must expose schema.org `Recipe` JSON-LD and retain source attribution.
- Online catalogue results are attributed to TheMealDB.
- Imports are reviewed before saving.
- Ingredient parsing is tolerant and deterministic; uncertain unit comparisons become stock checks.
- Ratings, favourites, and cooked timestamps are local personal data.

## Receipt and budget contract

- ML Kit text recognition runs on the selected image; PantryPal does not retain the receipt image.
- Parsing is conservative. Totals, payments, tax, dates, and loyalty lines are metadata, not products.
- Every candidate is editable and can be excluded before import.
- Accepted lines create price observations and pantry quantities in one confirmed action.
- Repeated observations are normalized to unit prices so the shopping tools can show changes between shops without mixing incompatible units or currencies.
- Weekly summaries use Monday boundaries and one ISO currency; no exchange-rate assumptions are made.

## Portability and collaboration

- Backups are versioned, complete, readable JSON.
- Restore validates first, asks for confirmation, and performs one Room transaction in foreign-key order.
- Household snapshots add revision metadata and an integrity checksum.
- Current household sharing is portable snapshot exchange, not secure real-time sync.
- A future backend must add authentication, membership/permissions, encryption, and durable per-record revision delivery before enabling the transport boundary.
