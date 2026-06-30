package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "community_issues")
data class CommunityIssue(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val imageUrl: String, // Can store base64, file path, or mock resource name
    val status: String,   // "Reported", "Verifying", "In Progress", "Resolved"
    val upvotes: Int = 0,
    val downvotes: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val reporterName: String = "Citizen Hero",
    val aiCategorized: Boolean = false,
    val severityLevel: String = "Medium", // "Low", "Medium", "High", "Critical"
    val resolutionNotes: String? = null,
    val city: String = "Dehradun",
    
    // Resolution voting for verified citizens to prevent bots
    val resolveVotesCount: Int = 0,
    val resolveVotesRequired: Int = 3,
    val votedUsersListJson: String = "", // Comma-separated list of voters (e.g. "Harg2468,Citizen Bob")
    val reporterId: String = "current_user",
    val validatedUsersListJson: String = "" // Comma-separated list of users who upvoted/downvoted
)

@Entity(tableName = "issue_comments")
data class IssueComment(
    @PrimaryKey(autoGenerate = true) val commentId: Int = 0,
    val issueId: Int,
    val author: String,
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "user_profiles")
data class UserProfile(
    @PrimaryKey val userId: String = "current_user",
    val name: String = "Citizen Hero",
    val level: Int = 1,
    val points: Int = 50,
    val totalReports: Int = 0,
    val resolvedCount: Int = 0,
    val verifiedCount: Int = 0,
    
    // User Verification flow
    val isEmailVerified: Boolean = false,
    val isPhoneVerified: Boolean = false,
    val emailAddress: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val profilePicture: String = "avatar_1",
    val signedOffIssueIds: String = "",
    val validatedIssueIds: String = ""
)
