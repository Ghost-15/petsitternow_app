package www.com.petsitternow_app.data.repository

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

private const val KEY_OWNER_PATH = "enable_owner_path"
private const val KEY_PETSITTER_PATH = "enable_petsitter_path"

class FeatureFlagRepositoryImpl @Inject constructor(
    private val remoteConfig: FirebaseRemoteConfig
) : FeatureFlagRepository {

    init {
        val settings = FirebaseRemoteConfigSettings.Builder()
            .setMinimumFetchIntervalInSeconds(3600)
            .build()
        remoteConfig.setConfigSettingsAsync(settings)
        remoteConfig.setDefaultsAsync(
            mapOf(
                KEY_OWNER_PATH to true,
                KEY_PETSITTER_PATH to true
            )
        )
    }

    override suspend fun isOwnerPathEnabled(): Boolean {
        return try {
            remoteConfig.getValue(KEY_OWNER_PATH).asBoolean()
        } catch (e: Exception) {
            true
        }
    }

    override suspend fun isPetsitterPathEnabled(): Boolean {
        return try {
            remoteConfig.getValue(KEY_PETSITTER_PATH).asBoolean()
        } catch (e: Exception) {
            true
        }
    }

    override suspend fun fetchAndActivate() {
        try {
            remoteConfig.fetchAndActivate().await()
        } catch (e: Exception) {
            // Use cached/default values on failure
        }
    }
}
