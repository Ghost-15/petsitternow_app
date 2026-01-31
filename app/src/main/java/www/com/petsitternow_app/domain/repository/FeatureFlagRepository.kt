package www.com.petsitternow_app.domain.repository

interface FeatureFlagRepository {
    suspend fun isOwnerPathEnabled(): Boolean
    suspend fun isPetsitterPathEnabled(): Boolean
    suspend fun fetchAndActivate()
}
