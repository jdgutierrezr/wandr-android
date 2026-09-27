package com.kotlin.wandr.data.mapper

import com.kotlin.wandr.data.remote.dto.FriendshipDto
import com.kotlin.wandr.data.remote.dto.NotificationDto
import com.kotlin.wandr.domain.model.Friendship
import com.kotlin.wandr.domain.model.FriendshipStatus
import com.kotlin.wandr.domain.model.Notification
import com.kotlin.wandr.domain.model.NotificationType
import com.kotlin.wandr.domain.model.UserSummary

/**
 * A friendship row has two users. [myId] decides who "the other user" is and whether
 * the request is incoming (only the receiver, `user_id_2`, can accept it).
 */
fun FriendshipDto.toDomain(myId: String): Friendship {
    val isIncoming = userId2 == myId
    val other = if (isIncoming) requester else receiver
    val otherId = if (isIncoming) userId1 else userId2
    return Friendship(
        id = id,
        otherUser = other?.toDomain() ?: UserSummary(id = otherId, name = "Unknown", profilePicture = null),
        status = FriendshipStatus.fromApi(status),
        isIncoming = isIncoming,
        createdAt = createdAt.toInstant(),
    )
}

fun NotificationDto.toDomain() = Notification(
    id = id,
    type = NotificationType.fromApi(type),
    content = content,
    isRead = readStatus,
    createdAt = createdAt.toInstant(),
)
