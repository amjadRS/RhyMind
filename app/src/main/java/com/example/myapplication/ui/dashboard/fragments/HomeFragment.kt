package com.example.myapplication.ui.dashboard.fragments

import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.myapplication.databinding.FragmentHomeBinding
import com.example.myapplication.notification.AlarmReceiver
import com.example.myapplication.ui.adapters.CalendarAdapter
import com.example.myapplication.ui.adapters.EventAdapter
import com.example.myapplication.ui.dashboard.DashboardActivity
import com.example.myapplication.ui.dashboard.fragments.detailsPages.EventDetails
import com.example.myapplication.ui.models.ReminderModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var calendarAdapter: CalendarAdapter
    private lateinit var eventAdapter: EventAdapter
    private val calendar = Calendar.getInstance()

    private lateinit var selectedDate: String
    private val TAG = "HomeFragment"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        setupRecyclerViews()
        setupListeners()
        updateCalendarDisplay()
        scrollToDay()
        getOnGoingEvents()


        return binding.root
    }



    private fun setupRecyclerViews() {
        calendarAdapter = CalendarAdapter(getDaysInMonth(calendar)) { day -> onDaySelected(day) }
        binding.calendarRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = calendarAdapter
        }

        eventAdapter = EventAdapter(mutableListOf()) { reminder ->
            val intent = Intent(requireContext(), EventDetails::class.java).apply {
                putExtra("reminder", reminder) // Pass the reminder data
            }
            startActivity(intent)
        }

        binding.eventDetailsRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = eventAdapter
        }

        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.RIGHT) {
            override fun onMove(
                recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder, target: RecyclerView.ViewHolder
            ): Boolean {
                return false
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.layoutPosition
                val event = eventAdapter.getEventAtPosition(position)

                // Show the confirmation dialog before deleting
                val builder = android.app.AlertDialog.Builder(requireContext())
                builder.setTitle("Delete Event")
                builder.setMessage("Are you sure you want to delete this event?")
                builder.setPositiveButton("Yes") { dialog, _ ->
                    val db = FirebaseFirestore.getInstance()
                    db.collection("events").document(event.eventId)
                        .delete()
                        .addOnSuccessListener {
                            eventAdapter.removeEventAtPosition(position) // Remove from local list
                            deleteAlarm(event)
                            getOnGoingEvents()
                        }
                        .addOnFailureListener { exception ->
                            Log.e("Firestore", "Failed to delete event", exception)
                        }
                    dialog.dismiss()
                }
                builder.setNegativeButton("No") { dialog, _ ->
                    eventAdapter.notifyItemChanged(position)
                    dialog.dismiss()
                }

                builder.show()
            }
        })
        itemTouchHelper.attachToRecyclerView(binding.eventDetailsRecyclerView)


    }

    private fun deleteAlarm(event: ReminderModel) {
        try {
            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("notificationId", event.eventId)
                putExtra("location", event.eventLocation)
                putExtra("channelName", "Event")
                putExtra("time", event.eventStartTime)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                requireContext(),
                event.eventId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            // Cancel the alarm
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()

            Log.d("Reminder", "Alarm canceled for ID ${event.eventId}")
        } catch (e: Exception) {
            Log.e("Reminder", "Error canceling alarm: ${e.message}")
        }
    }

    private fun scrollToDay() {
        val daysInMonth = getDaysInMonth(calendar)
        val today = calendar.get(Calendar.DAY_OF_MONTH).toString()
        val position = daysInMonth.indexOfFirst { it.first == today }

        if (position != -1) {
            // Get the layout manager for the RecyclerView
            val layoutManager = binding.calendarRecyclerView.layoutManager as LinearLayoutManager

            // Smoothly scroll to the position
            binding.calendarRecyclerView.smoothScrollToPosition(position)

            // Add an offset to ensure it's centered
            layoutManager.scrollToPositionWithOffset(position, (binding.calendarRecyclerView.width / 2) - (layoutManager.findViewByPosition(position)?.width ?: 0) / 2)

            // Set the selected day
            calendarAdapter.setSelectedDay(today)
            onDaySelected(today)
        }
    }


    private fun setupListeners() {
        binding.calendarImageButton.setOnClickListener { showDatePicker() }

        binding.addNewEventButton.setOnClickListener {
            val action = HomeFragmentDirections.navigateToReminderFragment(selectedDate = selectedDate)
            findNavController().navigate(action)
            (requireActivity() as DashboardActivity).updateBottomBarTab(1)
        }
    }

    private fun showDatePicker() {
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                updateCalendarDisplay()

                val daysInMonth = getDaysInMonth(calendar)
                calendarAdapter.updateDays(daysInMonth)

                val selectedDay = dayOfMonth.toString()
                onDaySelected(selectedDay)

                val position = daysInMonth.indexOfFirst { it.first == selectedDay }
                if (position != -1) {
                    binding.calendarRecyclerView.smoothScrollToPosition(position)
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun onDaySelected(day: String) {
        val dateFormat = SimpleDateFormat("EEE, d MMMM yyyy", Locale.getDefault())
        calendar.set(Calendar.DAY_OF_MONTH, day.toInt())
        binding.dayDetailsTextView.text = "You selected ${dateFormat.format(calendar.time)}"

        calendarAdapter.setSelectedDay(day)
        fetchEventsFromFirestore(calendar.time)

        val saveDateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        selectedDate = saveDateFormat.format(calendar.time)
    }

    private fun fetchEventsFromFirestore(date: Date) {

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val db = FirebaseFirestore.getInstance()

            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val formattedDate = dateFormat.format(date)

            db.collection("events")
                .whereEqualTo("creatorId", uid)
                .whereEqualTo("eventDate", formattedDate)
                .get()
                .addOnSuccessListener { documents ->
                    val eventsList = documents.map { doc ->
                        ReminderModel(
                            eventId = doc.id,
                            eventTitle = doc.getString("eventTitle") ?: "Untitled Event",
                            eventDescription = doc.getString("eventDescription"),
                            eventCategory = doc.getString("eventCategory") ?: "General",
                            eventDate = doc.getString("eventDate") ?: formattedDate,
                            eventStartTime = doc.getString("eventStartTime") ?: "",
                            eventEndTime = doc.getString("eventEndTime") ?: "",
                            eventLocation = doc.getString("eventLocation"),
                            priority = doc.getString("priority") ?: "Medium",
                            creationTime = doc.getTimestamp("creationTime"),
                            weather = doc.getString("weather"),
                            status = doc.getString("status") ?: "OnGoing",
                            creatorId = doc.getString("creatorId") ?: uid,
                            groupIds = doc.get("groupId") as? List<String> ?: listOf()
                        )
                    }
                    val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault()) // AM/PM format
                    val sortedEventsList = eventsList.sortedBy { event ->
                        try {
                            val normalizedStartTime = event.eventStartTime
                                .replace("(?i)am".toRegex(), "AM")
                                .replace("(?i)pm".toRegex(), "PM")
                                .let { if (it.contains(" ")) it else it.replace("([0-9])([a-zA-Z])", "$1 $2") }

                            timeFormat.parse(normalizedStartTime)
                        } catch (e: Exception) {
                            null  // In case of invalid time format, we will sort that item last
                        }
                    }
                    eventAdapter.updateEvents(sortedEventsList)

                    if (eventsList.isEmpty()) {
                        binding.eventDetailsRecyclerView.visibility = View.GONE
                        binding.noEventsView.visibility = View.VISIBLE
                        binding.addNewEventButton.visibility = View.VISIBLE  // Ensure button is visible
                    } else {
                        binding.eventDetailsRecyclerView.visibility = View.VISIBLE
                        binding.noEventsView.visibility = View.GONE
                        binding.addNewEventButton.visibility = View.VISIBLE  // Ensure button is visible
                    }

                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()

                }
        } else {
            Log.e("Firestore", "User is not logged in")

        }
    }

    private fun getOnGoingEvents() {

        var eventsCount = 0
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            val uid = currentUser.uid
            val db = FirebaseFirestore.getInstance()
            db.collection("events")
                .whereEqualTo("creatorId", uid)
                .whereEqualTo("status", "OnGoing")
                .get()
                .addOnSuccessListener { documents ->
                    eventsCount = if (documents.isEmpty) 0 else documents.size()


                    val count = String.format("%d", eventsCount)
                    binding.userEventsCardValue.text = count

                }
                .addOnFailureListener { exception ->
                    exception.printStackTrace()

                    Log.e("Firestore", "Failed to fetch ongoing events", exception)
                }
        }
    }


    private fun updateCalendarDisplay() {
        val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        binding.monthYearTextView.text = dateFormat.format(calendar.time)
    }

    private fun getDaysInMonth(calendar: Calendar): List<Pair<String, String>> {
        val daysWithWeekdays = mutableListOf<Pair<String, String>>()
        val tempCalendar = calendar.clone() as Calendar
        tempCalendar.set(Calendar.DAY_OF_MONTH, 1)

        val maxDay = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..maxDay) {
            tempCalendar.set(Calendar.DAY_OF_MONTH, i)
            val dayNumber = i.toString()
            val weekdayName = SimpleDateFormat("EEE", Locale.getDefault()).format(tempCalendar.time)
            daysWithWeekdays.add(Pair(dayNumber, weekdayName))
        }

        return daysWithWeekdays
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null  // Properly clean up binding
    }
}
