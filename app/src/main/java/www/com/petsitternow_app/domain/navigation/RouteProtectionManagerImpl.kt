package www.com.petsitternow_app.domain.navigation

import www.com.petsitternow_app.domain.repository.AuthRepository
import www.com.petsitternow_app.domain.repository.FeatureFlagRepository
import javax.inject.Inject

class RouteProtectionManagerImpl @Inject constructor(
    private val authRepository: AuthRepository,
    private val featureFlagRepository: FeatureFlagRepository
) : RouteProtectionManager {

    override suspend fun protectOwnerRoute(): RouteProtectionResult {
        if (!authRepository.isUserAuthenticated()) {
            return RouteProtectionResult.NotAuthenticated
        }
        if (!featureFlagRepository.isOwnerPathEnabled()) {
            return RouteProtectionResult.FeatureDisabled
        }
        val userType = authRepository.getUserType()
        if (userType != null && userType != "owner") {
            return RouteProtectionResult.WrongRole
        }
        return RouteProtectionResult.Allowed
    }

    override suspend fun protectPetsitterRoute(): RouteProtectionResult {
        if (!authRepository.isUserAuthenticated()) {
            return RouteProtectionResult.NotAuthenticated
        }
        if (!featureFlagRepository.isPetsitterPathEnabled()) {
            return RouteProtectionResult.FeatureDisabled
        }
        val userType = authRepository.getUserType()
        if (userType != null && userType != "petsitter") {
            return RouteProtectionResult.WrongRole
        }
        return RouteProtectionResult.Allowed
    }
}
