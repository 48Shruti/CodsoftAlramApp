package com.shruti.codsoftalramapp

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import android.widget.Toast
import java.util.*

class SnoozeReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarmId")
        Log.d("SnoozeReceiver", "Snooze received for alarm ID: $alarmId")

        // Schedule snooze for 5 minutes later
        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            add(Calendar.MINUTE, 5)
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("alarmId", alarmId)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context, alarmId.hashCode(), alarmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Check if the app can schedule exact alarms
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                // Handle the lack of permission by informing the user
                Toast.makeText(context, "Please allow exact alarm scheduling in app settings", Toast.LENGTH_LONG).show()
                return
            }
        }

        // Set the snooze alarm
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
            )
        }

        AlarmReceiver.ringtone?.stop()
        AlarmReceiver.ringtone = null
        // Cancel the current notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.cancel(alarmId.hashCode())


    }
}