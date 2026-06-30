package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface IssueDao {
    @Query("SELECT * FROM community_issues ORDER BY timestamp DESC")
    fun getAllIssues(): Flow<List<CommunityIssue>>

    @Query("SELECT * FROM community_issues WHERE id = :id")
    fun getIssueById(id: Int): Flow<CommunityIssue?>

    @Query("SELECT * FROM community_issues WHERE id = :id")
    suspend fun getIssueByIdSuspend(id: Int): CommunityIssue?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIssue(issue: CommunityIssue): Long

    @Update
    suspend fun updateIssue(issue: CommunityIssue)

    @Query("SELECT * FROM issue_comments WHERE issueId = :issueId ORDER BY timestamp ASC")
    fun getCommentsForIssue(issueId: Int): Flow<List<IssueComment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: IssueComment)

    @Query("SELECT * FROM user_profiles WHERE userId = 'current_user'")
    fun getUserProfileFlow(): Flow<UserProfile?>

    @Query("SELECT * FROM user_profiles WHERE userId = 'current_user'")
    suspend fun getUserProfileSuspend(): UserProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUserProfile(profile: UserProfile)
}
