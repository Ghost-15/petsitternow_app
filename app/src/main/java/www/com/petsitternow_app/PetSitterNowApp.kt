package www.com.petsitternow_app

import android.app.Application
import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import www.com.petsitternow_app.domain.repository.NotificationRepository
import javax.inject.Inject

@HiltAndroidApp
class PetSitterNowApp : Application() {

    @Inject
    lateinit var notificationRepository: NotificationRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        requestFcmTokenAndSave()
    }

    private fun requestFcmTokenAndSave() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "FCM token fetch failed", task.exception)
                    return@addOnCompleteListener
                }
                val token = task.result ?: return@addOnCompleteListener
                Log.d(TAG, "FCM token obtained, saving to Firestore")
                appScope.launch(Dispatchers.IO) {
                    notificationRepository.saveFcmToken(token).collect { result ->
                        result.onFailure { e -> Log.e(TAG, "Failed to save FCM token", e) }
                    }
                }
            }
    }

    companion object {
        private const val TAG = "PetSitterNowApp"
    }
}
