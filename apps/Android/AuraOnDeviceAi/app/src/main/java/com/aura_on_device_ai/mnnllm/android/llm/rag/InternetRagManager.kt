package com.aura_on_device_ai.mnnllm.android.llm.rag

import android.content.Context
import android.util.Log

/**
 * Manages fetching internet data and building the search-augmented prompt.
 * Simplified as per user request: No memory, only 5 snippet-based sources.
 */
class InternetRagManager(context: Context) {
    private val scraper = DuckDuckGoScraper()
    private val TAG = "InternetRagManager"

    suspend fun getAugmentedPrompt(userQuery: String): String {
        Log.d(TAG, "Fetching internet results for: $userQuery")
        
        // Search first and take maximum 3 sources immediately
        var results = scraper.search(userQuery).take(3)

        if (results.isEmpty()) return userQuery

        // Build the context block
        val contextBuilder = StringBuilder()
        contextBuilder.append("### SYSTEM: REAL-TIME KNOWLEDGE OVERRIDE ###\n")
        contextBuilder.append("The user has enabled LIVE INTERNET SEARCH. Provide an accurate answer based ONLY on the current web summaries below.\n\n")

        results.forEachIndexed { index, result ->
            contextBuilder.append("--- SOURCE ${index + 1} ---\n")
            contextBuilder.append("TITLE: ${result.title}\n")
            contextBuilder.append("SUMMARY: ${result.snippet}\n\n")
        }

        contextBuilder.append("### INSTRUCTIONS ###\n")
        contextBuilder.append("1. Prioritize these ${results.size} internet sources for your response. Give detailed content!\n")
        contextBuilder.append("2. If the info conflicts with your internal training data, the internet summaries are the source of truth.\n\n")
        
        contextBuilder.append("Question: $userQuery\n\nVerified Answer: \n")

        return contextBuilder.toString()
    }
}
