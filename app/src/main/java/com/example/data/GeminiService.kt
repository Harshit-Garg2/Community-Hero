package com.example.data

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

object GeminiService {
    private const val TAG = "GeminiService"
    private const val MODEL_NAME = "gemini-2.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    data class AnalysisResult(
        val category: String,
        val severity: String, // "Low", "Medium", "High", "Critical"
        val reasoning: String,
        val recommendedAction: String,
        val emergencyTip: String
    )

    /**
     * Gets Firebase App Check Token to securely sign requests sent to our proxy.
     */
    private suspend fun getAppCheckToken(): String? = kotlinx.coroutines.suspendCancellableCoroutine { continuation ->
        try {
            val appCheck = com.google.firebase.appcheck.FirebaseAppCheck.getInstance()
            appCheck.getAppCheckToken(false)
                .addOnSuccessListener { tokenResult ->
                    if (continuation.isActive) {
                        continuation.resume(tokenResult.token)
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Failed to get App Check token: ${exception.message}")
                    if (continuation.isActive) {
                        continuation.resume(null)
                    }
                }
        } catch (e: Throwable) {
            Log.w(TAG, "Firebase App Check is not initialized or not supported on this device.")
            if (continuation.isActive) {
                continuation.resume(null)
            }
        }
    }

    /**
     * Analyzes issue details using Gemini API and returns structured classification.
     */
    suspend fun analyzeIssue(title: String, description: String): AnalysisResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        val proxyUrl = try { BuildConfig.GEMINI_PROXY_URL } catch (e: Throwable) { "" }
        val isUsingProxy = !proxyUrl.isNullOrBlank() && proxyUrl != "MY_GEMINI_PROXY_URL"

        // Graceful check for missing key and missing proxy
        if (!isUsingProxy && (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY")) {
            Log.w(TAG, "Neither Gemini API key nor Proxy URL is configured. Using fallback local analysis.")
            return@withContext fallbackAnalysis(title, description)
        }

        val prompt = """
            You are "Community Hero AI", a civic intelligence system.
            Analyze the following citizen report:
            Title: "$title"
            Description: "$description"
            
            Provide a response strictly in valid JSON format with the following keys:
            - "category": Must be one of: "Pothole", "Water Leakage", "Damaged Streetlight", "Waste Management", "Public Safety", or "Public Infrastructure"
            - "severity": Must be one of: "Low", "Medium", "High", "Critical"
            - "reasoning": A short (1 sentence) explanation of why you selected this severity and category.
            - "recommendedAction": A short, concrete suggestion of what city department or local response team should do.
            - "emergencyTip": A safety warning or tip for local citizens passing by this issue.
            
            Strictly output only raw JSON. Do not include any markdown wrappers (like ```json).
        """.trimIndent()

        try {
            val jsonRequest = JSONObject().apply {
                put("contents", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", org.json.JSONArray().apply {
                            put(JSONObject().apply {
                                put("text", prompt)
                            })
                        })
                    })
                })
                // Enable Google Search/Maps grounding tool for accurate real-time local intelligence
                put("tools", org.json.JSONArray().apply {
                    put(JSONObject().apply {
                        put("googleSearch", JSONObject())
                    })
                })
            }

            val requestBody = jsonRequest.toString().toRequestBody("application/json".toMediaType())
            
            val url = if (isUsingProxy) {
                proxyUrl
            } else {
                "$BASE_URL?key=$apiKey"
            }

            val requestBuilder = Request.Builder()
                .url(url)
                .post(requestBody)
                .header("Content-Type", "application/json")

            if (isUsingProxy) {
                // Attach App Check Token to proxy request to verify client authenticity
                val appCheckToken = getAppCheckToken()
                if (!appCheckToken.isNullOrBlank()) {
                    requestBuilder.header("X-Firebase-AppCheck", appCheckToken)
                    Log.d(TAG, "App Check token successfully attached to request.")
                }
            }

            val request = requestBuilder.build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Gemini API call failed: Code ${response.code}, Message: ${response.message}")
                    return@withContext fallbackAnalysis(title, description)
                }

                val responseBodyStr = response.body?.string()
                if (responseBodyStr.isNullOrBlank()) {
                    return@withContext fallbackAnalysis(title, description)
                }

                // Parse standard Gemini structure (if direct or proxy returning standard structure):
                // candidates -> content -> parts -> text
                val rootJson = JSONObject(responseBodyStr)
                
                // Check if response is raw JSON directly or wrapped in standard Gemini schema
                var rawText = ""
                if (rootJson.has("candidates")) {
                    val candidates = rootJson.optJSONArray("candidates")
                    val firstCandidate = candidates?.optJSONObject(0)
                    val content = firstCandidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val firstPart = parts?.optJSONObject(0)
                    rawText = firstPart?.optString("text")?.trim() ?: ""
                } else {
                    // Direct JSON from custom proxy
                    rawText = responseBodyStr.trim()
                }

                // Clean markdown if Gemini outputted it despite instructions
                if (rawText.startsWith("```")) {
                    rawText = rawText.substringAfter("```")
                    if (rawText.startsWith("json")) {
                        rawText = rawText.substringAfter("json")
                    }
                    rawText = rawText.substringBeforeLast("```")
                }
                rawText = rawText.trim()

                val resultJson = JSONObject(rawText)
                AnalysisResult(
                    category = resultJson.optString("category", "Public Infrastructure"),
                    severity = resultJson.optString("severity", "Medium"),
                    reasoning = resultJson.optString("reasoning", "Categorized based on report keywords."),
                    recommendedAction = resultJson.optString("recommendedAction", "Report sent to city public works."),
                    emergencyTip = resultJson.optString("emergencyTip", "Use caution when traveling near this area.")
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during Gemini API analysis: ${e.message}", e)
            fallbackAnalysis(title, description)
        }
    }

    /**
     * Local fallback heuristics if Gemini API is offline or key is missing.
     */
    private fun fallbackAnalysis(title: String, description: String): AnalysisResult {
        val combined = "$title $description".lowercase()
        val category: String
        val severity: String
        val reasoning: String
        val recommendedAction: String
        val emergencyTip: String

        when {
            combined.contains("pothole") || combined.contains("cracked road") || combined.contains("street") -> {
                category = "Pothole"
                severity = if (combined.contains("large") || combined.contains("deep") || combined.contains("accident")) "High" else "Medium"
                reasoning = "Identified as road/pothole issue based on description keywords."
                recommendedAction = "Alert local road maintenance crew to fill road cavity."
                emergencyTip = "Slow down and avoid lane changes around the pavement crack."
            }
            combined.contains("water") || combined.contains("leak") || combined.contains("pipe") || combined.contains("flooding") -> {
                category = "Water Leakage"
                severity = if (combined.contains("flood") || combined.contains("burst") || combined.contains("gushing")) "Critical" else "Medium"
                reasoning = "Water utility indicators detected."
                recommendedAction = "Dispatch emergency water works and plumbing division."
                emergencyTip = "Avoid wading in water accumulations due to potential slip or pollution hazards."
            }
            combined.contains("light") || combined.contains("streetlight") || combined.contains("dark") || combined.contains("lamp") -> {
                category = "Damaged Streetlight"
                severity = if (combined.contains("dark intersection") || combined.contains("unsafe")) "High" else "Low"
                reasoning = "Public illumination concern flagged."
                recommendedAction = "Request electrical department to inspect light fixture and replace bulb."
                emergencyTip = "Stay in well-lit paths and keep flashlight handy in darkness."
            }
            combined.contains("trash") || combined.contains("garbage") || combined.contains("waste") || combined.contains("smell") || combined.contains("dump") -> {
                category = "Waste Management"
                severity = "Medium"
                reasoning = "Refuse accumulation or environmental sanitation issue recognized."
                recommendedAction = "Schedule sanitation vehicle for bulk rubbish collection."
                emergencyTip = "Keep children and pets away to prevent pathogen contact."
            }
            combined.contains("dangerous") || combined.contains("crime") || combined.contains("hazard") || combined.contains("wire") || combined.contains("electric") -> {
                category = "Public Safety"
                severity = "Critical"
                reasoning = "High-risk safety conditions or immediate hazards detected."
                recommendedAction = "Notify civil defense or local power grid emergency responders."
                emergencyTip = "DO NOT approach! Maintain a safe radius and keep bystanders clear."
            }
            else -> {
                category = "Public Infrastructure"
                severity = "Medium"
                reasoning = "General municipal infrastructure report classified automatically."
                recommendedAction = "Route report details to municipal complaints desk for validation."
                emergencyTip = "Keep alert and maintain situational awareness in the vicinity."
            }
        }

        return AnalysisResult(category, severity, reasoning, recommendedAction, emergencyTip)
    }
}
