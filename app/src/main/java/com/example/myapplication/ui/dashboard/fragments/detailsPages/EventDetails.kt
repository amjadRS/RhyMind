package com.example.myapplication.ui.dashboard.fragments.detailsPages

import android.animation.ValueAnimator
import android.app.AlarmManager
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.DatePicker
import android.widget.ImageButton
import android.widget.TimePicker
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.lifecycleScope
import com.example.myapplication.R
import com.example.myapplication.databinding.ActivityEventDetailsBinding
import com.example.myapplication.notification.AlarmReceiver
import com.example.myapplication.ui.dashboard.fragments.GoogleMapsFragment
import com.example.myapplication.ui.models.ReminderModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.Locale

class EventDetails : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailsBinding
    private var isEditMode = false
    private var eventId :String = ""
    private lateinit var reminder: ReminderModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize the ViewBinding
        binding = ActivityEventDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Fragment result listener for location updates
        supportFragmentManager.setFragmentResultListener(
            "locationRequestKey",
            this
        ) { _, result ->
            val location = result.getString("location")
            binding.etEventLocation.setText(location)

            binding.fragmentContainer.visibility = View.GONE
        }

        binding.btnReturn.setOnClickListener {
            finish()
        }


        reminder = intent.getParcelableExtra<ReminderModel>("reminder")!!

        reminder.let {
            eventId = it.eventId
            binding.etEventTitle.setText(it.eventTitle)
            binding.etEventDescription.setText(it.eventDescription)
            binding.etEventCategory.setText(it.eventCategory)
            binding.etEventDateTime.setText("${it.eventDate} : ${it.eventStartTime} - ${it.eventEndTime}")
            binding.etEventDate.setText("${it.eventDate}")
            binding.etEventStartTime.setText("${it.eventStartTime}")
            binding.etEventEndTime.setText("${it.eventEndTime}")
            binding.etEventLocation.setText(it.eventLocation ?: "No location added")
            binding.etEventPriority.setText(it.priority)
            binding.tvEventWeather.text = it.weather ?: "N/A"
            binding.tvEventCreationTime.text = "Created on: ${it.creationTime?.toDate() ?: "Unknown"}"
        }

        binding.imageButtonId.setOnClickListener {
            if (isEditMode) {
                showConfirmationDialog()
            } else {
                // Enter edit mode
                isEditMode = true
                animateButtonTransition(
                    R.drawable.transition_button_layout, // Replace with your new background drawable
                    R.drawable.confirm_icon, // Replace with your new icon drawable
                    true // Enable editing
                )
                enableEditing(true) // Enable fields for editing
            }
        }

        binding.btnChangeDate.setOnClickListener{
            val calendar = Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                    // Set the selected date in the format YYYY-MM-DD
                    val selectedDate = "$year-${month + 1}-$dayOfMonth"
                    binding.etEventDate.setText(selectedDate)
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.show()

        }

        binding.btnChangeStartTime.setOnClickListener{
            val calendar = Calendar.getInstance()
            val timePickerDialog = TimePickerDialog(
                this,
                { _: TimePicker, hourOfDay: Int, minute: Int ->
                    // Specify Locale.US to avoid issues with locale-specific formatting
                    val formattedTime = String.format(
                        Locale.US, "%02d:%02d %s",
                        if (hourOfDay > 12) hourOfDay - 12 else hourOfDay,
                        minute,
                        if (hourOfDay >= 12) "PM" else "AM"
                    )
                    binding.etEventStartTime.setText(formattedTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false // false for 12-hour format
            )
            timePickerDialog.show()
        }

        binding.btnChangeEndTime.setOnClickListener{
            val calendar = Calendar.getInstance()
            val timePickerDialog = TimePickerDialog(
                this,
                { _: TimePicker, hourOfDay: Int, minute: Int ->
                    // Specify Locale.US to avoid issues with locale-specific formatting
                    val formattedTime = String.format(
                        Locale.US, "%02d:%02d %s",
                        if (hourOfDay > 12) hourOfDay - 12 else hourOfDay,
                        minute,
                        if (hourOfDay >= 12) "PM" else "AM"
                    )
                    binding.etEventEndTime.setText(formattedTime)
                },
                calendar.get(Calendar.HOUR_OF_DAY),
                calendar.get(Calendar.MINUTE),
                false
            )
            timePickerDialog.show()
        }

        binding.changeLocation.setOnClickListener {
            if (isEditMode) {
                Log.d("eventLocationListener:","clicked")
                val fragment = GoogleMapsFragment().apply {
                    arguments = Bundle().apply {
                        putString("source", "activity")
                    }
                }
                binding.fragmentContainer.visibility= View.VISIBLE
                supportFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, fragment)
                    .addToBackStack(null)
                    .commit()
            }
        }
    }


    private fun enableEditing(enable: Boolean) {
        Log.d("method:","method is starting")
        binding.etEventTitle.isEnabled = enable
        binding.etEventDescription.isEnabled = enable
        binding.etEventCategory.isEnabled = enable
        binding.etEventDateTime.isEnabled = enable

        binding.etEventPriority.isEnabled = enable
        binding.changeLocation.visibility = View.VISIBLE

        binding.tilEventDateTime.visibility = View.INVISIBLE
        binding.editableDateTimeLayout.visibility = View.VISIBLE
        binding.btnChangeDate.visibility = View.VISIBLE
        binding.btnChangeStartTime.visibility = View.VISIBLE
        binding.btnChangeEndTime.visibility = View.VISIBLE

        if(!enable){
            binding.changeLocation.visibility = View.INVISIBLE
            binding.editableDateTimeLayout.visibility = View.INVISIBLE
            binding.btnChangeDate.visibility = View.INVISIBLE
            binding.btnChangeStartTime.visibility = View.INVISIBLE
            binding.btnChangeEndTime.visibility = View.INVISIBLE

            binding.tilEventDateTime.visibility = View.VISIBLE
        }

    }

    private fun showConfirmationDialog() {
        AlertDialog.Builder(this).apply {
            setTitle("Confirm Changes")
            setMessage("Are you sure you want to save changes?")
            setPositiveButton("Yes") { _, _ ->
                val date = binding.etEventDate.getText()
                val startTime = binding.etEventStartTime.getText()
                val endTime = binding.etEventEndTime.getText()
                binding.etEventDateTime.setText("$date : $startTime - $endTime")
                isEditMode = false
                enableEditing(isEditMode)
                animateButtonTransition(
                    R.drawable.custom_circle_design,
                    R.drawable.edit_icon,
                    false
                )
                updateEvent()
            }
            setNegativeButton("No", null)
            show()
        }
    }

    private fun updateEvent() {
        val db = FirebaseFirestore.getInstance()

        if (eventId.isNotEmpty()) {
            // Create a map with only the fields to update
            val updates = hashMapOf<String, Any>()

            // Add updated fields if they are not empty
            binding.etEventTitle.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
                updates["eventTitle"] = it
                reminder.eventTitle = it
            }
            binding.etEventDescription.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
                updates["eventDescription"] = it
                reminder.eventDescription = it
            }
            binding.etEventCategory.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
                updates["eventCategory"] = it
                reminder.eventCategory = it
            }
            binding.etEventDate.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
                updates["eventDate"] = it
                reminder.eventDate = it
            }
            binding.etEventStartTime.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
                updates["eventStartTime"] = it
                reminder.eventStartTime = it
            }
            binding.etEventEndTime.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
                updates["eventEndTime"] = it
                reminder.eventEndTime = it
            }
            binding.etEventLocation.text?.toString()?.takeIf { it.isNotEmpty() && it.firstOrNull()?.isDigit() == true}?.let {
                updates["eventLocation"] = it
                reminder.eventLocation = it
            }
            binding.etEventPriority.text?.toString()?.takeIf { it.isNotEmpty() }?.let {
                updates["priority"] = it
                reminder.priority = it
            }

            if (updates.isNotEmpty()) {
                lifecycleScope.launch {
                    try {
                        db.collection("events") // Replace with your collection name
                            .document(eventId)
                            .update(updates)
                            .addOnSuccessListener {
                                Toast.makeText(
                                    this@EventDetails,
                                    "Event updated successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                updateAlarm(reminder)
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(
                                    this@EventDetails,
                                    "Error updating fields: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                    } catch (e: Exception) {
                        Toast.makeText(
                            this@EventDetails,
                            "Unexpected error: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            } else {
                Toast.makeText(this, "No fields to update", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Document ID is null, cannot update fields", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAlarm(reminder: ReminderModel) {
        val eventDate = reminder.eventDate
        val startTime = reminder.eventStartTime

        try {
            val (startHour, startMinute) = parseTime(startTime)
            val dateParts = eventDate.split("-")
            val year = dateParts[0].toInt()
            val month = dateParts[1].toInt() - 1 // Calendar month is 0-based
            val day = dateParts[2].toInt()


            val calendar = Calendar.getInstance().apply {
                set(year, month, day, startHour, startMinute)
            }


            val alarmManager = applicationContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager


            if (alarmManager.canScheduleExactAlarms()) {

                val intent = Intent(this, AlarmReceiver::class.java).apply {
                    putExtra("notificationId", reminder.eventId)
                    putExtra("location", reminder.eventLocation)
                    putExtra("channelName", "Event")
                    putExtra("time", reminder.eventStartTime)
                }

                // Use PendingIntent to trigger the AlarmReceiver
                val pendingIntent = PendingIntent.getBroadcast(
                    this,
                    reminder.eventId.hashCode(), // Use a unique request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                // Set the alarm
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )

                Log.d(
                    "Reminder",
                    "Alarm updated date: $year/${month + 1}/$day at ${reminder.eventStartTime} with ID ${reminder.eventId}"
                )
            } else {
                // Permission not granted
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                startActivity(intent)
                Log.e("Reminder", "Permission to schedule exact alarms not granted")
            }
        } catch (e: Exception) {
            Log.e("Reminder", "Error parsing date/time: ${e.message}")
        }
    }

    private fun parseTime(time: String): Pair<Int, Int> {
        val timeRegex = "(\\d{1,2}):(\\d{2})\\s?(AM|PM)".toRegex()
        val matchResult = timeRegex.matchEntire(time)

        if (matchResult != null) {
            val (hourString, minuteString, amPm) = matchResult.destructured
            var hour = hourString.toInt()
            val minute = minuteString.toInt()

            // Adjust hour based on AM/PM
            if (amPm == "AM" && hour == 12) hour = 0
            if (amPm == "PM" && hour != 12) hour += 12

            return Pair(hour, minute)
        } else {
            throw IllegalArgumentException("Invalid time format")
        }
    }

    private fun animateButtonTransition(
        newBackgroundRes: Int,
        newIconBackgroundRes: Int, // Background for the ImageButton
        isEnabled: Boolean
    ) {
        // Start the background color animation for the main button
        val currentDrawable = binding.editButton.background
        val newDrawable = ResourcesCompat.getDrawable(resources, newBackgroundRes, theme)

        if (newDrawable is ColorDrawable) {
            val colorFrom = (currentDrawable as? ColorDrawable)?.color ?: Color.TRANSPARENT
            val colorTo = newDrawable.color

            // Animate background color change
            ValueAnimator.ofArgb(colorFrom, colorTo).apply {
                duration = 300 // Duration in milliseconds
                addUpdateListener { animator ->
                    binding.editButton.setBackgroundColor(animator.animatedValue as Int)
                }
                start()
            }
        }else {
            // If the new background is a shape drawable (non-color), apply it directly
            binding.editButton.setBackgroundResource(newBackgroundRes)
        }

        // Change the ImageButton background immediately (no delay)
        val imageButton = binding.editButton.findViewById<ImageButton>(R.id.imageButtonId)
        imageButton.setBackgroundResource(newIconBackgroundRes)
    }
}
