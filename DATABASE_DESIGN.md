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

### 6. ShoppingItem
Represents an item to review or buy.
- **TableName**: `shopping_list`
- **Fields**: name, quantity, unit, checked state, creation time, and frequency.

### 7. Meal
Represents a reusable entry in the two-week meal rotation.
- **TableName**: `meals`
- **Fields**:
    - `mealId`: Long (PK, AutoGenerate)
    - `name`: String
    - `week`: String (`A` or `B`)
    - `ingredients`: List<String> stored through a JSON type converter
    - `dayOfWeek`: Int (ISO weekday, Monday = 1)
    - `mealSlot`: String (`Breakfast`, `Lunch`, `Dinner`, or `Other`)

## Relationships
- **1 Item** has **Many InventoryItems**.
- **1 Item** has **Many ConsumptionEvents**.
- Meals are reusable templates. Their free-text ingredients are copied into the shopping checklist only when the user chooses **Build list**.

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
