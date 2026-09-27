package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.event.AppEvent
import com.kotlin.wandr.core.event.AppEventBus
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.remote.datasource.FriendRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.NotificationRemoteDataSource
import com.kotlin.wandr.domain.model.Friendship
import com.kotlin.wandr.domain.model.Notification
import com.kotlin.wandr.domain.model.UserSummary
import javax.inject.Inject
import javax.inject.Singleton

/** Friends. Always live data, so it is not cached. */
interface FriendRepository {
    /** Friends, requests received and requests sent. */
    suspend fun friendships(): Result<List<Friendship>>
    suspend fun searchUsers(query: String): Result<List<UserSummary>>
    suspend fun sendRequest(userId: String): Result<Unit>

    /** Only works for requests the user received ([Friendship.canAccept]). */
    suspend fun accept(friendshipId: String): Result<Unit>
    suspend fun block(friendshipId: String): Result<Unit>

    /** Removes a friend, or declines / cancels a request. */
    suspend fun remove(friendshipId: String): Result<Unit>
}

@Singleton
class FriendRepositoryImpl @Inject constructor(
    private val remote: FriendRemoteDataSource,
    private val eventBus: AppEventBus,
) : FriendRepository {

    override suspend fun friendships() = safeCall {
        val myId = remote.userId()
        remote.friendships().map { it.toDomain(myId) }
    }

    override suspend fun searchUsers(query: String) = safeCall {
        if (query.isBlank()) emptyList() else remote.searchUsers(query.trim()).map { it.toDomain() }
    }

    override suspend fun sendRequest(userId: String) = change { remote.sendRequest(userId) }

    override suspend fun accept(friendshipId: String) = change { remote.accept(friendshipId) }

    override suspend fun block(friendshipId: String) = change { remote.block(friendshipId) }

    override suspend fun remove(friendshipId: String) = change { remote.remove(friendshipId) }

    private suspend fun change(block: suspend () -> Any) = safeCall {
        block()
        eventBus.publish(AppEvent.FriendshipsChanged)
    }
}

/** In-app notifications. Always live data, so it is not cached. */
interface NotificationRepository {
    /** Newest first. */
    suspend fun notifications(): Result<List<Notification>>
    suspend fun markAsRead(notificationId: String): Result<Unit>
}

@Singleton
class NotificationRepositoryImpl @Inject constructor(
    private val remote: NotificationRemoteDataSource,
) : NotificationRepository {

    override suspend fun notifications() = safeCall { remote.notifications().map { it.toDomain() } }

    override suspend fun markAsRead(notificationId: String) = safeCall { remote.markAsRead(notificationId) }
}
