package com.shruti.codsoftalramapp

import android.Manifest
import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.firestore.FirebaseFirestore
import com.shruti.codsoftalramapp.databinding.ActivityMainBinding
import com.shruti.codsoftalramapp.databinding.DialogAlarmSetBinding
import java.util.*

class MainActivity : AppCompatActivity(), AlramInterface {
    private lateinit var binding: ActivityMainBinding
    private lateinit var firestore: FirebaseFirestore
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var adapter: AlramAdapter
    private var items = ArrayList<AlramDataClass>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        firestore = FirebaseFirestore.getInstance()
        setupRecyclerView()
        createNotificationChannel()
        requestNotificationPermission()
        fetchCollectionFromFirestore()
        binding.fab.setOnClickListener {
            showAlarmDialog(null, -1)
        }
    }

    private fun setupRecyclerView() {
        adapter = AlramAdapter(items, this)
        binding.recycler.adapter = adapter
        linearLayoutManager = LinearLayoutManager(this)
        binding.recycler.layoutManager = linearLayoutManager
    }
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Alarm Notification Channel"
            val descriptionText = "Channel for Alarm notifications"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel("ALARM_CHANNEL_ID", name, importance).apply {
                description = descriptionText
            }
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    private fun fetchCollectionFromFirestore() {
        items.clear()
        firestore.collection("alarm").addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("MainActivity", "Error fetching Firestore collection", e)
                return@addSnapshotListener
            }
            items.clear() // Clear previous items
            for (document in snapshot!!) {
                val alarmData = document.toObject(AlramDataClass::class.java)
                alarmData.id = document.id
                items.add(alarmData)
            }
            adapter.notifyDataSetChanged() // Notify adapter of dataset change
        }
    }

    private fun showAlarmDialog(alarmData: AlramDataClass?, position: Int) {
        val dialog = Dialog(this)
        val dialogBinding = DialogAlarmSetBinding.inflate(layoutInflater)
        dialog.setContentView(dialogBinding.root)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)

        // Populate dialog fields if editing an existing alarm
        alarmData?.let {
            dialogBinding.ettitle.setText(it.title)
            val timeParts = it.time.split(":")
            if (timeParts.size == 2) {
                dialogBinding.time.hour = timeParts[0].toInt()
                dialogBinding.time.minute = timeParts[1].toInt()
            }
        }

        // Save button click listener
        dialogBinding.btnsave.setOnClickListener {
            val hour = dialogBinding.time.hour
            val minute = dialogBinding.time.minute
            val formattedTime = String.format("%02d:%02d", hour, minute)
            val newAlarm = AlramDataClass(
                id = alarmData?.id,
                title = dialogBinding.ettitle.text.toString(),
                time = formattedTime,
                isAlarmOn = true // Default to true for new alarms
            )

            // Add or update alarm in Firestore
            if (alarmData == null) {
                addAlarmToFirestore(newAlarm)

            } else {
                updateAlarmInFirestore(newAlarm)
            }

            dialog.dismiss()
        }

        // Cancel button click listener
        dialogBinding.btncancel.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }


    private fun addAlarmToFirestore(alarmData: AlramDataClass) {
            firestore.collection("alarm")
                .whereEqualTo("time", alarmData.time)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        items.clear()
                        firestore.collection("alarm").add(alarmData)
                            .addOnSuccessListener { documentReference ->
                                alarmData.id = documentReference.id
                                items.add(alarmData)
                                adapter.notifyDataSetChanged()
                                setAlarm(alarmData, items.size - 1) // Set alarm after adding to Firestore
                                Toast.makeText(this, "Alarm added", Toast.LENGTH_SHORT).show()
                            }
                            .addOnFailureListener { e ->
                                Log.e("MainActivity", "Error adding alarm to Firestore", e)
                                Toast.makeText(this, "Failed to add alarm", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Alarm for this time already exists", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error checking for duplicate alarms", e)
                }
        }


    private fun updateAlarmInFirestore(alarmData: AlramDataClass) {
        alarmData.id?.let { alarmId ->
            firestore.collection("alarm").document(alarmId).set(alarmData)
                .addOnSuccessListener {
                    val position = items.indexOfFirst { it.id == alarmData.id }
                    if (position != -1) {
                        items[position] = alarmData
                        adapter.notifyItemChanged(position)
                        setAlarm(alarmData, position) // Update alarm after updating in Firestore
                        Toast.makeText(this, "Alarm updated", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("MainActivity", "Error updating alarm in Firestore", e)
                    Toast.makeText(this, "Failed to update alarm", Toast.LENGTH_SHORT).show()
                }
        }
    }

    override fun setAlarm(alramDataClass: AlramDataClass, position: Int) {
        val timeParts = alramDataClass.time.split(":")
        if (timeParts.size == 2) {
            val hour = timeParts[0].toInt()
            val minute = timeParts[1].toInt()

            val calendar = Calendar.getInstance().apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
            }

            val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val alarmIntent = Intent(this, AlarmReceiver::class.java).apply {
                putExtra("alarmId", alramDataClass.id)
                putExtra("title", alramDataClass.title)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                this, position, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            // Check for permission to schedule exact alarms on Android 12 (API level 31) and higher
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!alarmManager.canScheduleExactAlarms()) {
                    // Inform the user to allow exact alarm scheduling in app settings
                    Toast.makeText(this, "Please allow exact alarm scheduling in app settings", Toast.LENGTH_LONG).show()
                    return
                }
            }

            // Set the alarm
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP, calendar.timeInMillis, pendingIntent
                )
            }
        }
    }

    override fun cancelAlarm(alramDataClass: AlramDataClass, position: Int) {
        val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, AlarmReceiver::class.java).apply {
            putExtra("alarmId", alramDataClass.id ?: "")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            this, position, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)

        alramDataClass.isAlarmOn = false
        firestore.collection("alarm").document(alramDataClass.id ?: "").set(alramDataClass)
            .addOnSuccessListener {
                adapter.notifyItemChanged(position)
            }
    }

    override fun update(alramDataClass: AlramDataClass, position: Int) {
        showAlarmDialog(alramDataClass, position)
    }



    companion object {
        private const val TAG = "MainActivity"
    }
}