package www.com.petsitternow_app.domain.navigation

sealed class RouteProtectionResult {
    object Allowed : RouteProtectionResult()
    object NotAuthenticated : RouteProtectionResult()
    object FeatureDisabled : RouteProtectionResult()
    object WrongRole : RouteProtectionResult()
}

interface RouteProtectionManager {
    suspend fun protectOwnerRoute(): RouteProtectionResult
    suspend fun protectPetsitterRoute(): RouteProtectionResult
}
