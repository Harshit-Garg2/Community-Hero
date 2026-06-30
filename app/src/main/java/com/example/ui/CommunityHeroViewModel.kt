package com.example.ui

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed interface AppScreen {
    object Tutorial : AppScreen
    object Dashboard : AppScreen
    object ReportIssue : AppScreen
    data class IssueDetail(val issueId: Int) : AppScreen
    object ImpactDashboard : AppScreen
    object Leaderboard : AppScreen
    object Profile : AppScreen
}

sealed interface AiAnalysisState {
    object Idle : AiAnalysisState
    object Analyzing : AiAnalysisState
    data class Success(val result: GeminiService.AnalysisResult) : AiAnalysisState
    data class Error(val error: String) : AiAnalysisState
}

class CommunityHeroViewModel(application: Application) : AndroidViewModel(application) {
    private val database = IssueDatabase.getDatabase(application)
    private val repository = IssueRepository(database.issueDao())

    // Authentication & Supabase state
    private val _isLoggedIn = MutableStateFlow<Boolean>(false) // Defaults to false for Sign In / Sign Up
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _useSupabase = MutableStateFlow<Boolean>(true) // True by default for background sync
    val useSupabase: StateFlow<Boolean> = _useSupabase.asStateFlow()

    private val _supabaseUrl = MutableStateFlow<String>(BuildConfig.SUPABASE_URL)
    val supabaseUrl: StateFlow<String> = _supabaseUrl.asStateFlow()

    private val _supabaseKey = MutableStateFlow<String>(BuildConfig.SUPABASE_ANON_KEY)
    val supabaseKey: StateFlow<String> = _supabaseKey.asStateFlow()

    private val _sessionToken = MutableStateFlow<String?>(null)
    val sessionToken: StateFlow<String?> = _sessionToken.asStateFlow()

    private val _authError = MutableStateFlow<String?>(null)
    val authError: StateFlow<String?> = _authError.asStateFlow()

    private val _isAuthenticating = MutableStateFlow(false)
    val isAuthenticating: StateFlow<Boolean> = _isAuthenticating.asStateFlow()

    // UI state
    private val _currentScreen = MutableStateFlow<AppScreen>(AppScreen.Tutorial)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    private val _tutorialPage = MutableStateFlow(0)
    val tutorialPage: StateFlow<Int> = _tutorialPage.asStateFlow()

    // Database Flows
    val allIssues: StateFlow<List<CommunityIssue>> = repository.allIssues
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile> = repository.userProfile
        .map { it ?: UserProfile() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserProfile())

    // Selected issue & comments
    private val _selectedIssueId = MutableStateFlow<Int?>(null)
    val selectedIssue: StateFlow<CommunityIssue?> = _selectedIssueId
        .flatMapLatest { id ->
            if (id != null) repository.getIssueById(id) else flowOf(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val selectedIssueComments: StateFlow<List<IssueComment>> = _selectedIssueId
        .flatMapLatest { id ->
            if (id != null) repository.getCommentsForIssue(id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // AI suggestion state
    private val _aiAnalysisState = MutableStateFlow<AiAnalysisState>(AiAnalysisState.Idle)
    val aiAnalysisState: StateFlow<AiAnalysisState> = _aiAnalysisState.asStateFlow()

    // Local filters
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryFilter = MutableStateFlow("All")
    val selectedCategoryFilter: StateFlow<String> = _selectedCategoryFilter.asStateFlow()

    private val _selectedStatusFilter = MutableStateFlow("All")
    val selectedStatusFilter: StateFlow<String> = _selectedStatusFilter.asStateFlow()

    private val _selectedCity = MutableStateFlow("Dehradun")
    val selectedCity: StateFlow<String> = _selectedCity.asStateFlow()

    fun updateSelectedCity(city: String) {
        _selectedCity.value = city
    }

    // Prefilled Location from Map Long-Press
    data class PrefilledLocation(val latitude: Double, val longitude: Double, val address: String)
    private val _prefilledLocation = MutableStateFlow<PrefilledLocation?>(null)
    val prefilledLocation: StateFlow<PrefilledLocation?> = _prefilledLocation.asStateFlow()

    fun setPrefilledLocation(latitude: Double, longitude: Double, address: String) {
        _prefilledLocation.value = PrefilledLocation(latitude, longitude, address)
    }

    fun clearPrefilledLocation() {
        _prefilledLocation.value = null
    }

    init {
        // Pre-populate database with beautiful sample data if empty
        viewModelScope.launch {
            repository.allIssues.first().let { list ->
                if (list.isEmpty()) {
                    populateSampleData()
                }
            }
            // Initialize user profile
            val profile = repository.getUserProfileSuspend()
            if (profile == null) {
                repository.insertUserProfile(UserProfile())
                _isLoggedIn.value = false
            } else {
                // If they have an email, they are considered signed-in
                _isLoggedIn.value = profile.emailAddress.isNotBlank()
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
        if (screen is AppScreen.IssueDetail) {
            _selectedIssueId.value = screen.issueId
        } else {
            _selectedIssueId.value = null
        }
    }

    fun setTutorialPage(page: Int) {
        _tutorialPage.value = page
    }

    fun skipTutorial() {
        _currentScreen.value = AppScreen.Dashboard
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateCategoryFilter(category: String) {
        _selectedCategoryFilter.value = category
    }

    fun updateStatusFilter(status: String) {
        _selectedStatusFilter.value = status
    }

    // --- Gemini AI Action ---
    fun analyzeIssueWithAi(title: String, description: String) {
        if (title.isBlank() || description.isBlank()) return
        
        viewModelScope.launch {
            _aiAnalysisState.value = AiAnalysisState.Analyzing
            try {
                val result = GeminiService.analyzeIssue(title, description)
                _aiAnalysisState.value = AiAnalysisState.Success(result)
            } catch (e: Exception) {
                _aiAnalysisState.value = AiAnalysisState.Error(e.message ?: "Unknown AI analysis error")
            }
        }
    }

    fun clearAiAnalysis() {
        _aiAnalysisState.value = AiAnalysisState.Idle
    }

    // --- Civic Operations ---
    fun reportIssue(
        title: String,
        description: String,
        category: String,
        address: String,
        latitude: Double,
        longitude: Double,
        severity: String,
        imageUrl: String = ""
    ) {
        viewModelScope.launch {
            val isAiResult = _aiAnalysisState.value is AiAnalysisState.Success
            val issue = CommunityIssue(
                title = title,
                description = description,
                category = category,
                address = address,
                latitude = latitude,
                longitude = longitude,
                status = "Reported",
                severityLevel = severity,
                imageUrl = imageUrl,
                aiCategorized = isAiResult,
                reporterName = userProfile.value.name
            )

            repository.insertIssue(issue)
            
            // Upload to Supabase if connected
            val token = _sessionToken.value
            if (_useSupabase.value && !token.isNullOrBlank()) {
                viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                    SupabaseService.insertIssue(
                        _supabaseUrl.value,
                        _supabaseKey.value,
                        token,
                        issue
                    )
                }
            }
            
            // Gamification points! +50 points for submitting a report
            addPoints(50, reportAdded = true)
            triggerPushNotification("Report Submitted! 🚀", "Thank you for reporting '${issue.title}'. +50 points earned!")
            navigateTo(AppScreen.Dashboard)
            clearAiAnalysis()
        }
    }

    fun addComment(issueId: Int, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val profile = userProfile.value
            val comment = IssueComment(
                issueId = issueId,
                author = profile.name,
                content = content
            )
            repository.insertComment(comment)
            // Gamification points! +5 points for a comment
            addPoints(5)
            triggerPushNotification("New Comment Added 💬", "Your comment has been published. +5 points earned!")
        }
    }

    fun upvoteIssue(issueId: Int) {
        viewModelScope.launch {
            val issue = repository.getIssueByIdSuspend(issueId)
            val profile = repository.getUserProfileSuspend() ?: UserProfile()
            if (issue != null) {
                // Check verification
                if (!profile.isEmailVerified && !profile.isPhoneVerified) {
                    triggerPushNotification("Verification Required 🔐", "Please authenticate your account to validate issues.")
                    return@launch
                }
                
                // Check already voted (by profile ID)
                val validatedIds = profile.validatedIssueIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (validatedIds.contains(issueId.toString())) {
                    triggerPushNotification("Already Validated ⚠️", "You have already validated/upvoted this issue.")
                    return@launch
                }

                // Check already voted (by name)
                val validators = issue.validatedUsersListJson.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableList()
                
                if (validators.contains(profile.name)) {
                    triggerPushNotification("Already Validated ⚠️", "You have already validated/upvoted this issue.")
                    return@launch
                }
                
                validators.add(profile.name)
                val updated = issue.copy(
                    upvotes = issue.upvotes + 1,
                    validatedUsersListJson = validators.joinToString(",")
                )
                repository.updateIssue(updated)

                // Update User Profile
                val updatedValidatedIds = validatedIds.toMutableList().apply { add(issueId.toString()) }
                val updatedProfile = profile.copy(validatedIssueIds = updatedValidatedIds.joinToString(","))
                repository.insertUserProfile(updatedProfile)

                // Gamification points! +10 points for validating/upvoting an issue
                addPoints(10, verifiedAdded = true)
                triggerPushNotification("Validation Logged! 👍", "You upvoted/verified the issue '${issue.title}'. +10 points earned!")
            }
        }
    }

    fun verifyIssue(issueId: Int, isValid: Boolean) {
        viewModelScope.launch {
            val issue = repository.getIssueByIdSuspend(issueId)
            val profile = repository.getUserProfileSuspend() ?: UserProfile()
            if (issue != null) {
                // Ensure verified user
                if (!profile.isEmailVerified && !profile.isPhoneVerified) {
                    triggerPushNotification("Verification Required 🔐", "Please authenticate your account to validate issues.")
                    return@launch
                }

                // Check if they already validated (by profile ID)
                val validatedIds = profile.validatedIssueIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (validatedIds.contains(issueId.toString())) {
                    triggerPushNotification("Already Validated ⚠️", "You have already validated this issue.")
                    return@launch
                }

                // Check if they already validated (by name)
                val validators = issue.validatedUsersListJson.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableList()

                if (validators.contains(profile.name)) {
                    triggerPushNotification("Already Validated ⚠️", "You have already validated this issue.")
                    return@launch
                }

                validators.add(profile.name)
                val currentVotes = if (isValid) issue.upvotes + 1 else issue.downvotes
                val newStatus = if (issue.status == "Reported" && currentVotes >= 3) "Verifying" else issue.status
                val updated = if (isValid) {
                    issue.copy(
                        upvotes = issue.upvotes + 1,
                        status = newStatus,
                        validatedUsersListJson = validators.joinToString(",")
                    )
                } else {
                    issue.copy(
                        downvotes = issue.downvotes + 1,
                        validatedUsersListJson = validators.joinToString(",")
                    )
                }
                repository.updateIssue(updated)

                // Update User Profile
                val updatedValidatedIds = validatedIds.toMutableList().apply { add(issueId.toString()) }
                val updatedProfile = profile.copy(validatedIssueIds = updatedValidatedIds.joinToString(","))
                repository.insertUserProfile(updatedProfile)

                addPoints(10, verifiedAdded = true)
                triggerPushNotification("Validation Logged! 👍", "You validated the issue '${issue.title}'. +10 points earned!")
            }
        }
    }

    fun updateIssueStatus(issueId: Int, newStatus: String, notes: String? = null) {
        viewModelScope.launch {
            val issue = repository.getIssueByIdSuspend(issueId)
            if (issue != null) {
                val updated = issue.copy(
                    status = newStatus,
                    resolutionNotes = notes ?: issue.resolutionNotes
                )
                repository.updateIssue(updated)
                if (newStatus == "Resolved") {
                    // +100 points for resolving
                    addPoints(100, resolvedAdded = true)
                } else {
                    triggerPushNotification("Status Updated to $newStatus! 📋", "Issue '${issue.title}' has updated its lifecycle progress to $newStatus.")
                }
            }
        }
    }

    fun updateProfileName(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            val profile = userProfile.value.copy(name = newName)
            repository.insertUserProfile(profile)
        }
    }

    fun updateUserProfile(name: String, phoneNumber: String, address: String, profilePicture: String) {
        viewModelScope.launch {
            val current = userProfile.value
            val updated = current.copy(
                name = name.ifBlank { current.name },
                phoneNumber = phoneNumber,
                address = address,
                profilePicture = profilePicture
            )
            repository.insertUserProfile(updated)
            triggerPushNotification("Profile Updated! 👤", "Your citizen profile details have been securely updated.")
        }
    }

    // --- User Verification Flows (Simulated OTP with +40 civic points rewards) ---
    fun verifyUserEmail(email: String) {
        if (email.isBlank()) return
        viewModelScope.launch {
            val current = repository.getUserProfileSuspend() ?: UserProfile()
            val totalPoints = current.points + 40
            val currentLevel = calculateLevel(totalPoints)
            val updated = current.copy(
                isEmailVerified = true,
                emailAddress = email,
                points = totalPoints,
                level = currentLevel,
                verifiedCount = current.verifiedCount + 1
            )
            repository.insertUserProfile(updated)
        }
    }

    fun verifyUserPhone(phone: String) {
        if (phone.isBlank()) return
        viewModelScope.launch {
            val current = repository.getUserProfileSuspend() ?: UserProfile()
            val totalPoints = current.points + 40
            val currentLevel = calculateLevel(totalPoints)
            val updated = current.copy(
                isPhoneVerified = true,
                phoneNumber = phone,
                points = totalPoints,
                level = currentLevel,
                verifiedCount = current.verifiedCount + 1
            )
            repository.insertUserProfile(updated)
        }
    }

    // --- Authentication & Supabase Cloud Sync Operations ---
    fun updateSupabaseConfig(url: String, key: String) {
        _supabaseUrl.value = url.trim()
        _supabaseKey.value = key.trim()
    }

    fun toggleSupabase(enabled: Boolean) {
        _useSupabase.value = enabled
        if (enabled) {
            syncIssuesFromSupabase()
        }
    }

    fun localLoginOrCreate(email: String, displayName: String) {
        _isAuthenticating.value = true
        _authError.value = null
        viewModelScope.launch {
            try {
                val current = repository.getUserProfileSuspend() ?: UserProfile()
                val updated = current.copy(
                    emailAddress = email,
                    name = displayName,
                    isEmailVerified = email.isNotBlank()
                )
                repository.insertUserProfile(updated)
                _isLoggedIn.value = true
                _useSupabase.value = false
                _sessionToken.value = null
                _authError.value = null
            } catch (e: Exception) {
                _authError.value = e.localizedMessage ?: "Failed to save local offline profile"
            } finally {
                _isAuthenticating.value = false
            }
        }
    }

    fun signInWithSupabase(email: String, pword: String) {
        _isAuthenticating.value = true
        _authError.value = null
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = SupabaseService.signIn(
                baseUrl = _supabaseUrl.value,
                anonKey = _supabaseKey.value,
                email = email,
                password = pword
            )
            viewModelScope.launch {
                if (result.error != null) {
                    _authError.value = result.error
                    _isAuthenticating.value = false
                } else {
                    _sessionToken.value = result.accessToken
                    _useSupabase.value = true
                    
                    // Update Local Room profile with the logged-in user details
                    val current = repository.getUserProfileSuspend() ?: UserProfile()
                    val updated = current.copy(
                        userId = "current_user",
                        name = result.displayName,
                        emailAddress = result.email,
                        isEmailVerified = true
                    )
                    repository.insertUserProfile(updated)
                    _isLoggedIn.value = true
                    _authError.value = null
                    _isAuthenticating.value = false
                    
                    // Sync cloud issues down to local Room DB
                    syncIssuesFromSupabase()
                }
            }
        }
    }

    fun signUpWithSupabase(email: String, pword: String, displayName: String) {
        _isAuthenticating.value = true
        _authError.value = null
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            val result = SupabaseService.signUp(
                baseUrl = _supabaseUrl.value,
                anonKey = _supabaseKey.value,
                email = email,
                password = pword,
                displayName = displayName
            )
            viewModelScope.launch {
                if (result.error != null) {
                    _authError.value = result.error
                    _isAuthenticating.value = false
                } else {
                    _sessionToken.value = result.accessToken
                    _useSupabase.value = true
                    
                    // Update Local Room profile with the logged-in user details
                    val current = repository.getUserProfileSuspend() ?: UserProfile()
                    val updated = current.copy(
                        userId = "current_user",
                        name = result.displayName,
                        emailAddress = result.email,
                        isEmailVerified = true
                    )
                    repository.insertUserProfile(updated)
                    _isLoggedIn.value = true
                    _authError.value = null
                    _isAuthenticating.value = false
                    
                    // Sync cloud issues down to local Room DB
                    syncIssuesFromSupabase()
                }
            }
        }
    }

    fun syncIssuesFromSupabase() {
        if (!_useSupabase.value) return
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val cloudIssues = SupabaseService.fetchIssues(_supabaseUrl.value, _supabaseKey.value)
                if (cloudIssues.isNotEmpty()) {
                    for (issue in cloudIssues) {
                        repository.insertIssue(issue)
                    }
                }
            } catch (e: Exception) {
                Log.e("CommunityHeroViewModel", "Supabase sync error", e)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            // Reset to clean local defaults
            val defaultProfile = UserProfile(
                userId = "current_user",
                name = "Citizen Hero",
                level = 1,
                points = 50,
                totalReports = 0,
                resolvedCount = 0,
                verifiedCount = 0,
                isEmailVerified = false,
                isPhoneVerified = false,
                emailAddress = "",
                phoneNumber = ""
            )
            repository.insertUserProfile(defaultProfile)
            _isLoggedIn.value = false
            _useSupabase.value = false
            _sessionToken.value = null
            _authError.value = null
        }
    }

    // --- Verified Consensus Resolution Voting (Bot Prevention) ---
    fun submitResolveVote(issueId: Int, voterName: String) {
        viewModelScope.launch {
            val issue = repository.getIssueByIdSuspend(issueId)
            val profile = repository.getUserProfileSuspend() ?: UserProfile()
            if (issue != null && (profile.isEmailVerified || profile.isPhoneVerified)) {
                // Check if they already signed off (by profile ID)
                val signedOffIds = profile.signedOffIssueIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                if (signedOffIds.contains(issueId.toString())) {
                    triggerPushNotification("Already Signed Off ⚠️", "You have already signed off on this resolution.")
                    return@launch
                }

                // Parse existing comma separated voters (by name)
                val voters = issue.votedUsersListJson.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .toMutableList()
                
                if (voters.contains(voterName)) {
                    triggerPushNotification("Already Signed Off ⚠️", "You have already signed off on this resolution.")
                    return@launch
                }

                voters.add(voterName)
                val newCount = issue.resolveVotesCount + 1
                val isFullyResolved = newCount >= issue.resolveVotesRequired
                val updatedStatus = if (isFullyResolved) "Resolved" else issue.status
                
                val notes = if (isFullyResolved) {
                    "Resolved automatically via community verification sign-off consensus (${newCount}/${issue.resolveVotesRequired} votes achieved)."
                } else {
                    issue.resolutionNotes
                }
                
                val updatedIssue = issue.copy(
                    resolveVotesCount = newCount,
                    votedUsersListJson = voters.joinToString(","),
                    status = updatedStatus,
                    resolutionNotes = notes
                )
                repository.updateIssue(updatedIssue)

                // Update User Profile
                val updatedSignedOffIds = signedOffIds.toMutableList().apply { add(issueId.toString()) }
                val updatedProfile = profile.copy(signedOffIssueIds = updatedSignedOffIds.joinToString(","))
                repository.insertUserProfile(updatedProfile)
                
                // Reward the voter with +25 points for verification action
                addPoints(25, verifiedAdded = true)
                
                // If fully resolved, reward the community
                if (isFullyResolved) {
                    addPoints(50, resolvedAdded = true)
                }
            }
        }
    }

    fun triggerPushNotification(title: String, message: String) {
        CivicNotificationHelper.sendNotification(getApplication(), title, message)
    }

    // --- Points Management ---
    private suspend fun addPoints(
        pts: Int,
        reportAdded: Boolean = false,
        resolvedAdded: Boolean = false,
        verifiedAdded: Boolean = false
    ) {
        val current = repository.getUserProfileSuspend() ?: UserProfile()
        val totalPoints = current.points + pts
        val currentLevel = calculateLevel(totalPoints)

        // Check level-up milestone
        if (currentLevel > current.level) {
            val lvlName = when (currentLevel) {
                2 -> "Public Sentinel"
                3 -> "Waverly Guardian"
                4 -> "Neighborhood Legend"
                else -> "Civic Champion"
            }
            triggerPushNotification(
                "Level Up! 🎉",
                "Congratulations! You reached Credibility Level $currentLevel ($lvlName)!"
            )
        }

        // Check point badge milestones
        if (current.points < 50 && totalPoints >= 50) {
            triggerPushNotification(
                "Badge Unlocked: Civic Rookie! 🏅",
                "You have earned 50+ points and unlocked the Civic Rookie badge."
            )
        }
        if (current.points < 100 && totalPoints >= 100) {
            triggerPushNotification(
                "Badge Unlocked: Pothole Hunter! 🔍",
                "You have earned 100+ points and unlocked the Pothole Hunter badge."
            )
        }
        if (current.points < 250 && totalPoints >= 250) {
            triggerPushNotification(
                "Badge Unlocked: Community Guardian! 🛡️",
                "You have earned 250+ points and unlocked the Community Guardian badge."
            )
        }
        if (current.points < 500 && totalPoints >= 500) {
            triggerPushNotification(
                "Badge Unlocked: Neighborhood Hero! 🏆",
                "Legend status achieved! You unlocked the Neighborhood Hero badge."
            )
        }

        // Send notification for resolving
        if (resolvedAdded) {
            triggerPushNotification("Issue Resolved! 🎉", "Great job! You successfully resolved an issue. +100 points earned!")
        }

        val updated = current.copy(
            points = totalPoints,
            level = currentLevel,
            totalReports = current.totalReports + (if (reportAdded) 1 else 0),
            resolvedCount = current.resolvedCount + (if (resolvedAdded) 1 else 0),
            verifiedCount = current.verifiedCount + (if (verifiedAdded) 1 else 0)
        )
        repository.insertUserProfile(updated)
    }

    private fun calculateLevel(points: Int): Int {
        return when {
            points >= 600 -> 4
            points >= 300 -> 3
            points >= 100 -> 2
            else -> 1
        }
    }

    private suspend fun populateSampleData() {
        val issues = listOf(
            // DEHRADUN ISSUES
            CommunityIssue(
                title = "Deep Crater Pothole",
                description = "Massive deep asphalt pothole right next to the main crossing of IT Park Bus Stand. Multiple vehicles have damaged their tires and it poses an extreme risk for two-wheelers at night.",
                category = "Pothole",
                latitude = 30.3601,
                longitude = 78.0772,
                address = "IT park Bus stand, dehradun",
                imageUrl = "pothole",
                status = "Reported",
                upvotes = 18,
                severityLevel = "Critical",
                reporterName = "Elena Carter",
                city = "Dehradun",
                timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
            ),
            CommunityIssue(
                title = "Broken Streetlight on Sahastradhara Road",
                description = "The main street lamp near the IT Park crossing is completely damaged and dead. This segment of the highway is pitch black at night, leading to active safety concerns.",
                category = "Damaged Streetlight",
                latitude = 30.3585,
                longitude = 78.0792,
                address = "Sahastradhara Rd near IT Park, Dehradun",
                imageUrl = "streetlight",
                status = "In Progress",
                upvotes = 8,
                severityLevel = "Medium",
                reporterName = "Alex Rivera",
                city = "Dehradun",
                timestamp = System.currentTimeMillis() - 172800000 // 2 days ago
            ),
            CommunityIssue(
                title = "Severe Water Logging near Bus Stand",
                description = "Heavy rain has caused significant drainage blockage and water logging near the IT Park Bus Stand entrance. Pedestrians cannot access the transit platform safely.",
                category = "Water Leakage",
                latitude = 30.3602,
                longitude = 78.0775,
                address = "IT park Bus stand, dehradun",
                imageUrl = "leak",
                status = "Resolved",
                upvotes = 12,
                severityLevel = "High",
                reporterName = "Marcus Vance",
                resolutionNotes = "Municipal suction crew has cleared the drainage grate of leaves and plastic trash. Stream is flowing perfectly now.",
                city = "Dehradun",
                timestamp = System.currentTimeMillis() - 432000000 // 5 days ago
            ),
            // METROPOLIS ISSUES
            CommunityIssue(
                title = "Central Park Trash Overrun",
                description = "The main trash receptacles next to the picnic meadow are overflowing with picnic trash. High winds are scattering plastic bags and wrappers into the lake.",
                category = "Waste Management",
                latitude = 40.7110,
                longitude = -74.0090,
                address = "Metropolis Central Park East",
                imageUrl = "trash",
                status = "Reported",
                upvotes = 5,
                severityLevel = "Medium",
                reporterName = "Marcus Vance",
                city = "Metropolis",
                timestamp = System.currentTimeMillis() - 86400000 // 1 day ago
            ),
            CommunityIssue(
                title = "Avenue A Main Line Rupture",
                description = "Clean drinking water is bursting out of a cracked pavement seam on Avenue A. It is creating a constant stream that floods the entire pedestrian path.",
                category = "Water Leakage",
                latitude = 40.7180,
                longitude = -74.0040,
                address = "12 Avenue A, Metropolis",
                imageUrl = "leak",
                status = "Verifying",
                upvotes = 8,
                severityLevel = "Critical",
                reporterName = "Sophie Dubois",
                city = "Metropolis",
                timestamp = System.currentTimeMillis() - 14400000 // 4 hours ago
            )
        )

        for (issue in issues) {
            val id = repository.insertIssue(issue).toInt()
            
            // Add some initial comments
            if (id == 1) {
                repository.insertComment(IssueComment(issueId = id, author = "Chief Inspector", content = "Report reviewed and forwarded to Asphalt Repair Division."))
                repository.insertComment(IssueComment(issueId = id, author = "Resident Jack", content = "Almost hit this on my bike. Thanks for reporting!"))
            } else if (id == 2) {
                repository.insertComment(IssueComment(issueId = id, author = "Elena Carter", content = "It is so much safer to cross now. Thank you, town administration!"))
            } else if (id == 3) {
                repository.insertComment(IssueComment(issueId = id, author = "Plumber Pete", content = "Thanks for the swift resolution. Public transit is usable again!"))
            }
        }
    }
}
