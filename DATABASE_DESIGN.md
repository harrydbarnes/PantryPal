# KitchenLocal Database Design

## Entities

### 1. Item
Represents a product definition (e.g., "Whole Milk").
- **TableName**: `items`
- **Fields**:
    - `itemId`: Long (PK, AutoGenerate)
    - `name`: String
    - `barcode`: String? (Nullable, for scanning)
    - `defaultUnit`: String (e.g., "pcs", "liters", "kg")
    - `category`: String (e.g., "Dairy", "Pantry")
    - `isVegetarian`: Boolean (Dietary tag)
    - `isGlutenFree`: Boolean (Dietary tag)
    - `isUsual`: Boolean (Manual override for "Usuals")

### 2. InventoryItem
Represents a specific instance of an item in the kitchen.
- **TableName**: `inventory`
- **Fields**:
    - `inventoryId`: Long (PK, AutoGenerate)
    - `itemId`: Long (FK -> items.itemId, OnDelete=CASCADE)
    - `quantity`: Double
    - `addedDate`: Long (Timestamp)
    - `expirationDate`: Long? (Timestamp)

### 3. ConsumptionEvent
Logs the removal of an item.
- **TableName**: `consumption_history`
- **Fields**:
    - `eventId`: Long (PK, AutoGenerate)
    - `itemId`: Long (FK -> items.itemId, OnDelete=CASCADE)
    - `date`: Long (Timestamp)
    - `quantity`: Double
    - `type`: String/Enum ("FINISHED", "WASTED")
    - `wasteReason`: String? (Required if type is WASTED)

### 4. Tag (Optional/Advanced)
If arbitrary tags are needed beyond the boolean flags in Item.
- **TableName**: `tags`
- **Fields**:
    - `tagId`: Long (PK)
    - `label`: String (Unique)

### 5. ItemTagCrossRef (Optional)
- **PrimaryKeys**: `itemId`, `tagId`

## Relationships
- **1 Item** has **Many InventoryItems**.
- **1 Item** has **Many ConsumptionEvents**.

## Logic for Features

### Restock Cycles ("Usuals")
- **Query**: Select `itemId` from `consumption_history` where `type` = 'FINISHED'.
- **Analysis**: Calculate the average time interval between consecutive `date`s for the same `itemId`.
- **Threshold**: If variance is low and average interval is stable, mark as "Usual" (or suggest it).
- **Highlight**: If `(LastConsumedDate + AverageInterval) < CurrentDate`, highlight as "Needs Restock".

### Backup
- **Export**: Query all tables, serialize to JSON using Gson/Kotlinx.Serialization.
- **Format**:
  ```json
  {
    "items": [...],
    "inventory": [...],
    "history": [...]
  }
  ```
