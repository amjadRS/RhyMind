package com.example.myapplication.ui.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.R

class CalendarAdapter(
    var calendarDays: List<Pair<String, String>>,
    private val onDaySelected: (String) -> Unit
) : RecyclerView.Adapter<CalendarAdapter.CalendarViewHolder>() {

    private var selectedDay: String? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CalendarViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.calendar_day_item, parent, false)
        return CalendarViewHolder(view)
    }

    override fun onBindViewHolder(holder: CalendarViewHolder, position: Int) {
        val (day, weekday) = calendarDays[position]
        holder.bind(day, weekday, day == selectedDay)
    }

    override fun getItemCount(): Int = calendarDays.size

    fun setSelectedDay(day: String) {
        selectedDay = day
        notifyDataSetChanged()
    }

    fun updateDays(days: List<Pair<String, String>>) {
        calendarDays = days
        notifyDataSetChanged()
    }


    inner class CalendarViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val dayTextView: TextView = itemView.findViewById(R.id.dayTextView)
        private val weekdayTextView: TextView = itemView.findViewById(R.id.weekdayTextView)
        private val cardView: CardView = itemView.findViewById(R.id.calendarCardView)

        fun bind(day: String, weekday: String, isSelected: Boolean) {
            dayTextView.text = day
            weekdayTextView.text = weekday
            if (isSelected) {
                cardView.setCardBackgroundColor(Color.parseColor("#FD6B22"))
                dayTextView.setTextColor(Color.WHITE)
                weekdayTextView.setTextColor(Color.WHITE)
            } else {
                cardView.setCardBackgroundColor(Color.WHITE)
                dayTextView.setTextColor(Color.BLACK)
                weekdayTextView.setTextColor(Color.BLACK)
            }
            itemView.setOnClickListener {
                onDaySelected(day)
                setSelectedDay(day)
            }
        }
    }
}

