package com.shruti.codsoftalramapp

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import androidx.recyclerview.widget.RecyclerView

class AlramAdapter(val item : ArrayList<AlramDataClass>,val alramInterface: AlramInterface) : RecyclerView.Adapter<AlramAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title = view.findViewById<TextView>(R.id.tvtitlealarm)
        val date = view.findViewById<TextView>(R.id.tvdate)
        val time = view.findViewById<TextView>(R.id.tvalarmtime)
        val toggleButton = view.findViewById<ToggleButton>(R.id.toggleButton)  // Add ToggleButton reference
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.alarm_layout, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return item.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {


        holder.title.setText(item[position].title)
        holder.time.setText(item[position].time)
        holder.date.setText(item[position].date)
        // Update ToggleButton state based on isAlarmOn field
        holder.toggleButton.isChecked = item[position].isAlarmOn

        // Handle toggle button state change
        holder.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            item[position].isAlarmOn = isChecked
            if (isChecked) {
                alramInterface.setAlarm(item[position],position)
            } else {
                alramInterface.cancelAlarm(item[position],position)
            }
        }

        // Handle click on item view
        holder.itemView.setOnClickListener {
            alramInterface.update(item[position], position)
        }
    }
}