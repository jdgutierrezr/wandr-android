package com.kotlin.wandr.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * `friendships?select=*,requester:users!friendships_user_id_1_fkey(id,name,profile_picture),
 *  receiver:users!friendships_user_id_2_fkey(id,name,profile_picture)`
 */
@Serializable
data class FriendshipDto(
    val id: String,
    @SerialName("user_id_1") val userId1: String,
    @SerialName("user_id_2") val userId2: String,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    val requester: UserSummaryDto? = null,
    val receiver: UserSummaryDto? = null,
)

/** Row of `notifications`. */
@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val content: String? = null,
    @SerialName("read_status") val readStatus: Boolean = false,
    @SerialName("created_at") val createdAt: String,
)
