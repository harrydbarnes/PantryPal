@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package com.example.pantrypal.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddShoppingCart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.domain.recipe.RecipeExternalSearchMode
import com.example.pantrypal.domain.recipe.RecipeIdeaShelves
import com.example.pantrypal.domain.recipe.RecipeIngredient
import com.example.pantrypal.domain.recipe.RecipeMatch
import com.example.pantrypal.domain.recipe.RecipeSearch
import java.util.Locale

data class RecipeScreenState(
    val savedRecipes: List<Recipe> = emptyList(),
    val ideaShelves: RecipeIdeaShelves = RecipeIdeaShelves(),
    val searchQuery: String = "",
    val externalResults: List<Recipe> = emptyList(),
    val isExternalSearchLoading: Boolean = false,
    val isImportLoading: Boolean = false,
    val errorMessage: String? = null,
    val message: String? = null,
    val selectedRecipe: Recipe? = null,
    val selectedMissingIngredients: List<RecipeIngredient> = emptyList(),
    val importPreview: Recipe? = null
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RecipeScreen(
    state: RecipeScreenState,
    onSearchQueryChange: (String) -> Unit,
    onExternalSearch: (String, RecipeExternalSearchMode) -> Unit,
    onImportUrl: (String) -> Unit,
    onRecipeSelected: (Recipe, List<RecipeIngredient>) -> Unit,
    onRecipeDismissed: () -> Unit,
    onImportPreviewDismissed: () -> Unit,
    onSaveRecipe: (Recipe) -> Unit,
    onToggleFavourite: (Recipe, Boolean) -> Unit,
    onRateRecipe: (Recipe, Int) -> Unit,
    onMarkCooked: (Recipe) -> Unit,
    onOpenSource: (String) -> Unit,
    onAddToPlan: (Recipe) -> Unit,
    onAddMissingToShopping: (Recipe, List<RecipeIngredient>) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var externalSearchMode by rememberSaveable {
        mutableStateOf(RecipeExternalSearchMode.NAME)
    }
    var showImportDialog by rememberSaveable { mutableStateOf(false) }
    val localResults by remember(state.savedRecipes, state.searchQuery) {
        derivedStateOf { RecipeSearch.local(state.savedRecipes, state.searchQuery) }
    }

    Scaffold(modifier = modifier) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 96.dp
            ),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                RecipeHero(onImportClick = { showImportDialog = true })
            }
            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("My recipes", "Ideas", "Find online").forEachIndexed { index, label ->
                        FilterChip(
                            selected = selectedSection == index,
                            onClick = { selectedSection = index },
                            label = { Text(label) },
                            leadingIcon = {
                                Icon(
                                    imageVector = when (index) {
                                        0 -> Icons.Default.MenuBook
                                        1 -> Icons.Default.AutoAwesome
                                        else -> Icons.Default.TravelExplore
                                    },
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        )
                    }
                }
            }

            state.errorMessage?.let { message ->
                item { RecipeErrorCard(message) }
            }
            state.message?.let { message ->
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        shape = MaterialTheme.shapes.large
                    ) {
                        Text(
                            message,
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }

            when (selectedSection) {
                0 -> {
                    item {
                        RecipeSearchField(
                            query = state.searchQuery,
                            onQueryChange = onSearchQueryChange,
                            label = "Search saved recipes"
                        )
                    }
                    if (localResults.isEmpty()) {
                        item {
                            RecipeEmptyCard(
                                title = if (state.savedRecipes.isEmpty()) {
                                    "Build your family recipe book"
                                } else {
                                    "No saved recipes match"
                                },
                                supportingText = if (state.savedRecipes.isEmpty()) {
                                    "Import a recipe link or turn meals from your rotation into reusable recipes."
                                } else {
                                    "Try a meal name, ingredient, tag, or source."
                                },
                                onImport = { showImportDialog = true }
                            )
                        }
                    } else {
                        items(localResults, key = { "saved-${it.id}-${it.title}" }) { recipe ->
                            RecipeLibraryRow(
                                recipe = recipe,
                                onClick = { onRecipeSelected(recipe, emptyList()) },
                                onToggleFavourite = {
                                    onToggleFavourite(recipe, !recipe.isFavourite)
                                }
                            )
                        }
                    }
                }

                1 -> {
                    if (state.ideaShelves.isEmpty()) {
                        item {
                            RecipeEmptyCard(
                                title = "Ideas get smarter with your cupboard",
                                supportingText = "Save recipes and add pantry items to see what you can cook now, what uses food soon, and what only needs one or two extras.",
                                onImport = { showImportDialog = true }
                            )
                        }
                    } else {
                        recipeShelf(
                            title = "Cook now",
                            supportingText = "You already have the essential ingredients.",
                            matches = state.ideaShelves.cookNow,
                            onRecipeSelected = onRecipeSelected
                        )
                        recipeShelf(
                            title = "Use these soon",
                            supportingText = "Ideas that make use of food approaching its date.",
                            matches = state.ideaShelves.useSoon,
                            onRecipeSelected = onRecipeSelected
                        )
                        recipeShelf(
                            title = "Only missing 1–2",
                            supportingText = "Nearly ready, with a short top-up shop.",
                            matches = state.ideaShelves.missingOneOrTwo,
                            onRecipeSelected = onRecipeSelected
                        )
                        recipeShelf(
                            title = "Forgotten favourites",
                            supportingText = "Meals you liked but have not cooked for a while.",
                            matches = state.ideaShelves.forgottenFavourites,
                            onRecipeSelected = onRecipeSelected
                        )
                    }
                }

                else -> {
                    item {
                        RecipeExternalSearch(
                            query = state.searchQuery,
                            mode = externalSearchMode,
                            isLoading = state.isExternalSearchLoading,
                            onQueryChange = onSearchQueryChange,
                            onModeChange = { externalSearchMode = it },
                            onSearch = {
                                if (state.searchQuery.isNotBlank()) {
                                    onExternalSearch(state.searchQuery, externalSearchMode)
                                }
                            }
                        )
                    }
                    if (state.externalResults.isEmpty() && !state.isExternalSearchLoading) {
                        item {
                            RecipeEmptyCard(
                                title = "Search TheMealDB",
                                supportingText = "Find a recipe by meal name or start with an ingredient you want to use.",
                                onImport = { showImportDialog = true }
                            )
                        }
                    } else {
                        items(
                            state.externalResults,
                            key = { "external-${it.externalId}-${it.title}" }
                        ) { recipe ->
                            RecipeExternalResultRow(
                                recipe = recipe,
                                onOpen = { onRecipeSelected(recipe, emptyList()) },
                                onSave = { onSaveRecipe(recipe) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showImportDialog) {
        RecipeUrlDialog(
            isLoading = state.isImportLoading,
            onDismiss = { showImportDialog = false },
            onImport = { url ->
                onImportUrl(url)
                showImportDialog = false
            }
        )
    }

    state.importPreview?.let { recipe ->
        RecipeImportReviewDialog(
            recipe = recipe,
            onDismiss = onImportPreviewDismissed,
            onSave = { onSaveRecipe(recipe) }
        )
    }

    state.selectedRecipe?.let { recipe ->
        RecipeDetailDialog(
            recipe = recipe,
            missingIngredients = state.selectedMissingIngredients,
            onDismiss = onRecipeDismissed,
            onToggleFavourite = {
                onToggleFavourite(recipe, !recipe.isFavourite)
            },
            onRate = { onRateRecipe(recipe, it) },
            onMarkCooked = { onMarkCooked(recipe) },
            onOpenSource = onOpenSource,
            onAddToPlan = { onAddToPlan(recipe) },
            onAddMissingToShopping = {
                onAddMissingToShopping(recipe, state.selectedMissingIngredients)
            },
            onSave = { onSaveRecipe(recipe) }
        )
    }
}

private fun RecipeIdeaShelves.isEmpty(): Boolean =
    cookNow.isEmpty() &&
        useSoon.isEmpty() &&
        missingOneOrTwo.isEmpty() &&
        forgottenFavourites.isEmpty()

@Composable
private fun RecipeHero(onImportClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        shape = MaterialTheme.shapes.extraLarge
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    "Recipes that start at home",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Text(
                    "Save family favourites, use food before it expires, and shop only for what is missing.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
            IconButton(onClick = onImportClick) {
                Icon(
                    Icons.Default.Link,
                    contentDescription = "Import recipe from a link",
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }
    }
}

@Composable
private fun RecipeSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    label: String
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(Icons.Default.Search, contentDescription = null)
        },
        singleLine = true,
        modifier = Modifier.fillMaxWidth()
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RecipeExternalSearch(
    query: String,
    mode: RecipeExternalSearchMode,
    isLoading: Boolean,
    onQueryChange: (String) -> Unit,
    onModeChange: (RecipeExternalSearchMode) -> Unit,
    onSearch: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Find a recipe online", style = MaterialTheme.typography.titleLarge)
            Text(
                "Results are provided by TheMealDB. Open a result to review its source before saving.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            RecipeSearchField(
                query = query,
                onQueryChange = onQueryChange,
                label = if (mode == RecipeExternalSearchMode.NAME) {
                    "Meal name"
                } else {
                    "Ingredient"
                }
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = mode == RecipeExternalSearchMode.NAME,
                    onClick = { onModeChange(RecipeExternalSearchMode.NAME) },
                    label = { Text("By name") }
                )
                FilterChip(
                    selected = mode == RecipeExternalSearchMode.INGREDIENT,
                    onClick = { onModeChange(RecipeExternalSearchMode.INGREDIENT) },
                    label = { Text("By ingredient") }
                )
                Button(
                    onClick = onSearch,
                    enabled = query.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Search, contentDescription = null)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text("Search")
                }
            }
        }
    }
}

@Composable
private fun RecipeLibraryRow(
    recipe: Recipe,
    onClick: () -> Unit,
    onToggleFavourite: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecipeThumbnail(recipe)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    recipeSummary(recipe),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                recipe.source?.let {
                    Text(
                        it.attribution,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            IconButton(onClick = onToggleFavourite) {
                Icon(
                    imageVector = if (recipe.isFavourite) {
                        Icons.Default.Favorite
                    } else {
                        Icons.Outlined.FavoriteBorder
                    },
                    contentDescription = if (recipe.isFavourite) {
                        "Remove ${recipe.title} from favourites"
                    } else {
                        "Add ${recipe.title} to favourites"
                    }
                )
            }
        }
    }
}

@Composable
private fun RecipeExternalResultRow(
    recipe: Recipe,
    onOpen: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RecipeThumbnail(recipe)
            Column(modifier = Modifier.weight(1f)) {
                Text(recipe.title, style = MaterialTheme.typography.titleMedium)
                Text(
                    recipe.source?.attribution ?: "Recipe data from TheMealDB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onOpen) { Text("Review") }
            IconButton(onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = "Save ${recipe.title}")
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.recipeShelf(
    title: String,
    supportingText: String,
    matches: List<RecipeMatch>,
    onRecipeSelected: (Recipe, List<RecipeIngredient>) -> Unit
) {
    if (matches.isEmpty()) return
    item(key = "heading-$title") {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    item(key = "shelf-$title") {
        LazyRow(
            contentPadding = PaddingValues(end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(matches, key = { "${title}-${it.recipe.id}-${it.recipe.title}" }) { match ->
                RecipeIdeaCard(
                    match = match,
                    onClick = {
                        onRecipeSelected(match.recipe, match.missingIngredients)
                    }
                )
            }
        }
    }
}

@Composable
private fun RecipeIdeaCard(
    match: RecipeMatch,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.width(224.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column {
            RecipeWideImage(match.recipe)
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    match.recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    when {
                        match.missingIngredients.isEmpty() -> "Ready with what you have"
                        else -> "Missing ${match.missingIngredients.size}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                if (match.expiringIngredients.isNotEmpty()) {
                    AssistChip(
                        onClick = onClick,
                        label = { Text("Uses ${match.expiringIngredients.first().name}") },
                        leadingIcon = {
                            Icon(
                                Icons.Default.Kitchen,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun RecipeThumbnail(recipe: Recipe) {
    if (recipe.imageUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.medium),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        AsyncImage(
            model = recipe.imageUrl,
            contentDescription = "Photo of ${recipe.title}",
            modifier = Modifier
                .size(72.dp)
                .clip(MaterialTheme.shapes.medium),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun RecipeWideImage(recipe: Recipe) {
    if (recipe.imageUrl.isNullOrBlank()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        AsyncImage(
            model = recipe.imageUrl,
            contentDescription = "Photo of ${recipe.title}",
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            contentScale = ContentScale.Crop
        )
    }
}

@Composable
private fun RecipeErrorCard(message: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Text(
            message,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun RecipeEmptyCard(
    title: String,
    supportingText: String,
    onImport: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        shape = MaterialTheme.shapes.large
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                Icons.Default.MenuBook,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(title, style = MaterialTheme.typography.titleLarge)
            Text(
                supportingText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            FilledTonalButton(onClick = onImport) {
                Icon(Icons.Default.Link, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Import a link")
            }
        }
    }
}

@Composable
private fun RecipeUrlDialog(
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onImport: (String) -> Unit
) {
    var url by rememberSaveable { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.Link, contentDescription = null) },
        title = { Text("Import a recipe link") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "Paste a public recipe page. You can review the ingredients, source, and instructions before saving."
                )
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("Recipe URL") },
                    placeholder = { Text("https://…") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(url.trim()) },
                enabled = Regex("""^https://""", RegexOption.IGNORE_CASE)
                    .containsMatchIn(url.trim()) && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                }
                Spacer(Modifier.width(8.dp))
                Text("Review")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RecipeImportReviewDialog(
    recipe: Recipe,
    onDismiss: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
        title = { Text("Review ${recipe.title}") },
        text = {
            RecipeDetailContent(
                recipe = recipe,
                missingIngredients = emptyList(),
                showInstructions = true
            )
        },
        confirmButton = {
            Button(onClick = onSave) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Save recipe")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun RecipeDetailDialog(
    recipe: Recipe,
    missingIngredients: List<RecipeIngredient>,
    onDismiss: () -> Unit,
    onToggleFavourite: () -> Unit,
    onRate: (Int) -> Unit,
    onMarkCooked: () -> Unit,
    onOpenSource: (String) -> Unit,
    onAddToPlan: () -> Unit,
    onAddMissingToShopping: () -> Unit,
    onSave: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(recipe.title)
                recipe.source?.let {
                    Text(
                        it.attribution,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RecipeDetailContent(
                    recipe = recipe,
                    missingIngredients = missingIngredients,
                    showInstructions = true
                )
                Text("Your rating", style = MaterialTheme.typography.labelLarge)
                Row {
                    (1..5).forEach { rating ->
                        IconButton(onClick = { onRate(rating) }) {
                            Icon(
                                imageVector = if ((recipe.rating ?: 0) >= rating) {
                                    Icons.Default.Star
                                } else {
                                    Icons.Outlined.StarBorder
                                },
                                contentDescription = "Rate ${recipe.title} $rating out of 5"
                            )
                        }
                    }
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = onToggleFavourite) {
                        Icon(
                            imageVector = if (recipe.isFavourite) {
                                Icons.Default.Favorite
                            } else {
                                Icons.Outlined.FavoriteBorder
                            },
                            contentDescription = null
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(if (recipe.isFavourite) "Favourite" else "Add favourite")
                    }
                    Button(onClick = onAddToPlan) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add to plan")
                    }
                    FilledTonalButton(onClick = onMarkCooked) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cooked today")
                    }
                    recipe.source?.url?.let { sourceUrl ->
                        OutlinedButton(onClick = { onOpenSource(sourceUrl) }) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Open source")
                        }
                    }
                    FilledTonalButton(
                        onClick = onAddMissingToShopping,
                        enabled = missingIngredients.isNotEmpty()
                    ) {
                        Icon(Icons.Default.AddShoppingCart, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (missingIngredients.isEmpty()) {
                                "Nothing missing"
                            } else {
                                "Add ${missingIngredients.size} to shop"
                            }
                        )
                    }
                    if (recipe.id == 0L) {
                        OutlinedButton(onClick = onSave) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Save")
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
private fun RecipeDetailContent(
    recipe: Recipe,
    missingIngredients: List<RecipeIngredient>,
    showInstructions: Boolean
) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 520.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            RecipeWideImage(recipe)
        }
        item {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                recipe.yieldText?.let { RecipeMetadataPill(it) }
                recipe.totalTimeMinutes?.let {
                    RecipeMetadataPill(formatMinutes(it))
                }
            }
        }
        item {
            Text("Ingredients", style = MaterialTheme.typography.titleMedium)
        }
        items(recipe.ingredients, key = { "${it.sortOrder}-${it.normalizedName}" }) { ingredient ->
            val missing = missingIngredients.any {
                it.normalizedName == ingredient.normalizedName
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = if (missing) {
                        Icons.Default.AddShoppingCart
                    } else {
                        Icons.Default.Kitchen
                    },
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (missing) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    ingredient.rawText.ifBlank { ingredient.name },
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        if (showInstructions && recipe.instructions.isNotEmpty()) {
            item { HorizontalDivider() }
            item {
                Text("Method", style = MaterialTheme.typography.titleMedium)
            }
            items(recipe.instructions) { instruction ->
                Text(
                    "${recipe.instructions.indexOf(instruction) + 1}. $instruction",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        recipe.source?.let { source ->
            item {
                HorizontalDivider()
                Text(
                    source.attribution,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RecipeMetadataPill(label: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun recipeSummary(recipe: Recipe): String {
    val parts = listOfNotNull(
        "${recipe.ingredients.size} ingredient${if (recipe.ingredients.size == 1) "" else "s"}",
        recipe.totalTimeMinutes?.let(::formatMinutes),
        recipe.rating?.let { "$it/5" }
    )
    return parts.joinToString(" · ")
}

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} hr"
    else -> String.format(Locale.getDefault(), "%d hr %d min", minutes / 60, minutes % 60)
}
