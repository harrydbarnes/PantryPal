# PantryPal release shrinking
#
# Keep the names of Gson-backed fields because these JSON formats are persisted,
# shared between devices, exported, or supplied by external APIs. The classes
# remain eligible for normal R8 reachability/shrinking; only their reflective
# field contract is protected.
-keepattributes Signature
-keepattributes RuntimeVisibleAnnotations,RuntimeInvisibleAnnotations,AnnotationDefault

# Full backups and household snapshots.
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupDocument { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupPayload { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupItem { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupInventory { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupConsumption { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupShoppingSection { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupShoppingItem { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupShoppingHistory { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupMealWeek { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupMeal { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupRecipe { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupRecipeIngredient { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupPriceHistory { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupWeeklyBudget { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupShoppingLocation { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.backup.BackupPreferences { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.household.HouseholdSnapshotEnvelope { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.household.HouseholdSnapshotPayload { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.domain.household.HouseholdEventMetadata { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.domain.household.HouseholdRecordVersion { <fields>; }
-keepclassmembers,allowoptimization enum com.example.pantrypal.domain.household.HouseholdCollection { <fields>; }
-keepclassmembers,allowoptimization enum com.example.pantrypal.domain.household.HouseholdOperation { <fields>; }

# Existing JSON export and local shopping-location storage.
-keepclassmembers,allowoptimization class com.example.pantrypal.data.repository.ExportData { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.entity.ItemEntity { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.entity.InventoryEntity { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.entity.ConsumptionEntity { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.entity.ConsumptionType { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.util.ShoppingLocation { <fields>; }

# Gson Retrofit response models.
-keepclassmembers,allowoptimization class com.example.pantrypal.data.api.RecipeTheMealDbResponse { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.api.RecipeTheMealDbMeal { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.api.ProductResponse { <fields>; }
-keepclassmembers,allowoptimization class com.example.pantrypal.data.api.ProductData { <fields>; }

# WorkManager persists these class names and the custom factory compares them
# as strings, so keep the names stable across a release update.
-keepnames class com.example.pantrypal.util.ExpirationWorker
-keepnames class com.example.pantrypal.util.ShoppingReminderWorker

# Preserve fields explicitly mapped by Gson annotations in any future model.
-keepclassmembers,allowoptimization class * {
    @com.google.gson.annotations.SerializedName <fields>;
}
