package com.example.pantrypal.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.pantrypal.data.backup.BackupDecodeResult
import com.example.pantrypal.data.mapper.toPriceObservation
import com.example.pantrypal.data.repository.PantryFeaturesRepository
import com.example.pantrypal.data.household.FirebaseHouseholdSync
import android.app.Activity
import com.example.pantrypal.domain.budget.BudgetCalculator
import com.example.pantrypal.domain.budget.BudgetTarget
import com.example.pantrypal.domain.price.PriceCalculator
import com.example.pantrypal.domain.receipt.ReceiptParser
import com.example.pantrypal.domain.receipt.ReceiptReviewCandidate
import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.domain.recipe.RecipeExternalSearchMode
import com.example.pantrypal.domain.recipe.RecipeIngredient
import com.example.pantrypal.domain.recipe.RecipePantryIngredient
import com.example.pantrypal.domain.recipe.RecipeRanker
import com.example.pantrypal.ui.screens.DataManagementUiState
import com.example.pantrypal.ui.screens.HouseholdSyncUiState
import com.example.pantrypal.ui.screens.RecipeScreenState
import com.example.pantrypal.ui.screens.ShoppingToolsUiState
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PantryFeaturesViewModel(
    private val repository: PantryFeaturesRepository,
    private val firebaseHouseholdSync: FirebaseHouseholdSync
) : ViewModel() {
    private val _recipeState = MutableStateFlow(RecipeScreenState())
    val recipeState: StateFlow<RecipeScreenState> = _recipeState.asStateFlow()
    private var pantryForRecipes: List<RecipePantryIngredient> = emptyList()

    private val _receiptResult =
        MutableStateFlow<com.example.pantrypal.domain.receipt.ReceiptParseResult?>(null)
    val receiptResult = _receiptResult.asStateFlow()
    private val _receiptProcessing = MutableStateFlow(false)
    val receiptProcessing = _receiptProcessing.asStateFlow()
    private val _receiptError = MutableStateFlow<String?>(null)
    val receiptError = _receiptError.asStateFlow()

    private val _dataState = MutableStateFlow(DataManagementUiState())
    val dataState = _dataState.asStateFlow()

    private val _shoppingMessage = MutableStateFlow<String?>(null)
    private val _householdState = MutableStateFlow(HouseholdSyncUiState())
    val householdState = _householdState.asStateFlow()

    val shoppingToolsState: StateFlow<ShoppingToolsUiState> = combine(
        repository.prices,
        repository.budgets,
        _shoppingMessage
    ) { prices, budgets, message ->
        val weekStart = BudgetCalculator.mondayFor(LocalDate.now())
        val budget = budgets.firstOrNull { it.weekStartEpochDay == weekStart.toEpochDay() }
        val summary = BudgetCalculator.summarize(
            weekStart = weekStart,
            budget = budget?.let {
                BudgetTarget(it.weekStartEpochDay, it.budgetMinor, it.currencyCode)
            },
            purchases = prices.map {
                com.example.pantrypal.domain.budget.BudgetPurchase(
                    purchasedAt = it.purchasedAt,
                    priceMinor = it.priceMinor,
                    currencyCode = it.currencyCode,
                    retailer = it.retailer
                )
            }
        )
        ShoppingToolsUiState(
            summary = summary,
            priceSummaries = PriceCalculator.summarize(
                observations = prices.map { it.toPriceObservation() },
                currencyCode = budget?.currencyCode ?: "GBP"
            ).take(12),
            recentPrices = prices.take(20),
            message = message
        )
    }.flowOn(Dispatchers.Default).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ShoppingToolsUiState()
    )

    init {
        viewModelScope.launch {
            firebaseHouseholdSync.state.collect { live ->
                _householdState.update {
                    it.copy(
                        signedIn = live.signedIn,
                        accountName = live.accountName,
                        liveHouseholdId = live.householdId,
                        liveInvite = live.invite,
                        liveSyncing = live.syncing,
                        livePending = live.pending,
                        liveWorking = live.working,
                        liveFailed = live.failed,
                        lastSyncedAt = live.lastSyncedAt,
                        message = live.status
                    )
                }
            }
        }
        viewModelScope.launch {
            combine(repository.recipes, repository.pantryForRecipes) { recipes, pantry ->
                Triple(recipes, pantry, RecipeRanker.buildShelves(recipes, pantry))
            }.flowOn(Dispatchers.Default).collect { (recipes, pantry, shelves) ->
                pantryForRecipes = pantry
                _recipeState.update { it.copy(savedRecipes = recipes, ideaShelves = shelves) }
            }
        }
        viewModelScope.launch {
            runCatching { repository.bootstrapRecipesFromMeals() }
                .onFailure { showRecipeError(it) }
        }
    }

    fun signInToHousehold(activity: Activity) {
        viewModelScope.launch { firebaseHouseholdSync.signIn(activity) }
    }

    fun retryHousehold() = firebaseHouseholdSync.retry()
    fun disconnectHousehold() = firebaseHouseholdSync.leaveHousehold()
    fun signOutHousehold() = firebaseHouseholdSync.signOut()

    fun createLiveHousehold() = firebaseHouseholdSync.createHousehold()

    fun joinLiveHousehold(invite: String) = firebaseHouseholdSync.joinHousehold(invite)

    fun setRecipeQuery(query: String) {
        _recipeState.update {
            it.copy(searchQuery = query, errorMessage = null, message = null)
        }
    }

    fun searchRecipesOnline(query: String, mode: RecipeExternalSearchMode) {
        _recipeState.update {
            it.copy(
                isExternalSearchLoading = true,
                errorMessage = null,
                message = null
            )
        }
        viewModelScope.launch {
            runCatching { repository.searchOnline(query, mode) }
                .onSuccess { recipes ->
                    _recipeState.update {
                        it.copy(
                            externalResults = recipes,
                            isExternalSearchLoading = false,
                            message = "${recipes.size} online result${if (recipes.size == 1) "" else "s"}."
                        )
                    }
                }
                .onFailure { error ->
                    _recipeState.update {
                        it.copy(
                            isExternalSearchLoading = false,
                            errorMessage = error.userMessage("Online recipe search failed")
                        )
                    }
                }
        }
    }

    fun importRecipeUrl(url: String) {
        _recipeState.update {
            it.copy(isImportLoading = true, errorMessage = null, message = null)
        }
        viewModelScope.launch {
            runCatching { repository.importRecipeUrl(url) }
                .onSuccess { recipe ->
                    _recipeState.update {
                        it.copy(
                            importPreview = recipe,
                            isImportLoading = false,
                            message = null
                        )
                    }
                }
                .onFailure { error ->
                    _recipeState.update {
                        it.copy(
                            isImportLoading = false,
                            errorMessage = error.userMessage("Recipe import failed")
                        )
                    }
                }
        }
    }

    fun selectRecipe(recipe: Recipe, missing: List<RecipeIngredient>) {
        val calculatedMissing = if (missing.isEmpty()) {
            RecipeRanker.match(recipe, pantryForRecipes).missingIngredients
        } else {
            missing
        }
        _recipeState.update {
            it.copy(
                selectedRecipe = recipe,
                selectedMissingIngredients = calculatedMissing
            )
        }
    }

    fun dismissRecipe() {
        _recipeState.update {
            it.copy(selectedRecipe = null, selectedMissingIngredients = emptyList())
        }
    }

    fun dismissImportPreview() {
        _recipeState.update { it.copy(importPreview = null) }
    }

    fun saveRecipe(recipe: Recipe) {
        viewModelScope.launch {
            runCatching { repository.saveRecipe(recipe) }
                .onSuccess {
                    _recipeState.update { state ->
                        state.copy(
                            importPreview = null,
                            errorMessage = null,
                            message = "${recipe.title} saved."
                        )
                    }
                }
                .onFailure(::showRecipeError)
        }
    }

    fun setFavourite(recipe: Recipe, favourite: Boolean) {
        saveRecipe(recipe.copy(isFavourite = favourite))
        _recipeState.update { state ->
            state.copy(
                selectedRecipe = state.selectedRecipe
                    ?.takeIf { it.id == recipe.id }
                    ?.copy(isFavourite = favourite)
                    ?: state.selectedRecipe
            )
        }
    }

    fun rateRecipe(recipe: Recipe, rating: Int) {
        saveRecipe(recipe.copy(rating = rating.coerceIn(1, 5)))
        _recipeState.update { state ->
            state.copy(
                selectedRecipe = state.selectedRecipe
                    ?.takeIf { it.id == recipe.id }
                    ?.copy(rating = rating.coerceIn(1, 5))
                    ?: state.selectedRecipe
            )
        }
    }

    private val _cookingReview = MutableStateFlow<com.example.pantrypal.util.CookingReview?>(null)
    val cookingReview = _cookingReview.asStateFlow()
    private val _cookingWorking = MutableStateFlow(false)
    val cookingWorking = _cookingWorking.asStateFlow()
    private val _cookingError = MutableStateFlow<String?>(null)
    val cookingError = _cookingError.asStateFlow()

    private fun prepareCooking(block: suspend () -> com.example.pantrypal.util.CookingReview) {
        if (_cookingWorking.value) return
        _cookingWorking.value = true
        _cookingError.value = null
        viewModelScope.launch {
            try { _cookingReview.value = block() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _recipeState.update { it.copy(errorMessage = error.userMessage("Cooking review could not be opened")) } }
            finally { _cookingWorking.value = false }
        }
    }
    fun prepareRecipeCooking(recipe: Recipe, week: String) = prepareCooking { repository.prepareCooking(recipe, week) }
    fun prepareMealCooking(meal: com.example.pantrypal.data.entity.MealEntity) = prepareCooking { repository.prepareMealCooking(meal) }
    fun dismissCooking() { if (!_cookingWorking.value) _cookingReview.value = null }
    fun confirmCooking(uses: List<Pair<com.example.pantrypal.data.dao.InventoryWithItemMap, Double>>, logOnly: Boolean) {
        val review = _cookingReview.value ?: return
        if (_cookingWorking.value) return
        _cookingWorking.value = true
        _cookingError.value = null
        viewModelScope.launch {
            try {
                val result = repository.recordCooking(review, uses, logOnly)
                _recipeState.update { it.copy(selectedRecipe = it.selectedRecipe?.let { selected -> if (selected.id == review.recipe.id && (selected.id > 0 || selected.title == review.recipe.title)) result.recipe ?: selected else selected },
                    message = if (logOnly) "${review.recipe.title} recorded as cooked. Pantry stock unchanged." else "${review.recipe.title} cooked; confirmed pantry usage saved.") }
                _cookingReview.value = null
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { _cookingError.value = error.userMessage("Cooking could not be saved") }
            finally { _cookingWorking.value = false }
        }
    }

    fun addRecipeToPlan(recipe: Recipe, weekId: String) {
        viewModelScope.launch {
            runCatching { repository.addRecipeToPlan(recipe, weekId) }
                .onSuccess {
                    _recipeState.update { state ->
                        state.copy(
                            errorMessage = null,
                            message = "${recipe.title} added to Week $weekId."
                        )
                    }
                }
                .onFailure(::showRecipeError)
        }
    }

    fun addMissingToShopping(
        recipe: Recipe,
        ingredients: List<RecipeIngredient>,
        weekId: String
    ) {
        viewModelScope.launch {
            runCatching {
                repository.addMissingIngredientsToShopping(recipe.ingredients, weekId)
            }.onSuccess { count ->
                _recipeState.update { state ->
                    state.copy(
                        errorMessage = null,
                        message = if (count == 0) {
                            "This recipe is covered individually. For several meals, review the whole week in Plan."
                        } else {
                            "$count item${if (count == 1) "" else "s"} added to Week $weekId."
                        }
                    )
                }
            }.onFailure(::showRecipeError)
        }
    }

    fun parseReceiptText(text: String) {
        _receiptProcessing.value = true
        _receiptError.value = null
        viewModelScope.launch {
            try {
                val parsed = withContext(Dispatchers.Default) {
                    ReceiptParser.parse(text)
                }
                val options = repository.receiptPurchaseOptions()
                _receiptPurchaseOptions.value = options
                val suggestions = parsed.candidates.map { candidate ->
                    options.filter { option ->
                        com.example.pantrypal.util.normalizeShoppingName(option.name) == com.example.pantrypal.util.normalizeShoppingName(candidate.name) &&
                        com.example.pantrypal.util.convertShoppingQuantity(candidate.quantity, candidate.unit, option.unit)?.let {
                            kotlin.math.abs(it - option.quantity) < 0.000001
                        } == true
                    }.singleOrNull()?.key
                }
                _receiptResult.value = parsed.copy(candidates = parsed.candidates.mapIndexed { index, candidate ->
                    candidate.copy(purchaseLink = suggestions[index]?.takeIf { link -> suggestions.count { it == link } == 1 })
                })
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                _receiptResult.value = null
                _receiptError.value = error.userMessage("Receipt could not be read")
            } finally {
                _receiptProcessing.value = false
            }
        }
    }

    fun setReceiptProcessing(processing: Boolean) {
        _receiptProcessing.value = processing
        if (processing) {
            _receiptResult.value = null
            _receiptError.value = null
        }
    }

    private val _receiptPurchaseOptions = MutableStateFlow<List<com.example.pantrypal.domain.receipt.ReceiptPurchaseOption>>(emptyList())
    val receiptPurchaseOptions = _receiptPurchaseOptions.asStateFlow()

    fun updateReceiptCandidate(candidate: ReceiptReviewCandidate) {
        _receiptResult.update { result ->
            result?.copy(
                candidates = result.candidates.map {
                    if (it.candidateId == candidate.candidateId) candidate else it
                }
            )?.let { updated ->
                updated.copy(candidateTotalMinor = updated.candidates.sumOf { it.totalPriceMinor })
            }
        }
    }

    fun useAsDifferentReceipt() {
        _receiptResult.update { result -> result?.copy(receiptId = java.util.UUID.randomUUID().toString(),
            candidates = result.candidates.map { it.copy(purchaseLink = null) }) }
        _receiptError.value = null
    }

    fun importReceipt(candidates: List<ReceiptReviewCandidate>, onSuccess: () -> Unit = {}) {
        val receiptId = _receiptResult.value?.receiptId ?: return
        _receiptProcessing.value = true
        _receiptError.value = null
        viewModelScope.launch {
            runCatching { repository.importReceiptPurchases(candidates, receiptId = receiptId) }
                .onSuccess { count ->
                    _receiptProcessing.value = false
                    _receiptResult.value = null
                    _shoppingMessage.value =
                        "$count receipt purchase${if (count == 1) "" else "s"} saved. Linked pantry stock was counted once."
                    onSuccess()
                }
                .onFailure { error ->
                    _receiptProcessing.value = false
                    _receiptError.value = error.userMessage("Receipt import failed")
                }
        }
    }

    fun setReceiptError(error: Throwable) {
        _receiptProcessing.value = false
        _receiptResult.value = null
        _receiptError.value = error.userMessage("Receipt could not be read")
    }

    fun setReceiptError(message: String) {
        _receiptProcessing.value = false
        _receiptResult.value = null
        _receiptError.value = message
    }

    fun setWeeklyBudget(amountMinor: Long) {
        viewModelScope.launch {
            runCatching { repository.setWeeklyBudget(amountMinor) }
                .onSuccess { _shoppingMessage.value = "Weekly budget updated." }
                .onFailure {
                    _shoppingMessage.value = it.userMessage("Budget could not be saved")
                }
        }
    }

    suspend fun createBackupJson(): Result<String> {
        _dataState.update { it.copy(isWorking = true, message = null) }
        return runCatching { repository.exportBackupJson() }

            .onFailure {
                _dataState.value =
                    DataManagementUiState(message = it.userMessage("Backup failed"))
            }
    }

    fun backupFileWritten(error: Throwable? = null) {
        _dataState.value = DataManagementUiState(message = if (error == null) "Complete backup saved."
            else error.userMessage("Backup file could not be written"))
    }

    fun restoreBackupJson(json: String, onRestored: () -> Unit = {}) {
        _dataState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            runCatching { repository.restoreBackupJson(json) }
                .onSuccess { result ->
                    _dataState.value = when (result) {
                        is BackupDecodeResult.Success -> {
                            onRestored()
                            _cookingReview.value = null
                            DataManagementUiState(message = "Backup restored. Preferences and reminder schedules updated.")
                        }

                        is BackupDecodeResult.Failure -> DataManagementUiState(
                            message = result.errors.joinToString("\n")
                        )
                    }
                }
                .onFailure {
                    _dataState.value =
                        DataManagementUiState(message = it.userMessage("Restore failed"))
                }
        }
    }

    suspend fun createHouseholdJson(): Result<String> {
        _householdState.update { it.copy(isWorking = true, message = null) }
        return runCatching { repository.exportHouseholdSnapshot() }
            .onSuccess {
                _householdState.update {
                    it.copy(
                        isWorking = false,
                        lastSharedAtEpochMs = System.currentTimeMillis(),
                        message = "Household snapshot is ready to share."
                    )
                }
            }
            .onFailure { error ->
                _householdState.update {
                    it.copy(
                        isWorking = false,
                        message = error.userMessage("Household snapshot failed")
                    )
                }
            }
    }

    fun restoreHouseholdJson(json: String, onRestored: () -> Unit = {}) {
        _householdState.update { it.copy(isWorking = true, message = null) }
        viewModelScope.launch {
            repository.importHouseholdSnapshot(json)
                .onSuccess { warningCount ->
                    onRestored()
                    _cookingReview.value = null
                    _householdState.update {
                        it.copy(
                            isWorking = false,
                            lastImportedAtEpochMs = System.currentTimeMillis(),
                            message = buildString {
                                append("Household snapshot imported.")
                                if (warningCount > 0) append(" $warningCount note(s) were reviewed.")
                            }
                        )
                    }
                }
                .onFailure { error ->
                    _householdState.update {
                        it.copy(
                            isWorking = false,
                            message = error.userMessage("Household import failed")
                        )
                    }
                }
        }
    }

    private fun showRecipeError(error: Throwable) {
        _recipeState.update {
            it.copy(
                errorMessage = error.userMessage("Recipe action failed"),
                message = null
            )
        }
    }

    private fun Throwable.userMessage(fallback: String): String =
        message?.takeIf(String::isNotBlank) ?: fallback
}

class PantryFeaturesViewModelFactory(
    private val repository: PantryFeaturesRepository,
    private val firebaseHouseholdSync: FirebaseHouseholdSync
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PantryFeaturesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PantryFeaturesViewModel(repository, firebaseHouseholdSync) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
