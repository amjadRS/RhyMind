package com.example.myapplication.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.myapplication.R

class AlarmReceiver : BroadcastReceiver() {

    private var currentRingtone: Ringtone? = null

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra("notificationId") ?: return
        val location = intent.getStringExtra("location")
        val channelName = intent.getStringExtra("channelName")?: return
        val time  = intent.getStringExtra("time")?: return


        playAlarmSound(context)
        createNotificationChannel(context, notificationId, channelName)
        location?.let {
            createNotificationWithLocation(context, notificationId, it, time)
        } ?: run {
            createNotification(context, notificationId, time)
        }

    }

    private fun createNotificationWithLocation(context: Context, notificationId: String, location: String, time: String) {

        val gmmIntentUri = Uri.parse("google.navigation:q=$location")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")


        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = Intent(context, AlarmReceiver::class.java)
        dismissIntent.putExtra("notificationId", notificationId)
        dismissIntent.action = "com.example.myapplication.ACTION_DISMISS"

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, notificationId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("RyhMind")
            .setContentText("Event starting soon $time, start navigation")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(
                R.drawable.navigate_24x24_icon,
                "Navigate",
                pendingIntent
            ).setAutoCancel(true)

        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManagerCompat.notify(notificationId.hashCode(), builder.build())
        } else {
            Log.d("AlarmReceiver", "Permission not granted to post notification.")
        }
    }

    private fun playAlarmSound(context: Context) {
        var alarmSound: Uri? = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)

        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }

        currentRingtone = RingtoneManager.getRingtone(context, alarmSound)
        currentRingtone?.play()

        // Stop the alarm sound after 15 seconds
        Handler(Looper.getMainLooper()).postDelayed({
            stopAlarmSound()
        }, 15000) // 15,000 milliseconds = 15 seconds
    }

    private fun stopAlarmSound() {
        if (currentRingtone?.isPlaying == true) {
            currentRingtone?.stop()
        }
        currentRingtone = null
    }

    @SuppressLint("ObsoleteSdkInt")
    private fun createNotificationChannel(context: Context, notificationId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val descriptionText = "You have an event"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(notificationId, channelName, importance).apply {
                description = descriptionText
            }
            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
        }
    }

    private fun createNotification(context: Context, notificationId: String, time:String) {



        val dismissIntent = Intent(context, AlarmReceiver::class.java)
        dismissIntent.putExtra("notificationId", notificationId)
        dismissIntent.action = "com.example.myapplication.ACTION_DISMISS"

        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, notificationId)
            .setSmallIcon(R.drawable.logo)
            .setContentTitle("RyhMind")
            .setContentText("You  have an event starting at $time")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)


        val notificationManagerCompat = NotificationManagerCompat.from(context)
        if (ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            notificationManagerCompat.notify(notificationId.hashCode(), builder.build())
        } else {
            Log.d("AlarmReceiver", "Permission not granted to post notification.")
        }
    }


    private fun cancelNotification(context: Context, notificationId: String) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(notificationId.hashCode())
    }
}