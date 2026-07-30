package com.example.pantrypal.util

import com.example.pantrypal.domain.recipe.Recipe
import com.example.pantrypal.domain.recipe.RecipeIngredientNormalizer
import com.example.pantrypal.domain.recipe.RecipeSource
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import java.net.URI
import java.time.Duration

class RecipeJsonLdImporter(
    private val gson: Gson = Gson()
) {
    suspend fun importFromUrl(
        pageUrl: String,
        fetchHtml: suspend (String) -> String,
        now: Long = System.currentTimeMillis()
    ): Result<Recipe> = runCatching {
        val html = fetchHtml(pageUrl)
        importFromHtml(html, pageUrl, now).getOrThrow()
    }

    fun importFromHtml(
        html: String,
        pageUrl: String,
        now: Long = System.currentTimeMillis()
    ): Result<Recipe> = runCatching {
        val scriptBodies = jsonLdScriptPattern
            .findAll(html)
            .map { match -> decodeHtmlEntities(match.groupValues[2]).trim() }
            .filter(String::isNotBlank)
            .toList()

        require(scriptBodies.isNotEmpty()) {
            "This page does not contain schema.org recipe data."
        }

        scriptBodies.asSequence()
            .mapNotNull { body ->
                runCatching { gson.fromJson(body, JsonElement::class.java) }.getOrNull()
            }
            .mapNotNull(::findRecipeNode)
            .map { node -> mapRecipe(node, pageUrl, now) }
            .firstOrNull()
            ?: error("No schema.org Recipe was found on this page.")
    }

    fun importFromJsonLd(
        jsonLd: String,
        pageUrl: String,
        now: Long = System.currentTimeMillis()
    ): Result<Recipe> = runCatching {
        val root = gson.fromJson(decodeHtmlEntities(jsonLd), JsonElement::class.java)
        val node = findRecipeNode(root)
            ?: error("No schema.org Recipe was found in this data.")
        mapRecipe(node, pageUrl, now)
    }

    private fun mapRecipe(node: JsonObject, pageUrl: String, now: Long): Recipe {
        val title = node.string("name")?.trim().orEmpty()
        require(title.isNotBlank()) { "The recipe does not have a name." }

        val rawIngredients = node.arrayOrSingle("recipeIngredient")
            .mapNotNull { it.asText() }
            .map(String::trim)
            .filter(String::isNotBlank)
        require(rawIngredients.isNotEmpty()) {
            "The recipe does not list any ingredients."
        }

        val recipeUrl = pageUrl.takeIf(String::isNotBlank)
            ?: node.url("url")
            ?: node.url("mainEntityOfPage")
        val hostName = recipeUrl
            ?.let { runCatching { URI(it).host }.getOrNull() }
            ?.removePrefix("www.")
            ?.takeIf(String::isNotBlank)
        val author = node.personOrOrganizationName("author")
        val publisher = node.personOrOrganizationName("publisher")
        val sourceName = publisher ?: hostName ?: author ?: "Imported recipe"
        val attribution = when {
            author != null && hostName != null -> "Recipe by $author via $hostName"
            author != null -> "Recipe by $author"
            hostName != null -> "Recipe from $hostName"
            else -> "Imported recipe"
        }
        val yieldText = node.string("recipeYield")
            ?: node.arrayOrSingle("recipeYield").firstOrNull()?.asText()

        return Recipe(
            title = title,
            ingredients = rawIngredients.mapIndexed { index, raw ->
                RecipeIngredientNormalizer.parse(raw, index)
            },
            instructions = extractInstructions(node.get("recipeInstructions")),
            source = RecipeSource(
                name = sourceName,
                url = recipeUrl,
                attribution = attribution
            ),
            imageUrl = extractImage(node.get("image")),
            yieldText = yieldText,
            servings = yieldText?.let(::extractFirstNumber),
            prepTimeMinutes = node.string("prepTime")?.let(::durationMinutes),
            cookTimeMinutes = node.string("cookTime")?.let(::durationMinutes),
            totalTimeMinutes = node.string("totalTime")?.let(::durationMinutes),
            tags = listOf("recipeCategory", "recipeCuisine", "keywords")
                .flatMap { property -> node.arrayOrSingle(property) }
                .flatMap { value -> value.asText().orEmpty().split(',') }
                .map(String::trim)
                .filter(String::isNotBlank)
                .distinct(),
            createdAt = now,
            updatedAt = now
        )
    }

    private fun findRecipeNode(element: JsonElement?): JsonObject? {
        if (element == null || element.isJsonNull) return null
        if (element.isJsonArray) {
            return element.asJsonArray.asSequence()
                .mapNotNull(::findRecipeNode)
                .firstOrNull()
        }
        if (!element.isJsonObject) return null

        val objectValue = element.asJsonObject
        if (objectValue.arrayOrSingle("@type").any { it.asText() == "Recipe" }) {
            return objectValue
        }
        objectValue.get("@graph")?.let(::findRecipeNode)?.let { return it }
        objectValue.get("mainEntity")?.let(::findRecipeNode)?.let { return it }

        return objectValue.entrySet().asSequence()
            .filterNot { (key, _) -> key == "@context" }
            .mapNotNull { (_, value) -> findRecipeNode(value) }
            .firstOrNull()
    }

    private fun extractInstructions(element: JsonElement?): List<String> {
        if (element == null || element.isJsonNull) return emptyList()
        if (element.isJsonPrimitive) {
            return element.asString
                .split(Regex("""(?:\r?\n)+"""))
                .map(String::trim)
                .filter(String::isNotBlank)
        }
        if (element.isJsonArray) {
            return element.asJsonArray.flatMap(::extractInstructions)
        }

        val instruction = element.asJsonObject
        val nested = instruction.get("itemListElement")
        if (nested != null) return extractInstructions(nested)
        return listOfNotNull(instruction.string("text")?.trim()?.takeIf(String::isNotBlank))
    }

    private fun extractImage(element: JsonElement?): String? {
        if (element == null || element.isJsonNull) return null
        if (element.isJsonPrimitive) return element.asString.takeIf(String::isNotBlank)
        if (element.isJsonArray) {
            return element.asJsonArray.asSequence()
                .mapNotNull(::extractImage)
                .firstOrNull()
        }
        return element.asJsonObject.url("url")
            ?: element.asJsonObject.url("contentUrl")
    }

    private fun JsonObject.personOrOrganizationName(property: String): String? {
        val value = get(property) ?: return null
        if (value.isJsonPrimitive) return value.asString.trim().takeIf(String::isNotBlank)
        if (value.isJsonArray) {
            return value.asJsonArray.asSequence()
                .mapNotNull { entry ->
                    if (entry.isJsonObject) entry.asJsonObject.string("name") else entry.asText()
                }
                .firstOrNull(String::isNotBlank)
        }
        return value.asJsonObject.string("name")?.trim()?.takeIf(String::isNotBlank)
    }

    private fun JsonObject.string(property: String): String? {
        val value = get(property) ?: return null
        return if (value.isJsonPrimitive) {
            runCatching { value.asString }.getOrNull()
        } else {
            null
        }
    }

    private fun JsonObject.url(property: String): String? {
        val value = get(property) ?: return null
        return when {
            value.isJsonPrimitive -> value.asString.takeIf(String::isNotBlank)
            value.isJsonObject -> value.asJsonObject.string("@id")
                ?: value.asJsonObject.string("url")
            else -> null
        }
    }

    private fun JsonObject.arrayOrSingle(property: String): List<JsonElement> {
        val value = get(property) ?: return emptyList()
        return if (value.isJsonArray) value.asJsonArray.toList() else listOf(value)
    }

    private fun JsonElement.asText(): String? = when {
        isJsonPrimitive -> runCatching { asString }.getOrNull()
        isJsonObject -> asJsonObject.string("name")
            ?: asJsonObject.string("text")
            ?: asJsonObject.string("@id")
        else -> null
    }

    private fun durationMinutes(value: String): Int? = runCatching {
        Duration.parse(value.trim().uppercase()).toMinutes().toInt()
    }.getOrNull()

    private fun extractFirstNumber(value: String): Double? = Regex("""\d+(?:[.,]\d+)?""")
        .find(value)
        ?.value
        ?.replace(',', '.')
        ?.toDoubleOrNull()

    private companion object {
        val jsonLdScriptPattern = Regex(
            """<script\b(?=[^>]*\btype\s*=\s*(["'])application/ld\+json\1)[^>]*>([\s\S]*?)</script\s*>""",
            RegexOption.IGNORE_CASE
        )

        fun decodeHtmlEntities(value: String): String = value
            .removePrefix("<!--")
            .removeSuffix("-->")
            .replace("&quot;", "\"")
            .replace("&#34;", "\"")
            .replace("&#39;", "'")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
    }
}
