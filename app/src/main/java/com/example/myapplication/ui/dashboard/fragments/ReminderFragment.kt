package com.example.myapplication.ui.dashboard.fragments




import android.Manifest
import android.app.AlarmManager
import java.util.Calendar
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.DatePicker
import android.app.DatePickerDialog
import android.app.PendingIntent
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location

import android.provider.Settings
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat

import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentReminderBinding
import com.example.myapplication.notification.AlarmReceiver
import com.example.myapplication.ui.dashboard.DashboardActivity
import com.example.myapplication.ui.models.GroupModel
import com.example.myapplication.ui.models.ReminderModel
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Locale
import com.example.myapplication.BuildConfig

class ReminderFragment : Fragment() {

    private val args: ReminderFragmentArgs by navArgs<ReminderFragmentArgs>()
    private lateinit var binding: FragmentReminderBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var selectedLocation: String? = null
    private var startTimeValue: Calendar? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var currentLocation: LatLng
    private val apiKey = BuildConfig.MAPS_API_KEY

    private var groups = mutableListOf<GroupModel>()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentReminderBinding.inflate(inflater, container, false)

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())

        checkLocationPermissionAndFetch()
        return binding.root
    }

    private fun checkLocationPermissionAndFetch() {
        val locationPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                fetchCurrentLocation()
            } else {
                Toast.makeText(requireContext(), "Location permission denied", Toast.LENGTH_SHORT).show()
            }
        }

        // Check if permission is already granted
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fetchCurrentLocation()
        } else {
            // Request permission
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun fetchCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationProviderClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    currentLocation = LatLng(location.latitude, location.longitude)
                } else {
                    Toast.makeText(requireContext(), "Unable to fetch location", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(requireContext(), "Error fetching location: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }



    private fun initializeGroups() {

        val groupSelector = binding.groupSelector
        val groupList = resources.getStringArray(R.array.placeholder_array) // Replace with your group data
        val spinnerAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, groupList)
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        groupSelector.adapter = spinnerAdapter

        // Initialize groupMultiSelect MultiAutoCompleteTextView (multiple selection)
        val groupMultiSelect = binding.groupMultiSelect
        val multiAdapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, groupList)
        groupMultiSelect.setAdapter(multiAdapter)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        binding.apply {

            fetchGroups()

            val passedArgsDate = args.selectedDate
            println(passedArgsDate)
            if(passedArgsDate!= null) {
                eventDate.setText(passedArgsDate)
            }
            datePick.setOnClickListener {
                showDatePickDialog()
            }

            eventStartTime.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    startTimePickDialog()
                }
            }

            eventEndTime.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    endTimePickDialog()
                }
            }

            val navController = findNavController()
            eventLocation.setOnClickListener {
                navController.navigate(R.id.googleMapsFragment)
            }


            selectedLocation = args.address
            println(selectedLocation)
            if (selectedLocation != null){
                eventGoeLoc.setText(selectedLocation)
            }

            addEventButton.setOnClickListener {

                if (auth.currentUser != null) {
                    val title = binding.eventTitle.text.toString().trim()
                    val description = binding.eventDescription.text.toString().trim()
                    val category = binding.eventCategory.text.toString().trim()
                    val date = binding.eventDate.text.toString().trim()
                    val startTime = binding.eventStartTime.text.toString().trim()
                    val endTime = binding.eventEndTime.text.toString().trim()
                    val location = selectedLocation

                    if (title.isEmpty()) {
                        binding.eventTitle.error = "Please enter the event title"
                        return@setOnClickListener
                    }

                    if (category.isEmpty()) {
                        binding.eventCategory.error = "Please select a category"
                        return@setOnClickListener
                    }

                    if (date.isEmpty()) {
                        binding.eventDate.error = "Please select a date"
                        return@setOnClickListener
                    }

                    val calendar = Calendar.getInstance()
                    val eventDate = Calendar.getInstance().apply {
                        set(Calendar.YEAR, date.split("-")[0].toInt())
                        set(Calendar.MONTH, date.split("-")[1].toInt() - 1) // Month is 0-indexed
                        set(Calendar.DAY_OF_MONTH, date.split("-")[2].toInt())
                    }

                    if (eventDate.before(calendar)) {
                        binding.eventDate.error = "Please select a valid reminder date"
                        return@setOnClickListener
                    }

                    val currentTime = Calendar.getInstance()


                    val (startHour, startMinute) = parseTime(startTime) // Assuming startTime is in the "hh:mm AM/PM" format

                    val startTimeCalendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, startHour)
                        set(Calendar.MINUTE, startMinute)
                    }

                    val timeDifferenceInMinutes = (startTimeCalendar.timeInMillis - currentTime.timeInMillis) / 60000

                    if (timeDifferenceInMinutes < 1) {
                        binding.eventStartTime.error = "Please select a valid reminder time, at least 1 min over current time"
                        return@setOnClickListener
                    }
                    val groups = groupMultiSelect.text.toString().split(",").map { it.trim() }
                    val eventsRef = db.collection("events").document()
                    val reminder = ReminderModel(
                        eventId = eventsRef.id,
                        eventTitle = title,
                        eventDescription = description,
                        eventCategory = category,
                        eventDate = date,
                        eventStartTime = startTime,
                        eventEndTime = endTime,
                        eventLocation = location,
                        priority = null,
                        creationTime = Timestamp.now(),
                        weather = null,
                        status = "OnGoing",
                        creatorId = auth.currentUser?.uid ?: "",
                        groupIds = groups
                    )
                    saveReminderToFirestore(reminder)
                    Toast.makeText(requireContext(), "Event added successfully", Toast.LENGTH_SHORT).show()
                    (requireActivity() as DashboardActivity).navigate(0)
                } else {
                    Toast.makeText(requireContext(), "Please log in first", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }



    private fun fetchGroups() {
        val currentUserId = auth.currentUser?.uid ?: return

        db.collection("groups")
            .whereEqualTo("creatorId", currentUserId)  // Add query to filter groups by creatorId
            .get()
            .addOnSuccessListener { documents ->
                val fetchedGroups = mutableListOf<GroupModel>()
                for (document in documents) {
                    val group = document.toObject(GroupModel::class.java)
                    fetchedGroups.add(group)
                }
                groups = fetchedGroups
                initializeGroups()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching groups: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun startTimePickDialog() {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%d:%02d %s",
                    if (selectedHour == 0 || selectedHour == 12) 12 else selectedHour % 12,
                    selectedMinute,
                    if (selectedHour < 12) "AM" else "PM"
                )

                val newStartTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                }

                startTimeValue = newStartTime
                binding.eventStartTime.setText(formattedTime)

            },
            hour,
            minute,
            false
        )
        timePickerDialog.setOnCancelListener {
            binding.eventStartTime.clearFocus()
        }

        timePickerDialog.show()
    }

    private fun endTimePickDialog() {
        if (startTimeValue == null) {
            binding.eventStartTime.error = "Please select a starting time"
            return
        }
        else {
            binding.eventStartTime.error = null
        }

        val calendar = startTimeValue
        val hour = calendar?.get(Calendar.HOUR_OF_DAY) ?: 0
        val minute = calendar?.get(Calendar.MINUTE) ?: 0

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _, selectedHour, selectedMinute ->
                val formattedTime = String.format(
                    Locale.getDefault(),
                    "%d:%02d %s",
                    if (selectedHour == 0 || selectedHour == 12) 12 else selectedHour % 12,
                    selectedMinute,
                    if (selectedHour < 12) "AM" else "PM"
                )

                val newEndTime = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, selectedHour)
                    set(Calendar.MINUTE, selectedMinute)
                }

                if (newEndTime.before(startTimeValue)) {
                    binding.eventEndTime.error = "End time must be after start time"
                    return@TimePickerDialog
                } else {
                    binding.eventEndTime.error = null
                    binding.eventEndTime.setText(formattedTime)
                }
            },
            hour,
            minute,
            false
        )

        timePickerDialog.setOnCancelListener {
            binding.eventStartTime.clearFocus()
        }

        timePickerDialog.show()
    }



    private fun showDatePickDialog() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _: DatePicker, year: Int, month: Int, dayOfMonth: Int ->
                val formattedDate = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                binding.eventDate.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }


    private fun saveReminderToFirestore(reminder: ReminderModel) {
        val eventsRef = db.collection("events").document()
        eventsRef.set(reminder.toMap())
            .addOnSuccessListener {
                Log.d("Firestore", "Reminder saved successfully")
                Toast.makeText(requireContext(), "Event added successfully", Toast.LENGTH_SHORT).show()

                setReminder(reminder,currentLocation)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "Error adding event: ${e.message}")
                Toast.makeText(requireContext(), "Error adding event: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun setReminder(reminder: ReminderModel, currentLocation: LatLng) {
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

            val alarmManager = requireContext().getSystemService(Context.ALARM_SERVICE) as AlarmManager

            if (alarmManager.canScheduleExactAlarms()) {
                val destination = reminder.eventLocation?.let { parseLatLng(it) }
                if (destination != null) {
                    CoroutineScope(Dispatchers.IO).launch {
                        val travelData = fetchTravelTime(currentLocation, destination, apiKey)
                        if (travelData != null) {
                            val (travelTime, distance) = travelData

                            val travelTimeInMillis = travelTime * 1000L
                            val reminderTimeInMillis = calendar.timeInMillis - travelTimeInMillis

                            val time = Calendar.getInstance().apply {
                                timeInMillis = reminderTimeInMillis
                            }
                            val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
                            val formattedTime = timeFormat.format(time.time)

                            val intent = Intent(context, AlarmReceiver::class.java).apply {
                                putExtra("notificationId", reminder.eventId)
                                putExtra("location", reminder.eventLocation)
                                putExtra("channelName", "Event")
                                putExtra("time", formattedTime)
                                putExtra("distance", distance)
                            }

                            val pendingIntent = PendingIntent.getBroadcast(
                                requireContext(),
                                reminder.eventId.hashCode(),
                                intent,
                                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                            )

                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                reminderTimeInMillis,
                                pendingIntent
                            )


                            Log.d(
                                "Reminder",
                                "Alarm set for $year/${month + 1}/$day at $formattedTime with ID ${reminder.eventId}"
                            )
                        }
                    }
                }
            } else {
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

fun parseLatLng(location: String): LatLng? {
    return try {
        val parts = location.split(",")
        if (parts.size == 2) {
            val latitude = parts[0].trim().toDouble()
            val longitude = parts[1].trim().toDouble()
            LatLng(latitude, longitude)
        } else {
            null // Return null if the format is incorrect
        }
    } catch (e: Exception) {
        Log.e("LatLng Parsing", "Error parsing location string: $e")
        null
    }
}


    fun fetchTravelTime(currentLocation: LatLng, destination: LatLng, apiKey: String): Pair<Int, Float>? {
        val client = OkHttpClient()
        val url = "https://maps.googleapis.com/maps/api/distancematrix/json?origins=${currentLocation.latitude},${currentLocation.longitude}&destinations=${destination.latitude},${destination.longitude}&key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .build()

        try {
            Log.d("API", "Making API request to fetch travel time... URL: $url")
            val response = client.newCall(request).execute()

            if (response.isSuccessful) {
                val responseData = response.body?.string()
                Log.d("API", "API Response: $responseData")

                // Parse the response
                val jsonObject = responseData?.let { JSONObject(it) }
                val status = jsonObject?.getString("status")
                if (status != "OK") {
                    Log.e("API", "API Request failed with status: $status")
                    return null
                }

                val rows = jsonObject.getJSONArray("rows")
                val elements = rows.getJSONObject(0).getJSONArray("elements")
                val duration = elements.getJSONObject(0).getJSONObject("duration")
                val distance = elements.getJSONObject(0).getJSONObject("distance")

                val travelTime = duration.getInt("value") // In seconds
                val dist = distance.getInt("value") // In meters

                Log.d("API", "Travel Time: $travelTime, Distance: $dist")
                return Pair(travelTime, dist.toFloat())
            } else {
                Log.e("API", "Failed to fetch data: ${response.code}")
                return null
            }
        } catch (e: Exception) {
            Log.e("API", "Error during API call: ${e.message}", e)
            return null
        }
    }
}

