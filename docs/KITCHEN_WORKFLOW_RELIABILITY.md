# Kitchen workflow reliability after PR #78

This follow-up implements the product reassessment in three batches on top of merged PR #78.

## Batch 1: correctness and data trust

- Convert compatible stock into the product unit; expose incompatible units as uncertainty instead of summing raw numbers.
- Use the same totals for pantry warnings, automatic restock and suggestions; exclude expired quantities.
- Exclude zero-quantity batches from expiry queries and classify notification expiry by calendar day.
- Block full backup and kitchen-snapshot restore while connected to live shopping, including concurrent joins/creates.
- Keep export working state until the destination stream has written and closed; report write failures without a success receipt.

## Batch 2: useful setup and accurate Settings

- Named starter dinners require ingredients and save into the current rotation week.
- Optional first pantry stock and expiry provide a starting point for pantry-aware shopping.
- Save kitchen setup transactionally and finish only after success; failed saves retain setup input.
- Replaying setup preserves reminder preferences and is explicitly labelled Edit setup.
- Settings explains local kitchen data versus optional shared shopping and displays connection status; specialised Keep controls follow common settings.
- Empty/undated pantry states explain missing information instead of assuring freshness.

## Batch 3: daily usage and cooking

- Enter usage/waste amounts in batch units or choose the whole batch; preserve history separately from corrections.
- Scan-out accepts weighed/liquid quantities, retains drafts through recreation/failures and closes only after success.
- Manual products offer reuse of an exact-name, compatible-unit product or an explicit distinct-product choice.
- Recipe and meal cooking review scales portions, honours pantry links, allocates stock once, permits edits and explains unknown/incompatible/insufficient stock.
- Confirmation atomically saves usage, restock and cooked records after checking reviewed snapshots. Logging cooking without stock changes remains available.
- Planned serving changes scale measured shopping ingredients. Unmeasured ingredients remain explicit review work.
- Planned cooked dates and usage units survive backup/restore; copied meals start uncooked.
- Single-recipe coverage explains its scope and points users to whole-week review. Budget copy identifies local recorded spending.

## Validation

Regression coverage includes mixed mass/volume quantities, incompatible and expired stock, consumed-batch expiry exclusion, connected restore isolation, setup rollback/current-week placement, explicit product reuse, cooking portions and pantry links, insufficient shared ingredients, atomic rollback, stale-review rejection, log-only cooking and cooked/unit backup round trips. Native UI tests cover setup reminder retention, ingredient requirements, save completion, connected Settings copy and failed cooking review retention. Existing migration tests exercise upgrades through schema 11.

Local Gradle cannot initialise because Java reports `Unable to establish loopback connection`, including outside the sandbox. Android CI is used for compilation, unit tests, instrumented emulator tests, debug/release assembly and lint. CI screenshots provide inspectable native layout evidence for setup, Settings and cooking review.

Production Google sign-in/Keep authorisation, physical camera/OCR, actual destination-provider failures and two physical household devices remain live verification boundaries. Shared pantry/recipe/meal state is intentionally outside this follow-up; live sync continues to cover shopping only.
