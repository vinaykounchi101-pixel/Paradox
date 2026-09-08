package com.paradox.finance.core

data class CategorySuggestion(
    val categoryName: String,
    val color: String,
    val matchedWord: String
)

object PredictiveDictionary {

    private val KEYWORD_MAP = mapOf(
        // Alcohol
        "brandy" to ("Alcohol" to "#EF4444"),
        "whiskey" to ("Alcohol" to "#EF4444"),
        "beer" to ("Alcohol" to "#EF4444"),
        "vodka" to ("Alcohol" to "#EF4444"),
        "rum" to ("Alcohol" to "#EF4444"),
        "wine" to ("Alcohol" to "#EF4444"),
        "old monk" to ("Alcohol" to "#EF4444"),
        "kingfisher" to ("Alcohol" to "#EF4444"),
        "bira" to ("Alcohol" to "#EF4444"),
        "budweiser" to ("Alcohol" to "#EF4444"),
        "theka" to ("Alcohol" to "#EF4444"),
        "daaru" to ("Alcohol" to "#EF4444"),

        // Gaming
        "ps5" to ("Gaming" to "#8B5CF6"),
        "ps4" to ("Gaming" to "#8B5CF6"),
        "playstation" to ("Gaming" to "#8B5CF6"),
        "xbox" to ("Gaming" to "#8B5CF6"),
        "steam" to ("Gaming" to "#8B5CF6"),
        "gta" to ("Gaming" to "#8B5CF6"),
        "fifa" to ("Gaming" to "#8B5CF6"),
        "valorant" to ("Gaming" to "#8B5CF6"),
        "bgmi" to ("Gaming" to "#8B5CF6"),
        "nintendo" to ("Gaming" to "#8B5CF6"),

        // Shopping & Tech
        "nike" to ("Shopping" to "#EC4899"),
        "adidas" to ("Shopping" to "#EC4899"),
        "zara" to ("Shopping" to "#EC4899"),
        "h&m" to ("Shopping" to "#EC4899"),
        "apple" to ("Shopping" to "#EC4899"),
        "iphone" to ("Shopping" to "#EC4899"),
        "macbook" to ("Shopping" to "#EC4899"),
        "airpods" to ("Shopping" to "#EC4899"),
        "amazon" to ("Shopping" to "#EC4899"),
        "flipkart" to ("Shopping" to "#EC4899"),

        // Groceries & FMCG
        "blinkit" to ("Groceries" to "#10B981"),
        "zepto" to ("Groceries" to "#10B981"),
        "instamart" to ("Groceries" to "#10B981"),
        "amul" to ("Groceries" to "#10B981"),
        "milk" to ("Groceries" to "#10B981"),
        "maggi" to ("Groceries" to "#10B981"),
        "bread" to ("Groceries" to "#10B981"),
        "eggs" to ("Groceries" to "#10B981"),
        "vegetables" to ("Groceries" to "#10B981"),

        // Food & Dining
        "zomato" to ("Food & Dining" to "#F59E0B"),
        "swiggy" to ("Food & Dining" to "#F59E0B"),
        "mcdonalds" to ("Food & Dining" to "#F59E0B"),
        "burger" to ("Food & Dining" to "#F59E0B"),
        "pizza" to ("Food & Dining" to "#F59E0B"),
        "chai" to ("Food & Dining" to "#F59E0B"),
        "coffee" to ("Food & Dining" to "#F59E0B"),
        "starbucks" to ("Food & Dining" to "#F59E0B"),

        // Healthcare
        "dolo" to ("Healthcare" to "#06B6D4"),
        "paracetamol" to ("Healthcare" to "#06B6D4"),
        "apollo" to ("Healthcare" to "#06B6D4"),
        "pharmacy" to ("Healthcare" to "#06B6D4"),
        "pharmeasy" to ("Healthcare" to "#06B6D4"),
        "doctor" to ("Healthcare" to "#06B6D4"),
        "medicine" to ("Healthcare" to "#06B6D4"),

        // Subscriptions
        "netflix" to ("Subscriptions" to "#6366F1"),
        "spotify" to ("Subscriptions" to "#6366F1"),
        "youtube" to ("Subscriptions" to "#6366F1"),
        "prime" to ("Subscriptions" to "#6366F1"),
        "hotstar" to ("Subscriptions" to "#6366F1")
    )

    fun predict(text: String): CategorySuggestion? {
        val lower = text.lowercase().trim()
        if (lower.length < 2) return null

        val words = lower.split(Regex("[^a-zA-Z0-9&]+"))
        for (word in words) {
            KEYWORD_MAP[word]?.let { (cat, color) ->
                return CategorySuggestion(cat, color, word)
            }
        }
        return null
    }
}
