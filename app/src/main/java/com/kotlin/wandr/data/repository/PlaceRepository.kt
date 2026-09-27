package com.kotlin.wandr.data.repository

import com.kotlin.wandr.core.error.safeCall
import com.kotlin.wandr.core.strategy.FetchPolicy
import com.kotlin.wandr.core.strategy.Resource
import com.kotlin.wandr.core.strategy.StrategySelector
import com.kotlin.wandr.data.local.dao.PlaceDao
import com.kotlin.wandr.data.mapper.toDomain
import com.kotlin.wandr.data.mapper.toEntity
import com.kotlin.wandr.data.remote.datasource.LocationRemoteDataSource
import com.kotlin.wandr.data.remote.datasource.PlaceRemoteDataSource
import com.kotlin.wandr.domain.model.FriendOnMap
import com.kotlin.wandr.domain.model.GeoPoint
import com.kotlin.wandr.domain.model.Place
import com.kotlin.wandr.domain.model.PlaceCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface PlaceRepository {
    /** Places around [location], closest first (Map). [category] null = all. */
    fun nearbyPlaces(
        location: GeoPoint,
        radiusKm: Double = QuestRepository.DEFAULT_RADIUS_KM,
        category: PlaceCategory? = null,
        policy: FetchPolicy = FetchPolicy.NETWORK_FIRST,
    ): Flow<Resource<List<Place>>>
}

@Singleton
class PlaceRepositoryImpl @Inject constructor(
    private val remote: PlaceRemoteDataSource,
    private val dao: PlaceDao,
    private val strategies: StrategySelector,
) : PlaceRepository {

    override fun nearbyPlaces(location: GeoPoint, radiusKm: Double, category: PlaceCategory?, policy: FetchPolicy) =
        strategies.select(policy).fetch(
            local = dao.observeNearby(radiusKm, category?.apiValue).map { rows -> rows.map { it.toDomain() } },
            refresh = {
                val places = remote.nearbyPlaces(location.latitude, location.longitude, radiusKm, category?.apiValue)
                dao.replaceNearby(places.map { it.toEntity() })
            },
        )
}

/** Friends on Quest. Always live data, so it is not cached. */
interface LocationRepository {
    /** Saves the user's position. [isBroadcasting] = friends can see it. */
    suspend fun shareMyLocation(location: GeoPoint, isBroadcasting: Boolean): Result<Unit>

    suspend fun friendsOnMap(): Result<List<FriendOnMap>>
}

@Singleton
class LocationRepositoryImpl @Inject constructor(
    private val remote: LocationRemoteDataSource,
) : LocationRepository {

    override suspend fun shareMyLocation(location: GeoPoint, isBroadcasting: Boolean) = safeCall {
        remote.upsertMyLocation(location.latitude, location.longitude, isBroadcasting)
    }

    override suspend fun friendsOnMap() = safeCall {
        remote.friendsOnMap().map { it.toDomain() }
    }
}
