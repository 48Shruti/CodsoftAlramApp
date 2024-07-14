package com.shruti.codsoftalramapp

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.getSystemService
import com.google.firebase.firestore.FirebaseFirestore


class DismissReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getStringExtra("alarmId")
        Log.d("DismissReceiver", "Dismiss received for alarm ID: $alarmId")

        // Stop the alarm sound


        // Update Firebase to mark alarm as off
        val firestore = FirebaseFirestore.getInstance()

        AlarmReceiver.ringtone?.stop()
        AlarmReceiver.ringtone = null

        if (alarmId != null) {
            firestore.collection("alarm").document(alarmId).update("isAlarmOn", false)
                .addOnSuccessListener {

                    Log.d("DismissReceiver", "Alarm status updated to false")
                }
                .addOnFailureListener { e ->
                    Log.w("DismissReceiver", "Error updating alarm status",e)
                }
        }

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.cancel(alarmId.hashCode())

    }
}