package com.example.upitracker.network

import android.util.Log
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.android.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

// Data class to hold the relevant fields from the GitHub API response
@Serializable
data class GitHubRelease(
    @SerialName("tag_name")
    val tagName: String = "", // e.g., "v1.7.1"
    @SerialName("name")
    val name: String = "", // e.g., "Version 1.7.1"
    @SerialName("body")
    val body: String = "", // The release notes
    @SerialName("html_url")
    val htmlUrl: String = "" // URL to the release page
)

object UpdateService {
    // Configure the HTTP client
    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 10_000
            connectTimeoutMillis = 10_000
            socketTimeoutMillis = 10_000
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true // Safely ignore fields we don't need
            })
        }
    }

    // IMPORTANT: Replace these with your actual GitHub username and repository name
    private const val GITHUB_OWNER = "harishhari0525"
    private const val GITHUB_REPO = "upitracker"

    suspend fun getLatestRelease(): GitHubRelease? {
        // Do not run if the placeholders haven't been replaced

        val url = "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"
        return try {
            client.get(url).body<GitHubRelease>().takeIf { release ->
                release.htmlUrl.lowercase(Locale.ROOT)
                    .startsWith("https://github.com/$GITHUB_OWNER/$GITHUB_REPO/")
            }
        } catch (e: Exception) {
            Log.e("UpdateService", "Failed to fetch latest release: ${e.message}")
            null
        }
    }
}
