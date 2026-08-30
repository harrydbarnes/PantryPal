package com.example.pantrypal.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.RoomDatabase.Callback
import com.example.pantrypal.data.dao.ConsumptionDao
import com.example.pantrypal.data.dao.InventoryDao
import com.example.pantrypal.data.dao.ItemDao
import com.example.pantrypal.data.dao.ShoppingDao
import com.example.pantrypal.data.dao.MealDao
import com.example.pantrypal.data.dao.MealWeekDao
import com.example.pantrypal.data.dao.BudgetWeeklyDao
import com.example.pantrypal.data.dao.BackupDao
import com.example.pantrypal.data.dao.PriceHistoryDao
import com.example.pantrypal.data.dao.RecipeDao
import com.example.pantrypal.data.dao.ShoppingHistoryDao
import com.example.pantrypal.data.dao.ShoppingSectionDao
import com.example.pantrypal.data.entity.BudgetWeeklyEntity
import com.example.pantrypal.data.entity.ConsumptionEntity
import com.example.pantrypal.data.entity.InventoryEntity
import com.example.pantrypal.data.entity.ItemEntity
import com.example.pantrypal.data.entity.PriceHistoryEntity
import com.example.pantrypal.data.entity.RecipeEntity
import com.example.pantrypal.data.entity.RecipeIngredientEntity
import com.example.pantrypal.data.entity.ShoppingArchiveEntity
import com.example.pantrypal.data.entity.ShoppingItemEntity
import com.example.pantrypal.data.entity.MealEntity
import com.example.pantrypal.data.entity.MealWeekEntity
import com.example.pantrypal.data.entity.ShoppingHistoryEntity
import com.example.pantrypal.data.entity.ShoppingSectionEntity
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.pantrypal.data.converter.Converters
import com.google.gson.Gson

@Database(
    entities = [
        ItemEntity::class,
        InventoryEntity::class,
        ConsumptionEntity::class,
        ShoppingItemEntity::class,
        ShoppingArchiveEntity::class,
        MealEntity::class,
        MealWeekEntity::class,
        ShoppingSectionEntity::class,
        ShoppingHistoryEntity::class,
        PriceHistoryEntity::class,
        BudgetWeeklyEntity::class,
        RecipeEntity::class,
        RecipeIngredientEntity::class
    ],
    version = 7,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class KitchenDatabase : RoomDatabase() {
    abstract fun itemDao(): ItemDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun consumptionDao(): ConsumptionDao
    abstract fun shoppingDao(): ShoppingDao
    abstract fun mealDao(): MealDao
    abstract fun mealWeekDao(): MealWeekDao
    abstract fun shoppingSectionDao(): ShoppingSectionDao
    abstract fun shoppingHistoryDao(): ShoppingHistoryDao
    abstract fun priceHistoryDao(): PriceHistoryDao
    abstract fun budgetWeeklyDao(): BudgetWeeklyDao
    abstract fun recipeDao(): RecipeDao
    abstract fun backupDao(): BackupDao

    companion object {
        @Volatile
        private var INSTANCE: KitchenDatabase? = null

        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add imageUrl to items table
                db.execSQL("ALTER TABLE items ADD COLUMN imageUrl TEXT DEFAULT NULL")

                // Create shopping_list table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `shopping_list` (
                        `shoppingId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `isChecked` INTEGER NOT NULL,
                        `addedAt` INTEGER NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add frequency to shopping_list table
                val defaultFreq = ShoppingItemEntity.FREQ_ONE_OFF
                db.execSQL("ALTER TABLE shopping_list ADD COLUMN frequency TEXT DEFAULT '$defaultFreq' NOT NULL")

                // Create meals table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `meals` (
                        `mealId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `week` TEXT NOT NULL,
                        `ingredients` TEXT NOT NULL
                    )
                """.trimIndent())
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE meals ADD COLUMN dayOfWeek INTEGER NOT NULL DEFAULT 1")
                db.execSQL("ALTER TABLE meals ADD COLUMN mealSlot TEXT NOT NULL DEFAULT 'Dinner'")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                createPlannerTables(db)
                db.execSQL("ALTER TABLE shopping_list ADD COLUMN sectionId INTEGER NOT NULL DEFAULT ${ShoppingSectionEntity.ID_THE_REST}")
                db.execSQL("ALTER TABLE shopping_list ADD COLUMN weekId TEXT DEFAULT NULL")
                db.execSQL(
                    "UPDATE shopping_list SET sectionId = ${ShoppingSectionEntity.ID_EVERY_WEEK} " +
                        "WHERE frequency = '${ShoppingItemEntity.FREQ_ESSENTIAL}'"
                )
                db.execSQL(
                    "UPDATE shopping_list SET weekId = 'A' WHERE frequency = '${ShoppingItemEntity.FREQ_WEEK_A}'"
                )
                db.execSQL(
                    "UPDATE shopping_list SET weekId = 'B' WHERE frequency = '${ShoppingItemEntity.FREQ_WEEK_B}'"
                )
                db.execSQL(
                    "INSERT OR IGNORE INTO shopping_history (normalizedName, displayName, lastUsedAt) " +
                        "SELECT lower(trim(name)), name, addedAt FROM shopping_list WHERE trim(name) != ''"
                )
                seedPlannerDefaults(db)
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN lowStockThreshold REAL DEFAULT NULL")
                db.execSQL(
                    "ALTER TABLE inventory ADD COLUMN storageLocation TEXT NOT NULL DEFAULT '${InventoryEntity.LOCATION_PANTRY}'"
                )
                db.execSQL("ALTER TABLE inventory ADD COLUMN isOpened INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE meals ADD COLUMN recipeId INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE meals ADD COLUMN servings REAL NOT NULL DEFAULT 4.0")
                createDataToolsTables(db)
                createRecipeTables(db)
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `shopping_archive` (
                        `archiveId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `tripId` TEXT NOT NULL,
                        `weekId` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `quantity` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `sectionName` TEXT NOT NULL,
                        `completedAt` INTEGER NOT NULL,
                        `storageLocation` TEXT
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_shopping_archive_tripId` " +
                        "ON `shopping_archive` (`tripId`)"
                )
                db.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_shopping_archive_completedAt` " +
                        "ON `shopping_archive` (`completedAt`)"
                )
            }
        }

        fun getDatabase(context: Context): KitchenDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KitchenDatabase::class.java,
                    "pantry_pal_db"
                )
                .addMigrations(
                    MIGRATION_1_2,
                    MIGRATION_2_3,
                    MIGRATION_3_4,
                    MIGRATION_4_5,
                    MIGRATION_5_6,
                    MIGRATION_6_7
                )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        seedPlannerDefaults(db)
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private fun createPlannerTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `meal_weeks` (
                    `weekId` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `emoji` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    PRIMARY KEY(`weekId`)
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `shopping_sections` (
                    `sectionId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `sortOrder` INTEGER NOT NULL,
                    `recursEveryWeek` INTEGER NOT NULL,
                    `systemKey` TEXT
                )
                """.trimIndent()
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `shopping_history` (
                    `normalizedName` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `lastUsedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`normalizedName`)
                )
                """.trimIndent()
            )
        }

        private fun createDataToolsTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `price_history` (
                    `priceId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `itemId` INTEGER,
                    `normalizedItemName` TEXT NOT NULL,
                    `displayName` TEXT NOT NULL,
                    `priceMinor` INTEGER NOT NULL,
                    `quantity` REAL NOT NULL,
                    `unit` TEXT NOT NULL,
                    `retailer` TEXT,
                    `purchasedAt` INTEGER NOT NULL,
                    `currencyCode` TEXT NOT NULL,
                    `source` TEXT NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_price_history_normalizedItemName` " +
                    "ON `price_history` (`normalizedItemName`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_price_history_purchasedAt` " +
                    "ON `price_history` (`purchasedAt`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_price_history_retailer` " +
                    "ON `price_history` (`retailer`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `weekly_budgets` (
                    `weekStartEpochDay` INTEGER NOT NULL,
                    `budgetMinor` INTEGER NOT NULL,
                    `currencyCode` TEXT NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    PRIMARY KEY(`weekStartEpochDay`)
                )
                """.trimIndent()
            )
        }

        private fun createRecipeTables(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recipes` (
                    `recipeId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `title` TEXT NOT NULL,
                    `normalizedTitle` TEXT NOT NULL,
                    `sourceUrl` TEXT,
                    `sourceName` TEXT,
                    `attribution` TEXT,
                    `externalId` TEXT,
                    `imageUrl` TEXT,
                    `yieldText` TEXT,
                    `servings` REAL,
                    `prepTimeMinutes` INTEGER,
                    `cookTimeMinutes` INTEGER,
                    `totalTimeMinutes` INTEGER,
                    `instructions` TEXT NOT NULL,
                    `tags` TEXT NOT NULL,
                    `rating` INTEGER,
                    `isFavourite` INTEGER NOT NULL,
                    `lastCookedAt` INTEGER,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE UNIQUE INDEX IF NOT EXISTS `index_recipes_normalizedTitle` " +
                    "ON `recipes` (`normalizedTitle`)"
            )
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `recipe_ingredients` (
                    `ingredientId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `recipeId` INTEGER NOT NULL,
                    `rawText` TEXT NOT NULL,
                    `name` TEXT NOT NULL,
                    `normalizedName` TEXT NOT NULL,
                    `quantity` REAL,
                    `unit` TEXT,
                    `isOptional` INTEGER NOT NULL,
                    `linkedPantryItemId` INTEGER,
                    `sortOrder` INTEGER NOT NULL,
                    FOREIGN KEY(`recipeId`) REFERENCES `recipes`(`recipeId`) ON UPDATE NO ACTION ON DELETE CASCADE,
                    FOREIGN KEY(`linkedPantryItemId`) REFERENCES `items`(`itemId`) ON UPDATE NO ACTION ON DELETE SET NULL
                )
                """.trimIndent()
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_recipeId` " +
                    "ON `recipe_ingredients` (`recipeId`)"
            )
            db.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_recipe_ingredients_linkedPantryItemId` " +
                    "ON `recipe_ingredients` (`linkedPantryItemId`)"
            )
        }

        private fun seedPlannerDefaults(db: SupportSQLiteDatabase) {
            createPlannerTables(db)
            val weeks = listOf(
                MealWeekEntity("A", "Week A", "🥔", 0),
                MealWeekEntity("B", "Week B", "🐟", 1),
                MealWeekEntity("C", "Week C", "🍕", 2),
                MealWeekEntity("D", "Week D", "🍝", 3)
            )
            weeks.forEach { week ->
                db.execSQL(
                    "INSERT OR IGNORE INTO meal_weeks (weekId, name, emoji, sortOrder) VALUES (?, ?, ?, ?)",
                    arrayOf<Any?>(week.weekId, week.name, week.emoji, week.sortOrder)
                )
            }

            val sections = listOf(
                ShoppingSectionEntity(ShoppingSectionEntity.ID_EVERY_WEEK, "Every week", 0, true, ShoppingSectionEntity.KEY_EVERY_WEEK),
                ShoppingSectionEntity(ShoppingSectionEntity.ID_BABY_STUFF, "Baby stuff", 1, true, ShoppingSectionEntity.KEY_BABY_STUFF),
                ShoppingSectionEntity(ShoppingSectionEntity.ID_MEAL_PLAN, "Meal plan", 2, false, ShoppingSectionEntity.KEY_MEAL_PLAN),
                ShoppingSectionEntity(ShoppingSectionEntity.ID_THE_REST, "The rest", 3, false, ShoppingSectionEntity.KEY_THE_REST)
            )
            sections.forEach { section ->
                db.execSQL(
                    "INSERT OR IGNORE INTO shopping_sections (sectionId, name, sortOrder, recursEveryWeek, systemKey) VALUES (?, ?, ?, ?, ?)",
                    arrayOf<Any?>(section.sectionId, section.name, section.sortOrder, if (section.recursEveryWeek) 1 else 0, section.systemKey)
                )
            }

            seedShoppingItem(db, "Bananas", 1.0, "bunch", ShoppingSectionEntity.ID_EVERY_WEEK)
            seedShoppingItem(db, "Eggs", 3.0, "pcs", ShoppingSectionEntity.ID_EVERY_WEEK)
            seedShoppingItem(db, "Grapes", 1.0, "pack", ShoppingSectionEntity.ID_EVERY_WEEK)
            seedShoppingItem(db, "Raspberries", 1.0, "pack", ShoppingSectionEntity.ID_EVERY_WEEK)
            seedShoppingItem(db, "Broccoli", 1.0, "pcs", ShoppingSectionEntity.ID_EVERY_WEEK)
            seedShoppingItem(db, "Mozzarella", 1.0, "pack", ShoppingSectionEntity.ID_EVERY_WEEK)
            seedShoppingItem(db, "Formula milk", 0.0, "tubs", ShoppingSectionEntity.ID_BABY_STUFF)
            seedShoppingItem(db, "Size 5 nappies pull ups", 0.0, "packs", ShoppingSectionEntity.ID_BABY_STUFF)
            seedShoppingItem(db, "Sensitive wipes", 0.0, "boxes", ShoppingSectionEntity.ID_BABY_STUFF)

            val hasMeals = db.query("SELECT EXISTS(SELECT 1 FROM meals LIMIT 1)").use { cursor ->
                cursor.moveToFirst() && cursor.getInt(0) == 1
            }
            if (!hasMeals) seedExampleMeals(db)
        }

        private fun seedShoppingItem(
            db: SupportSQLiteDatabase,
            name: String,
            quantity: Double,
            unit: String,
            sectionId: Long
        ) {
            db.execSQL(
                """
                INSERT INTO shopping_list (name, quantity, unit, isChecked, addedAt, frequency, sectionId, weekId)
                SELECT ?, ?, ?, 0, ?, ?, ?, NULL
                WHERE NOT EXISTS (
                    SELECT 1 FROM shopping_list WHERE lower(name) = lower(?) AND sectionId = ?
                )
                """.trimIndent(),
                arrayOf<Any?>(
                    name,
                    quantity,
                    unit,
                    System.currentTimeMillis(),
                    ShoppingItemEntity.FREQ_ESSENTIAL,
                    sectionId,
                    name,
                    sectionId
                )
            )
            db.execSQL(
                "INSERT OR IGNORE INTO shopping_history (normalizedName, displayName, lastUsedAt) VALUES (?, ?, ?)",
                arrayOf<Any?>(name.trim().lowercase(), name, System.currentTimeMillis())
            )
        }

        private fun seedExampleMeals(db: SupportSQLiteDatabase) {
            val meals = listOf(
                SeedMeal("A", 1, "Jacket potatoes", listOf("Jacket potatoes", "Baked beans", "Cheese")),
                SeedMeal("A", 2, "Broccoli pasta", listOf("Broccoli", "Pasta", "Mozzarella")),
                SeedMeal("A", 3, "Stir fry", listOf("Stir-fry vegetables", "Stir-fry sauce", "Noodles")),
                SeedMeal("A", 4, "Chilli con carne or Bolognese", listOf("Mince", "Cooking sauce")),
                SeedMeal("A", 5, "Fish & chips", listOf("Fish for fish and chips", "Chips")),
                SeedMeal("A", 6, "Indian curry", listOf("Rice", "Indian sauce")),
                SeedMeal("A", 7, "Sunday roast", listOf("Roasting joint", "Roast potatoes", "Carrots", "Yorkshire puddings", "Gravy")),
                SeedMeal("B", 1, "Broccoli pasta", listOf("Broccoli", "Pasta", "Mozzarella")),
                SeedMeal("B", 2, "Salmon and mash", listOf("Salmon", "Mash", "Green beans")),
                SeedMeal("B", 3, "Fajitas", listOf("Fajita wraps", "Fajita sauce", "Red onion")),
                SeedMeal("B", 4, "Bolognese", listOf("Bolognese sauce", "Mince", "Spaghetti")),
                SeedMeal("B", 5, "Tuna steak, chips and mixed veg", listOf("Tuna steak", "Chips", "Mixed vegetables")),
                SeedMeal("B", 6, "Chicken and rice", listOf("Chicken", "Rice")),
                SeedMeal("B", 7, "Sunday roast", listOf("Roasting joint", "Roast potatoes", "Carrots", "Yorkshire puddings", "Gravy")),
                SeedMeal("C", 1, "Jacket potatoes", listOf("Jacket potatoes", "Baked beans", "Cheese")),
                SeedMeal("C", 2, "Broccoli pasta", listOf("Broccoli", "Pasta", "Mozzarella")),
                SeedMeal("C", 3, "Salmon and mash", listOf("Salmon", "Mash", "Green beans")),
                SeedMeal("C", 4, "Pizza and garlic bread", listOf("Pizzas x2", "Garlic bread")),
                SeedMeal("C", 5, "Fish & chips", listOf("Fish for fish and chips", "Chips")),
                SeedMeal("C", 6, "Chilli con carne", listOf("Mince", "Chilli sauce", "Kidney beans", "Rice")),
                SeedMeal("C", 7, "Sunday roast", listOf("Roasting joint", "Roast potatoes", "Carrots", "Yorkshire puddings", "Gravy")),
                SeedMeal("D", 1, "Broccoli pasta", listOf("Broccoli", "Pasta", "Mozzarella")),
                SeedMeal("D", 2, "Salmon and mash", listOf("Salmon", "Mash", "Green beans")),
                SeedMeal("D", 3, "Fajitas", listOf("Fajita wraps", "Fajita sauce", "Red onion")),
                SeedMeal("D", 4, "Bolognese", listOf("Bolognese sauce", "Mince", "Spaghetti")),
                SeedMeal("D", 5, "Lasagne", listOf("Lasagne")),
                SeedMeal("D", 6, "Chicken and rice", listOf("Chicken", "Rice")),
                SeedMeal("D", 7, "Sunday roast", listOf("Roasting joint", "Roast potatoes", "Carrots", "Yorkshire puddings", "Gravy"))
            )
            val gson = Gson()
            meals.forEach { meal ->
                db.execSQL(
                    "INSERT INTO meals (name, week, ingredients, dayOfWeek, mealSlot, servings) VALUES (?, ?, ?, ?, ?, ?)",
                    arrayOf<Any?>(
                        meal.name,
                        meal.week,
                        gson.toJson(meal.ingredients),
                        meal.day,
                        MealEntity.SLOT_DINNER,
                        MealEntity.DEFAULT_SERVINGS
                    )
                )
            }
        }

        private data class SeedMeal(
            val week: String,
            val day: Int,
            val name: String,
            val ingredients: List<String>
        )
    }
}
