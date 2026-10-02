package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.QuestDropoffDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

/** Facade over the analytics RPCs. */
@Singleton
class AnalyticsRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    /** BQ8: `POST /rest/v1/rpc/get_quest_dropoff`. No parameters; counts every user's abandons. */
    suspend fun questDropoff(): List<QuestDropoffDto> =
        supabase.postgrest.rpc("get_quest_dropoff").decodeList()
}
