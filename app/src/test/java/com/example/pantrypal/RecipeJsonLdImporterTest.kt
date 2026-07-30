package com.example.pantrypal

import com.example.pantrypal.util.RecipeJsonLdImporter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeJsonLdImporterTest {
    private val importer = RecipeJsonLdImporter()

    @Test
    fun `imports recipe from graph and preserves source attribution`() {
        val html = """
            <html>
              <head>
                <script type="application/ld+json">
                {
                  "@context": "https://schema.org",
                  "@graph": [
                    {"@type": "WebSite", "name": "Family Food"},
                    {
                      "@type": ["Recipe", "CreativeWork"],
                      "name": "Tomato Pasta",
                      "author": {"@type": "Person", "name": "Alex Cook"},
                      "publisher": {"@type": "Organization", "name": "Family Food"},
                      "image": {"url": "https://cdn.example/pasta.jpg"},
                      "recipeYield": "4 servings",
                      "prepTime": "PT15M",
                      "cookTime": "PT30M",
                      "totalTime": "PT45M",
                      "recipeCategory": ["Dinner", "Pasta"],
                      "recipeIngredient": [
                        "250 g spaghetti",
                        "2 cans chopped tomatoes",
                        "1 tbsp olive oil (optional)"
                      ],
                      "recipeInstructions": [
                        {"@type": "HowToStep", "text": "Boil the pasta."},
                        {
                          "@type": "HowToSection",
                          "name": "Sauce",
                          "itemListElement": [
                            {"@type": "HowToStep", "text": "Simmer the tomatoes."}
                          ]
                        }
                      ]
                    }
                  ]
                }
                </script>
              </head>
            </html>
        """.trimIndent()

        val result = importer.importFromHtml(
            html = html,
            pageUrl = "https://familyfood.example/recipes/tomato-pasta",
            now = 123L
        ).getOrThrow()

        assertEquals("Tomato Pasta", result.title)
        assertEquals(4.0, result.servings!!, 0.001)
        assertEquals(15, result.prepTimeMinutes)
        assertEquals(30, result.cookTimeMinutes)
        assertEquals(45, result.totalTimeMinutes)
        assertEquals("https://cdn.example/pasta.jpg", result.imageUrl)
        assertEquals("Family Food", result.source?.name)
        assertEquals(
            "https://familyfood.example/recipes/tomato-pasta",
            result.source?.url
        )
        assertEquals("Recipe by Alex Cook via familyfood.example", result.source?.attribution)
        assertEquals(listOf("Boil the pasta.", "Simmer the tomatoes."), result.instructions)
        assertEquals(listOf("Dinner", "Pasta"), result.tags)
        assertEquals("spaghetti", result.ingredients[0].normalizedName)
        assertEquals(250.0, result.ingredients[0].quantity!!, 0.001)
        assertEquals("g", result.ingredients[0].unit)
        assertTrue(result.ingredients[2].isOptional)
    }

    @Test
    fun `returns failure when page has no recipe data`() {
        val result = importer.importFromHtml(
            "<html><body>No structured recipe</body></html>",
            "https://example.com/no-recipe"
        )

        assertTrue(result.isFailure)
    }
}
