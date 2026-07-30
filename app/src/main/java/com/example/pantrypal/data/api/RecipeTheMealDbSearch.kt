package com.example.pantrypal.data.api

import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.domain.recipe.RecipeExternalSearchMode
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class RecipeTheMealDbSearch(
    private val api: RecipeTheMealDbApi
) {
    suspend fun search(
        query: String,
        mode: RecipeExternalSearchMode
    ): Result<List<Recipe>> = runCatching {
        val cleanQuery = query.trim()
        require(cleanQuery.isNotEmpty()) { "Enter a meal or ingredient to search for." }

        when (mode) {
            RecipeExternalSearchMode.NAME -> api.searchByName(cleanQuery)
                .meals
                .orEmpty()
            RecipeExternalSearchMode.INGREDIENT -> api.searchByIngredient(cleanQuery)
                .meals
                .orEmpty()
                .map { summary ->
                    api.getById(summary.idMeal).meals?.firstOrNull() ?: summary
                }
        }.map(RecipeTheMealDbMapper::toDomain)
    }

    companion object {
        fun create(): RecipeTheMealDbSearch {
            val api = Retrofit.Builder()
                .baseUrl(RecipeTheMealDbApi.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(RecipeTheMealDbApi::class.java)
            return RecipeTheMealDbSearch(api)
        }
    }
}
