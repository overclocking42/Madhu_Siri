package com.example.madhu_siri.service

import com.example.madhu_siri.utils.NotificationHelper
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class MyFirebaseMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        remoteMessage.notification?.let {
            NotificationHelper.showNotification(
                applicationContext,
                it.title ?: "Spray Alert",
                it.body ?: "Spraying near your hive"
            )
        } ?: run {
            // Handle data payload if notification is null
            val title = remoteMessage.data["title"] ?: "Spray Alert"
            val body = remoteMessage.data["body"] ?: "Spraying near your hive"
            NotificationHelper.showNotification(applicationContext, title, body)
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // In a full implementation, you'd send this token to your server/Firestore
    }
}
