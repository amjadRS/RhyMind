package com.example.myapplication.ui.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.SearchCardViewBinding
import com.example.myapplication.ui.models.ReminderModel

class EventSearchAdapter : RecyclerView.Adapter<EventSearchAdapter.SearchViewHolder>() {

    // Using a mutable list for internal list management
    private var eventList = mutableListOf<ReminderModel>()

    // ViewHolder now uses View Binding
    class SearchViewHolder(private val binding: SearchCardViewBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(reminder: ReminderModel) {
            binding.searchEventTitle.text = reminder.eventTitle
            binding.searchEventCategory.text = reminder.eventCategory
            binding.searchEventDate.text = reminder.eventDate
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchViewHolder {
        val binding = SearchCardViewBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return SearchViewHolder(binding)
    }

    override fun onBindViewHolder(holder: SearchViewHolder, position: Int) {
        holder.bind(eventList[position])
    }

    override fun getItemCount(): Int = eventList.size

    // Update the list with new data and notify the adapter
    fun updateList(newList: List<ReminderModel>) {
        eventList.clear()
        eventList.addAll(newList)
        notifyDataSetChanged() // You could optimize this if needed
    }
}