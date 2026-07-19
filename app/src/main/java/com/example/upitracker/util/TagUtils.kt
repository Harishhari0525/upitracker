package com.example.upitracker.util

object TagUtils {
    // Matches words starting with # containing letters, numbers, or underscores
    private val hashtagRegex = Regex("#\\w+")

    fun extractTags(text: String): String {
        return hashtagRegex.findAll(text).joinToString(" ") { it.value } // Store as "#tag1 #tag2"
    }
}