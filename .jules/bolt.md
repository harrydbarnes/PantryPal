## 2024-05-22 - SQL Aggregation for Statistics
**Learning:** Moving statistical calculations (like average consumption interval) from Application layer (Kotlin) to Database layer (SQL) significantly reduces memory overhead by avoiding loading large datasets.
**Action:** When calculating derived data from large tables, always prefer SQL aggregation (`GROUP BY`, `HAVING`) over fetching all rows and filtering in memory. Use `ConsumptionDao.getRestockCandidates` as a reference.
