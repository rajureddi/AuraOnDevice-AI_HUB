package com.aura_on_device_ai.mnnllm.android.llm.rag

import android.net.Uri
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.util.concurrent.TimeUnit

data class SearchResult(
    val title: String,
    val url: String,
    val snippet: String,
    val isWikipedia: Boolean = false
)

class DuckDuckGoScraper {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val TAG = "DuckDuckGoScraper"

    fun search(query: String): List<SearchResult> {
        val searchUrl = "https://html.duckduckgo.com/html/"
        Log.d(TAG, "Internet Search attempting POST: $searchUrl with query: $query")

        val formBody = okhttp3.FormBody.Builder()
            .add("q", query)
            .build()

        val request = Request.Builder()
            .url(searchUrl)
            .post(formBody)
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
            .header("Content-Type", "application/x-www-form-urlencoded")
            .build()

        try {
            val response = client.newCall(request).execute()
            if (!response.isSuccessful) {
                Log.w(TAG, "Search URL failed ($searchUrl): ${response.code}")
                return emptyList()
            }

            val html = response.body?.string() ?: return emptyList()
            val doc = Jsoup.parse(html)
            val results = mutableListOf<SearchResult>()

            // DuckDuckGo results are usually in divs with class "result" or "results_links"
            var resultElements = doc.select(".result")
            if (resultElements.isEmpty()) resultElements = doc.select(".results_links")
            if (resultElements.isEmpty()) resultElements = doc.select(".results_links_deep")
            
            if (resultElements.isEmpty()) {
                Log.w(TAG, "No result divs in HTML from $searchUrl. Checking raw page content...")
                if (html.length > 500) Log.d(TAG, "HTML (first 500): " + html.substring(0, 500).replace("\n", " "))
                return emptyList()
            }

            Log.d(TAG, "Found ${resultElements.size} raw result divs from $searchUrl")

            for (element in resultElements.take(15)) {
                val aElement = element.selectFirst("a.result__a") ?: 
                              element.selectFirst(".result__title a") ?: 
                              element.selectFirst("a[href^=http]") ?:
                              element.selectFirst("a[href^=//]")
                
                val snippetElement = element.selectFirst(".result__snippet") ?: 
                                    element.selectFirst(".snippet") ?:
                                    element.selectFirst("p")
                
                if (aElement != null) {
                    val title = aElement.text()
                    val rawUrl = aElement.attr("href")
                    val snippet = snippetElement?.text() ?: "No summary available"
                    
                    val decodedUrl = extractActualUrl(rawUrl)
                    if (decodedUrl.isEmpty() || decodedUrl.contains("duckduckgo.com") || decodedUrl.contains("proxy")) continue
                    
                    val isWiki = decodedUrl.contains("wikipedia.org", ignoreCase = true)
                    results.add(SearchResult(title, decodedUrl, snippet, isWiki))
                }
            }
            
            if (results.isNotEmpty()) {
                val finalResults = results.sortedByDescending { it.isWikipedia }.take(5)
                Log.d(TAG, "Success! Returning ${finalResults.size} search results")
                return finalResults
            }
        } catch (e: Exception) {
            Log.e(TAG, "Search attempt failed ($searchUrl)", e)
        }
        
        Log.e(TAG, "All search attempts failed to return results.")
        return emptyList()
    }

    private fun extractActualUrl(rawUrl: String): String {
        return try {
            var url = rawUrl
            if (url.startsWith("//")) {
                url = "https:$url"
            } else if (!url.startsWith("http")) {
                // If it's a relative path on DDG
                url = "https://duckduckgo.com$url"
            }

            // DDG redirects often have 'uddg=' parameter
            if (url.contains("uddg=")) {
                val uri = Uri.parse(url)
                val uddg = uri.getQueryParameter("uddg")
                if (uddg != null) return uddg
            }
            
            url
        } catch (e: Exception) {
            rawUrl
        }
    }
}
