package com.shruti.codsoftalramapp

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.Ringtone
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.firestore.FirebaseFirestore

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        var ringtone: Ringtone? = null
    }
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarmId")

        if (alarmId != null) {
            val firestore = FirebaseFirestore.getInstance()
            firestore.collection("alarm").document(alarmId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val alarmData = document.toObject(AlramDataClass::class.java)

                        alarmData?.let {
                            // Update isAlarmOn to true
                            it.isAlarmOn = true
                            firestore.collection("alarm").document(alarmId).set(it)
                                .addOnSuccessListener {
                                    Log.d("AlarmReceiver", "Alarm status updated to true")
                                }
                                .addOnFailureListener { e ->
                                    Log.w("AlarmReceiver", "Error updating alarm status", e)
                                }

                            // Notify MainActivity to update RecyclerView
                            val updateIntent = Intent("com.shruti.codsoftalramapp.UPDATE_ALARM_LIST")
                            context.sendBroadcast(updateIntent)

                            // Play alarm sound
                            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                            val ringtone = RingtoneManager.getRingtone(context, alarmSound)
                            if (ringtone != null && !ringtone.isPlaying) {
                                ringtone.play()
                            }

                            // Vibrate
                            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                            vibrator.vibrate(2000)

                            // Display notification
                            showNotification(context, alarmData)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.w("AlarmReceiver", "Error getting alarm document", e)
                }
        }
    }


    private fun showNotification(context: Context, alarmData: AlramDataClass?) {

        alarmData?.let {
            val dismissIntent = Intent(context, DismissReceiver::class.java).apply {
                putExtra("alarmId", it.id)
            }
            val dismissPendingIntent = PendingIntent.getBroadcast(
                context, it.id.hashCode(), dismissIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            val snoozeIntent = Intent(context, SnoozeReceiver::class.java).apply {
                putExtra("alarmId", it.id)
            }
            val snoozePendingIntent = PendingIntent.getBroadcast(
                context, it.id.hashCode(), snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED) {
                // Build the notification
                val notification = NotificationCompat.Builder(context, "ALARM_CHANNEL_ID")
                    .setSmallIcon(R.drawable.baseline_access_alarms_24) // Replace with your app's notification icon
                    .setContentTitle("Alarm for ${it.title}")
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .addAction(
                        R.drawable.baseline_cancel_24,
                        "Dismiss",
                        dismissPendingIntent
                    )
                    .addAction(
                        R.drawable.baseline_snooze_24,
                        "Snooze",
                        snoozePendingIntent
                    )
                    .build()

                val notificationManager = NotificationManagerCompat.from(context)
                notificationManager.notify(it.id.hashCode(), notification)

                // Notify the user
                Toast.makeText(
                    context,
                    "Alarm for ${it.title} is ringing",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}