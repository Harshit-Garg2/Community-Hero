package com.example.data

import kotlinx.coroutines.flow.Flow

class IssueRepository(private val issueDao: IssueDao) {
    val allIssues: Flow<List<CommunityIssue>> = issueDao.getAllIssues()
    val userProfile: Flow<UserProfile?> = issueDao.getUserProfileFlow()

    fun getIssueById(id: Int): Flow<CommunityIssue?> = issueDao.getIssueById(id)
    suspend fun getIssueByIdSuspend(id: Int): CommunityIssue? = issueDao.getIssueByIdSuspend(id)

    suspend fun insertIssue(issue: CommunityIssue): Long = issueDao.insertIssue(issue)
    suspend fun updateIssue(issue: CommunityIssue) = issueDao.updateIssue(issue)

    fun getCommentsForIssue(issueId: Int): Flow<List<IssueComment>> = issueDao.getCommentsForIssue(issueId)
    suspend fun insertComment(comment: IssueComment) = issueDao.insertComment(comment)

    suspend fun getUserProfileSuspend(): UserProfile? = issueDao.getUserProfileSuspend()
    suspend fun insertUserProfile(profile: UserProfile) = issueDao.insertUserProfile(profile)
}
