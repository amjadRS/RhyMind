package com.example.myapplication.ui.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R
import com.example.myapplication.ui.models.ReminderModel

class EventAdapter(private val events: MutableList<ReminderModel>,
                   private val onItemClick: (ReminderModel) -> Unit) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.event_cardview, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        if (position >= 0 && position < events.size) {
            val event = events[position]

            holder.bind(event)
            holder.itemView.setOnClickListener {
                onItemClick(event)
            }
        }
    }

    override fun getItemCount(): Int = events.size

    // Function to update events when new events are available
    fun updateEvents(newEvents: List<ReminderModel>) {
        events.clear()
        events.addAll(newEvents)
        notifyDataSetChanged()
    }

    fun getEventAtPosition(position: Int): ReminderModel {
        return events[position]
    }

    // Function to remove an event at a specific position
    fun removeEventAtPosition(position: Int) {
        if (position >= 0 && position < events.size) {
            events.removeAt(position)
            notifyItemRemoved(position)
        }
    }

    inner class EventViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val eventTitle: TextView = itemView.findViewById(R.id.eventTitle)
        val eventDescription: TextView = itemView.findViewById(R.id.eventDescription)
        val eventStartTime: TextView = itemView.findViewById(R.id.eventStartTime)
        val eventEndTime: TextView = itemView.findViewById(R.id.eventEndTime)

        fun bind(event: ReminderModel) {
            eventTitle.text = event.eventTitle
            eventDescription.text = event.eventDescription ?: "No description available"
            eventStartTime.text = event.eventStartTime
            eventEndTime.text = event.eventEndTime
        }
    }
}
