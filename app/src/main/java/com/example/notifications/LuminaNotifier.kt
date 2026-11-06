package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

/**
 * Notifications locales : rappels d'enregistrement, confirmations de virement
 * et alertes de dépassement de plafond. Aucun service distant n'est utilisé.
 */
object LuminaNotifier {

    const val CHANNEL_ID = "lumina_activite"
    private const val CHANNEL_NAME = "Activité financière"
    private const val CHANNEL_DESCRIPTION =
        "Confirmations d'opérations, virements et alertes de budget"
    const val SYNC_CHANNEL_ID = "lumina_synchronisation"

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = CHANNEL_DESCRIPTION
            enableVibration(true)
        }
        manager.createNotificationChannel(channel)
    }

    private fun ensureSyncChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (manager.getNotificationChannel(SYNC_CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(SYNC_CHANNEL_ID, "Synchronisation des sauvegardes", NotificationManager.IMPORTANCE_LOW).apply {
                description = "Progression des transferts de sauvegarde cloud"
                setShowBadge(false)
            }
        )
    }

    fun notifySyncProgress(context: Context, workspaceId: String, percent: Int, message: String, finished: Boolean = false) {
        ensureSyncChannel(context)
        if (!canPost(context)) return
        val notificationId = (workspaceId.hashCode() and 0x3fffffff) or 0x40000000
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val percentSafe = percent.coerceIn(0, 100)
        val title = if (finished) "Sauvegarde synchronisée" else "Synchronisation cloud · $percentSafe %"
        val notification = NotificationCompat.Builder(context, SYNC_CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setProgress(if (finished) 0 else 100, if (finished) 0 else percentSafe, false)
            .setOngoing(!finished)
            .setAutoCancel(finished)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(pending)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(notificationId, notification) }
    }

    fun cancelSyncProgress(context: Context, workspaceId: String) {
        val notificationId = (workspaceId.hashCode() and 0x3fffffff) or 0x40000000
        NotificationManagerCompat.from(context).cancel(notificationId)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    fun notify(context: Context, title: String, body: String, id: Int = System.currentTimeMillis().toInt()) {
        NotificationInboxStore.add(context, title, body)
        ensureChannel(context)
        if (!canPost(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        runCatching {
            NotificationManagerCompat.from(context).notify(id, notification)
        }
    }
}
