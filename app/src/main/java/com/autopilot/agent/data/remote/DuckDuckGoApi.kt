package com.autopilot.agent.data.remote

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Retrofit interface for DuckDuckGo HTML search (no API key needed).
 */
interface DuckDuckGoApi {

    /**
     * Perform a web search using DuckDuckGo's HTML endpoint.
     * Returns raw HTML that needs to be parsed with Jsoup.
     */
    @GET("html/")
    suspend fun search(
        @Query("q") query: String
    ): Response<ResponseBody>
}
