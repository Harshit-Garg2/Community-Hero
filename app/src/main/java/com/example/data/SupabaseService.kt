package com.example.data

import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object SupabaseService {
    private const val TAG = "SupabaseService"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun getCleanBaseUrl(baseUrl: String): String {
        var clean = baseUrl.trim().trimEnd('/')
        if (clean.endsWith("/rest/v1")) {
            clean = clean.substring(0, clean.length - "/rest/v1".length)
        } else if (clean.endsWith("/auth/v1")) {
            clean = clean.substring(0, clean.length - "/auth/v1".length)
        }
        return clean.trimEnd('/')
    }

    data class AuthResult(
        val accessToken: String,
        val userId: String,
        val email: String,
        val displayName: String,
        val error: String? = null
    )

    /**
     * Signs up a new user via Supabase Auth.
     */
    fun signUp(
        baseUrl: String,
        anonKey: String,
        email: String,
        password: String,
        displayName: String
    ): AuthResult {
        try {
            val cleanBase = getCleanBaseUrl(baseUrl)
            val url = "$cleanBase/auth/v1/signup"
            
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("password", password)
                put("data", JSONObject().apply {
                    put("display_name", displayName)
                })
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(bodyStr)
                    return AuthResult("", "", "", "", errorMsg)
                }

                val json = JSONObject(bodyStr)
                val accessToken = json.optString("access_token") ?: ""
                val userJson = json.optJSONObject("user")
                val userId = userJson?.optString("id") ?: ""
                val userEmail = userJson?.optString("email") ?: email
                val userMetadata = userJson?.optJSONObject("user_metadata")
                val name = userMetadata?.optString("display_name") ?: displayName

                return AuthResult(accessToken, userId, userEmail, name)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign up error", e)
            return AuthResult("", "", "", "", e.localizedMessage ?: "Network connection failed")
        }
    }

    /**
     * Signs in an existing user via Supabase Auth (Resource Owner Password Credentials).
     */
    fun signIn(
        baseUrl: String,
        anonKey: String,
        email: String,
        password: String
    ): AuthResult {
        try {
            val cleanBase = getCleanBaseUrl(baseUrl)
            val url = "$cleanBase/auth/v1/token?grant_type=password"
            
            val jsonBody = JSONObject().apply {
                put("email", email)
                put("password", password)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = parseErrorMessage(bodyStr)
                    return AuthResult("", "", "", "", errorMsg)
                }

                val json = JSONObject(bodyStr)
                val accessToken = json.optString("access_token") ?: ""
                val userJson = json.optJSONObject("user")
                val userId = userJson?.optString("id") ?: ""
                val userEmail = userJson?.optString("email") ?: email
                val userMetadata = userJson?.optJSONObject("user_metadata")
                val name = userMetadata?.optString("display_name") ?: "Citizen Hero"

                return AuthResult(accessToken, userId, userEmail, name)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in error", e)
            return AuthResult("", "", "", "", e.localizedMessage ?: "Network connection failed")
        }
    }

    /**
     * Pushes a community issue to Supabase table `community_issues`.
     */
    fun insertIssue(
        baseUrl: String,
        anonKey: String,
        token: String,
        issue: CommunityIssue
    ): Boolean {
        try {
            val cleanBase = getCleanBaseUrl(baseUrl)
            val url = "$cleanBase/rest/v1/community_issues"
            
            val json = JSONObject().apply {
                put("title", issue.title)
                put("description", issue.description)
                put("category", issue.category)
                put("latitude", issue.latitude)
                put("longitude", issue.longitude)
                put("address", issue.address)
                put("image_url", issue.imageUrl)
                put("status", issue.status)
                put("upvotes", issue.upvotes)
                put("downvotes", issue.downvotes)
                put("timestamp", issue.timestamp)
                put("reporter_name", issue.reporterName)
                put("ai_categorized", issue.aiCategorized)
                put("severity_level", issue.severityLevel)
                put("city", issue.city)
            }

            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $token")
                .addHeader("Content-Type", "application/json")
                .post(json.toString().toRequestBody("application/json".toMediaType()))
                .build()

            client.newCall(request).execute().use { response ->
                return response.isSuccessful
            }
        } catch (e: Exception) {
            Log.e(TAG, "Insert issue error", e)
            return false
        }
    }

    /**
     * Fetches issues from Supabase table `community_issues`.
     */
    fun fetchIssues(
        baseUrl: String,
        anonKey: String
    ): List<CommunityIssue> {
        val list = mutableListOf<CommunityIssue>()
        try {
            val cleanBase = getCleanBaseUrl(baseUrl)
            val url = "$cleanBase/rest/v1/community_issues?select=*"
            
            val request = Request.Builder()
                .url(url)
                .addHeader("apikey", anonKey)
                .addHeader("Authorization", "Bearer $anonKey")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return emptyList()
                val bodyStr = response.body?.string() ?: "[]"
                val jsonArray = JSONArray(bodyStr)
                for (i in 0 until jsonArray.length()) {
                    val obj = jsonArray.getJSONObject(i)
                    list.add(
                        CommunityIssue(
                            id = obj.optInt("id", 0),
                            title = obj.optString("title", ""),
                            description = obj.optString("description", ""),
                            category = obj.optString("category", "General"),
                            latitude = obj.optDouble("latitude", 0.0),
                            longitude = obj.optDouble("longitude", 0.0),
                            address = obj.optString("address", ""),
                            imageUrl = obj.optString("image_url", ""),
                            status = obj.optString("status", "Reported"),
                            upvotes = obj.optInt("upvotes", 0),
                            downvotes = obj.optInt("downvotes", 0),
                            timestamp = obj.optLong("timestamp", System.currentTimeMillis()),
                            reporterName = obj.optString("reporter_name", "Anonymous Citizen"),
                            aiCategorized = obj.optBoolean("ai_categorized", false),
                            severityLevel = obj.optString("severity_level", "Medium"),
                            city = obj.optString("city", "Dehradun")
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch issues error", e)
        }
        return list
    }

    private fun parseErrorMessage(responseBody: String): String {
        if (responseBody.isBlank()) return "Unknown server error (empty response)"
        return try {
            val json = JSONObject(responseBody)
            
            // Check msg first (most common in GoTrue / Supabase Auth)
            val msg = json.optString("msg", "")
            if (msg.isNotBlank()) return msg
            
            // Check error_description
            val errDesc = json.optString("error_description", "")
            if (errDesc.isNotBlank()) return errDesc
            
            // Check message
            val message = json.optString("message", "")
            if (message.isNotBlank()) return message
            
            // Check error field itself (can be a string or object)
            val errorField = json.opt("error")
            if (errorField is String && errorField.isNotBlank()) {
                return errorField
            } else if (errorField is JSONObject) {
                val subMsg = errorField.optString("message", errorField.optString("msg", ""))
                if (subMsg.isNotBlank()) return subMsg
            }
            
            // Fallback to raw string if nothing found
            responseBody.take(200)
        } catch (e: Exception) {
            responseBody.take(150)
        }
    }
}
