package com.kotlin.wandr.data.remote.datasource

import com.kotlin.wandr.data.remote.dto.QuestDropoffDto
import com.kotlin.wandr.data.remote.dto.RecommendationLoadTimeDto
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRemoteDataSource @Inject constructor(
    private val supabase: SupabaseClient,
) {
    suspend fun questDropoff(): List<QuestDropoffDto> =
        supabase.postgrest.rpc("get_quest_dropoff").decodeList()

    suspend fun recommendationLoadTime() =
        supabase.postgrest.rpc("get_recommendation_load_time").decodeList<RecommendationLoadTimeDto>().first()
}
