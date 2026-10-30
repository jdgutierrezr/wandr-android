package com.kotlin.wandr.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.homePreferencesDataStore: DataStore<Preferences> by
preferencesDataStore(name = "home_preferences")

data class HomePreferences(
    val energyLevel: String = "balanced",
    val radiusKm: Double = 3.0
)

interface HomePreferencesRepository {
    val preferences: Flow<HomePreferences>

    suspend fun saveEnergyLevel(energyLevel: String)

    suspend fun saveRadiusKm(radiusKm: Double)
}

@Singleton
class HomePreferencesRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : HomePreferencesRepository {

    override val preferences: Flow<HomePreferences> =
        context.homePreferencesDataStore.data.map { preferences ->
            HomePreferences(
                energyLevel = preferences[ENERGY_LEVEL] ?: "balanced",
                radiusKm = preferences[RADIUS_KM] ?: 3.0
            )
        }

    override suspend fun saveEnergyLevel(energyLevel: String) {
        context.homePreferencesDataStore.edit { preferences ->
            preferences[ENERGY_LEVEL] = energyLevel
        }
    }

    override suspend fun saveRadiusKm(radiusKm: Double) {
        context.homePreferencesDataStore.edit { preferences ->
            preferences[RADIUS_KM] = radiusKm
        }
    }

    private companion object {
        val ENERGY_LEVEL = stringPreferencesKey("energy_level")
        val RADIUS_KM = doublePreferencesKey("radius_km")
    }
}
