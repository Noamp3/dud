package com.smartboiler.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartboiler.R

/**
 * Handles boiler event notifications.
 */
object NotificationHelper {

    const val CHANNEL_ID = "boiler_events"
    private const val CHANNEL_NAME = "Boiler Events"
    private var notificationId = 1000

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Notifications for boiler heating events"
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun notifyBoilerStarted(context: Context, durationMinutes: Int) {
        show(context, "🔥 Boiler Heating", "Heating for $durationMinutes minutes")
    }

    fun notifyBoilerReady(context: Context, tempCelsius: Int) {
        show(context, "✅ Hot Water Ready!", "Estimated temp: ${tempCelsius}°C")
    }

    fun notifyBoilerOff(context: Context) {
        show(context, "❄️ Boiler Off", "Heating complete, boiler turned off")
    }

    fun notifyNoHeatingNeeded(context: Context) {
        show(context, "☀️ No Heating Needed", "Solar energy is sufficient today!")
    }

    fun notifyFeedbackReminder(context: Context) {
        show(context, "🚿 How Was Your Shower?", "Tap to rate and improve future estimates")
    }

    private fun show(context: Context, title: String, text: String) {
        try {
            val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(title)
                .setContentText(text)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true)
                .build()

            NotificationManagerCompat.from(context).notify(notificationId++, notification)
        } catch (_: SecurityException) {
            // Notification permission not granted
        }
    }
}
