package www.com.petsitternow_app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import www.com.petsitternow_app.R
import www.com.petsitternow_app.ui.dashboard.DashboardActivity

class PetSitterNowMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "FCM token refreshed: ${token.take(20)}...")
        serviceScope.launch {
            saveTokenToFirestore(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.d(TAG, "Message received from: ${message.from}")

        message.notification?.let { notification ->
            showNotification(
                title = notification.title ?: getString(R.string.app_name),
                body = notification.body ?: "",
                data = message.data
            )
        } ?: run {
            message.data.isNotEmpty().let {
                val title = message.data["title"] ?: getString(R.string.app_name)
                val body = message.data["body"] ?: message.data["message"] ?: ""
                showNotification(title = title, body = body, data = message.data)
            }
        }
    }

    private fun showNotification(
        title: String,
        body: String,
        data: Map<String, String>
    ) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.app_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications de promenades et missions"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(this, DashboardActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            data.forEach { (key, value) -> putExtra(key, value) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()

        val notificationId = data["requestId"]?.hashCode()?.and(0x7FFFFFFF) ?: NOTIFICATION_ID
        notificationManager.notify(notificationId, notification)
    }

    private suspend fun saveTokenToFirestore(token: String) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val firestore = FirebaseFirestore.getInstance()

        try {
            firestore.collection(COLLECTION_USERS)
                .document(uid)
                .set(mapOf(FIELD_FCM_TOKEN to token), com.google.firebase.firestore.SetOptions.merge())
                .await()
            Log.d(TAG, "FCM token saved to users/$uid")

            val tokenResult = FirebaseAuth.getInstance().currentUser?.getIdToken(true)?.await()
            val role = tokenResult?.claims?.get("role") as? String
            if (role == "petsitter") {
                firestore.collection(COLLECTION_PETSITTERS_PROFILES)
                    .document(uid)
                    .set(mapOf(FIELD_FCM_TOKEN to token), com.google.firebase.firestore.SetOptions.merge())
                    .await()
                Log.d(TAG, "FCM token saved to petsitters_profiles/$uid")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save FCM token", e)
        }
    }

    companion object {
        private const val TAG = "PetSitterNowFCM"
        private const val CHANNEL_ID = "walk_notifications"
        private const val NOTIFICATION_ID = 1
        private const val COLLECTION_USERS = "users"
        private const val COLLECTION_PETSITTERS_PROFILES = "petsitters_profiles"
        private const val FIELD_FCM_TOKEN = "fcmToken"
    }
}
