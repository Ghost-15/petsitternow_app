package www.com.petsitternow_app.domain.repository

import kotlinx.coroutines.flow.Flow

/**
 * Repository for FCM token and notification-related operations.
 */
interface NotificationRepository {

    /**
     * Saves the FCM token to Firestore for the current user.
     * Updates users/{uid}.fcmToken for all users (owners get notifications).
     * Updates petsitters_profiles/{uid}.fcmToken when user is petsitter.
     *
     * @param token The FCM device token
     * @return Flow emitting Result success or failure
     */
    fun saveFcmToken(token: String): Flow<Result<Unit>>

    /**
     * Removes the FCM token from Firestore when user logs out.
     *
     * @return Flow emitting Result success or failure
     */
    fun clearFcmToken(): Flow<Result<Unit>>
}
