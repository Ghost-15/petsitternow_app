package www.com.petsitternow_app

import android.app.Application
import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.crashlytics.FirebaseCrashlytics
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
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        super.onCreate()
        initCrashlytics()
        requestFcmTokenAndSave()
    }

    /**
     * Initialise Firebase Crashlytics avec les clés personnalisées
     * pour faciliter le diagnostic des crashs en production.
     */
    private fun initCrashlytics() {
        val crashlytics = FirebaseCrashlytics.getInstance()
        crashlytics.setCrashlyticsCollectionEnabled(true)
        crashlytics.setCustomKey("app_version", BuildConfig.VERSION_NAME)
        crashlytics.setCustomKey("build_type", BuildConfig.BUILD_TYPE)
        crashlytics.log("Crashlytics initialized")
        Log.d(TAG, "Firebase Crashlytics initialized")

        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            crashlytics.setCustomKey("crash_thread", thread.name)
            crashlytics.recordException(throwable)
            Log.e(TAG, "Uncaught exception on thread ${thread.name}", throwable)
        }
    }

    private fun requestFcmTokenAndSave() {
        FirebaseMessaging.getInstance().token
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    Log.w(TAG, "FCM token fetch failed", task.exception)
                    FirebaseCrashlytics.getInstance().recordException(
                        task.exception ?: Exception("FCM token fetch failed")
                    )
                    return@addOnCompleteListener
                }
                val token = task.result ?: return@addOnCompleteListener
                Log.d(TAG, "FCM token obtained, saving to Firestore")
                appScope.launch(Dispatchers.IO) {
                    notificationRepository.saveFcmToken(token).collect { result ->
                        result.onFailure { e ->
                            Log.e(TAG, "Failed to save FCM token", e)
                            FirebaseCrashlytics.getInstance().recordException(e)
                        }
                    }
                }
            }
    }

    companion object {
        private const val TAG = "PetSitterNowApp"
    }
}
