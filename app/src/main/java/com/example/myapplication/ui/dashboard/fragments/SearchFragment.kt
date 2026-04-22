package com.example.myapplication.ui.dashboard.fragments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.SearchView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.databinding.FragmentSearchBinding
import com.example.myapplication.ui.adapters.CategoryAdapter
import com.example.myapplication.ui.adapters.EventSearchAdapter
import com.example.myapplication.ui.models.ReminderModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SearchFragment : Fragment() {

    private lateinit var binding: FragmentSearchBinding
    private lateinit var adapter: EventSearchAdapter
    private lateinit var categoryAdapter: CategoryAdapter  // Added adapter for categories
    private val database = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser?.uid

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentSearchBinding.inflate(
            inflater, container, false
        )

        // Set up the click listener for the "+Create new" TextView
        binding.createNewTextView.setOnClickListener {
            showCreateNewDialog()
        }

        setUpRecyclerViews()
        setUpListeners()

        // Update categories in RecyclerView when the fragment is created
        updateCategoriesInRecyclerView()

        return binding.root
    }

    private fun showCreateNewDialog() {
        // Inflate the custom dialog layout
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.category, null)

        // Create the AlertDialog
        val dialog = android.app.AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .create()

        // Get references to dialog views
        val editTextNewCategoryTitle = dialogView.findViewById<EditText>(R.id.editTextNewCategoryTitle)
        val saveButton = dialogView.findViewById<Button>(R.id.buttonSaveEvent)

        saveButton.setOnClickListener {
            val categoryNameValidationCheck = editTextNewCategoryTitle.text.toString().trim()
            val newCategory = editTextNewCategoryTitle.text.toString()

            if (categoryNameValidationCheck.isEmpty()) {
                Toast.makeText(requireContext(), "Category name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val db = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser?.uid

            if(currentUser != null){
                val docRef = db.collection("users").document(currentUser)
                docRef.get().addOnSuccessListener { document ->
                    if (document.exists()) {
                        @Suppress("UNCHECKED_CAST")
                        val categories = document.get("categories") as? List<String> ?: emptyList()
                        val mutableCategories = categories.toMutableList()

                        if (!mutableCategories.contains(newCategory)) {
                            mutableCategories.add(newCategory)

                            docRef.update("categories", mutableCategories)
                                .addOnSuccessListener {
                                    Log.d("FirestoreSuccess", "Category added successfully")
                                    Toast.makeText(context, "Category added: $newCategory", Toast.LENGTH_SHORT).show()

                                    // Update the RecyclerView after adding a new category
                                    updateCategoriesInRecyclerView()
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FirestoreError", "Error updating categories", e)
                                }

                        } else {
                            Toast.makeText(context, "Category $newCategory already exists", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun updateCategoriesInRecyclerView() {
        if (currentUser != null) {
            val docRef = FirebaseFirestore.getInstance().collection("users").document(currentUser)
            docRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    // Get categories and update the RecyclerView
                    @Suppress("UNCHECKED_CAST")
                    val categories = document.get("categories") as? List<String> ?: emptyList()

                    // Set the adapter to the RecyclerView with the categories
                    categoryAdapter = CategoryAdapter(categories)
                    binding.categoriesRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
                    binding.categoriesRecyclerView.adapter = categoryAdapter
                }
            }.addOnFailureListener { e ->
                Log.e("FirestoreError", "Error fetching categories", e)
            }
        }
    }

    private fun setUpListeners() {
        binding.searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                Toast.makeText(context, "SearchView focused", Toast.LENGTH_SHORT).show()
                fetchInitialData()
                binding.searchRecyclerView.visibility =
                    View.VISIBLE // Show RecyclerView when search is focused
            } else {
                binding.searchRecyclerView.visibility =
                    View.GONE // Hide RecyclerView when search loses focus
            }
        }
        binding.relativeLayout2.setOnClickListener {
            // Hide RecyclerView
            binding.searchRecyclerView.visibility = View.GONE
            // Clear focus from the SearchView
            binding.searchView.clearFocus()
        }

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.let { fetchData(it) }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                newText?.let { fetchData(it) }
                return true
            }
        })
    }

    private fun setUpRecyclerViews() {
        // Initialize the adapter for the search events RecyclerView
        adapter = EventSearchAdapter()
        binding.searchRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.searchRecyclerView.adapter = adapter
    }

    private fun fetchInitialData() {
        if (!currentUser.isNullOrEmpty()) {
            val eventRef = database.collection("events")
                .whereEqualTo("creatorId", currentUser)

            eventRef.get()
                .addOnSuccessListener { result ->
                    val fullEventList = mutableListOf<ReminderModel>()
                    if (!result.isEmpty) {
                        for (document in result) {
                            val reminder = document.toObject(ReminderModel::class.java)
                            fullEventList.add(reminder)
                        }
                        Log.d("FirestoreSuccess", "Successfully fetched data")
                    } else {
                        Log.d("FirestoreSuccess", "No events found")
                        Toast.makeText(context, "No events to display.", Toast.LENGTH_SHORT).show()
                    }
                    adapter.updateList(fullEventList)
                }
                .addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Error fetching data: ", exception)
                    Toast.makeText(context, "Failed to fetch events.", Toast.LENGTH_SHORT).show()
                }
        } else {
            Log.d("FirebaseAuth", "Current user not logged in")
            Toast.makeText(context, "Please log in to view your events.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchData(query: String = "") {
        if (!currentUser.isNullOrEmpty()) {
            val eventRef = database.collection("events")
                .whereEqualTo("creatorId", currentUser)

            eventRef.get()
                .addOnSuccessListener { result ->
                    val fullEventList = mutableListOf<ReminderModel>()
                    if (!result.isEmpty) {
                        for (document in result) {
                            val reminder = document.toObject(ReminderModel::class.java)
                            // Filter based on query
                            if (reminder.eventTitle.contains(query, true) ||
                                reminder.eventCategory.contains(query, true) ||
                                reminder.eventDate.contains(query, true)) {
                                fullEventList.add(reminder)
                            }
                        }
                        adapter.updateList(fullEventList)  // Update the RecyclerView
                    } else {
                        Log.d("FirestoreSuccess", "No events found")
                        Toast.makeText(context, "No events to display.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("FirestoreError", "Error fetching data: ", exception)
                    Toast.makeText(context, "Failed to fetch events.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
