package com.example.myapplication.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.MenuHost
import androidx.core.view.MenuProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.myapplication.R
import com.example.myapplication.repository.FirebaseRepository
import com.example.myapplication.ui.adapters.NotificationAdapter
import com.example.myapplication.ui.auth.LoginActivity
import com.example.myapplication.ui.profile.ProfileActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.skydoves.balloon.Balloon
import com.skydoves.balloon.BalloonAnimation
import com.skydoves.balloon.BalloonSizeSpec
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch
import nl.joery.animatedbottombar.AnimatedBottomBar

class DashboardActivity : AppCompatActivity() {

    private lateinit var bottomBar: AnimatedBottomBar
    private lateinit var navController: NavController
    private lateinit var notificationCounter: TextView

    private var isUpdatingTabIndex = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dashbaord)

        // toolbar
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        val menuHost = this as MenuHost
        menuHost.addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.toolbar_menu, menu)
                loadProfileImageIntoMenu(menu)
                supportActionBar?.setDisplayShowTitleEnabled(false)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.menu_one -> {
                        showProfileBalloon()
                        true
                    }

                    R.id.menu_two -> {
                        showNotificationBalloon()
                        true
                    }

                    else -> false
                }
            }
        })

        notificationCounter = findViewById(R.id.notificationCounter)
        // Get the NavHostFragment from the fragment container view
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.fragments) as NavHostFragment
        navController = navHostFragment.navController

        bottomBar = findViewById(R.id.bottom_bar)

        // Set listener for tab selections
        bottomBar.setOnTabSelectListener(object : AnimatedBottomBar.OnTabSelectListener {
            override fun onTabSelected(
                lastIndex: Int,
                lastTab: AnimatedBottomBar.Tab?,
                newIndex: Int,
                newTab: AnimatedBottomBar.Tab
            ) {
                if (isUpdatingTabIndex) {

                    return
                }
                // Perform navigation based on the selected index
                when (newIndex) {
                    0 -> navController.navigate(R.id.homeFragment)
                    1 -> navController.navigate(R.id.reminderFragment)
                    2 -> navController.navigate(R.id.searchFragment)
                    3 -> navController.navigate(R.id.userGroupsFragment)
                    4 -> navController.navigate(R.id.friendsFragment)
                }
            }
        })
    }

    private fun showNotificationBalloon() {
        val notificationBalloon = notificationBalloon(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val anchorView = toolbar.findViewById<View>(R.id.menu_two)
        anchorView?.let { notificationBalloon.showAlignBottom(it) }

        val recyclerView: RecyclerView = notificationBalloon.getContentView().findViewById(R.id.notification_recyclerView)
        val notificationsResult: TextView = notificationBalloon.getContentView().findViewById(R.id.notificationsResult)

        val adapter = NotificationAdapter(mutableListOf())
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        val currentUser = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUser != null) {
            lifecycleScope.launch {
                try {
                    val notifications = FirebaseRepository().getUnReadNotifications(currentUser)
                    adapter.updateNotifications(notifications)
                    val notificationCount = notifications.size
                    if (notificationCount > 0) {
                        notificationCounter.text = "$notificationCount"
                        notificationCounter.visibility = View.VISIBLE
                        notificationsResult.visibility = View.INVISIBLE
                    } else {
                        notificationsResult.visibility = View.VISIBLE
                        notificationCounter.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    Log.e("Notification", "Error fetching notifications", e)
                }
            }
        }

        notificationBalloon.setOnBalloonDismissListener {
            fetchUnreadNotificationsAgain(adapter)
        }
    }

    private fun fetchUnreadNotificationsAgain(adapter: NotificationAdapter) {
        val currentUser = FirebaseAuth.getInstance().currentUser?.uid
        if (currentUser != null) {
            lifecycleScope.launch {
                try {
                    val notifications = FirebaseRepository().getUnReadNotifications(currentUser)
                    adapter.updateNotifications(notifications) // Refresh the adapter with new data
                } catch (e: Exception) {
                    Log.e("Notification", "Error fetching notifications", e)
                }
            }
        }
    }

    private fun notificationBalloon(context: Context): Balloon {
      return Balloon.Builder(context)
          .setLayout(R.layout.layout_custom_notification)
          .setWidthRatio(1f)
          .setHeight(125)
          .setArrowSize(10)
          .setArrowPosition(0.82f)
          .setCornerRadius(8f)
          .setBalloonAnimation(BalloonAnimation.FADE)
          .setLifecycleOwner(this)
          .setBackgroundColorResource(R.color.white)
          .build()
    }


    private fun showProfileBalloon() {
        val profileBalloon = profileBalloon(this)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val anchorView = toolbar.findViewById<View>(R.id.menu_one)
        anchorView?.let { profileBalloon.showAlignBottom(it) }
        val profileImage: CircleImageView = profileBalloon.getContentView().findViewById(R.id.profile_image)
        val current = FirebaseAuth.getInstance().currentUser?.uid!!
        val user = FirebaseFirestore.getInstance().collection("users").document(current)
        user.get()
            .addOnSuccessListener { document ->
                val profileImageUrl = document.getString("profileImage")
                if (!profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.app_logo)
                        .circleCrop()
                        .into(profileImage)
                }
            }
            .addOnFailureListener { error ->
                Log.e("ProfileImage", "Failed to load profile image: ${error.message}")
            }

        val editButton: Button = profileBalloon.getContentView().findViewById(R.id.EditButton)
        editButton.setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }

        val logoutButton: Button = profileBalloon.getContentView().findViewById(R.id.LogOut)
        logoutButton.setOnClickListener {
            FirebaseAuth.getInstance().signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun profileBalloon(context: Context): Balloon {
        return Balloon.Builder(context)
            .setLayout(R.layout.layout_custom_profile)
            .setWidthRatio(0.5f)
            .setHeight(BalloonSizeSpec.WRAP)
            .setArrowSize(10)
            .setArrowPosition(0.85f)
            .setCornerRadius(8f)
            .setBalloonAnimation(BalloonAnimation.FADE)
            .setLifecycleOwner(this)
            .setBackgroundColorResource(R.color.white)
            .build()
    }

    private fun loadProfileImageIntoMenu(menu: Menu) {
        val menuItem = menu.findItem(R.id.menu_one)
        val actionView = menuItem.actionView as? ImageView ?: ImageView(this).apply {
            setImageResource(R.drawable.app_logo)
            setOnClickListener { showProfileBalloon() }
        }

        menuItem.actionView = actionView

        // Load the profile image from Firebase
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()

        db.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val profileImageUrl = document.getString("profileImage")
                if (!profileImageUrl.isNullOrEmpty()) {
                    Glide.with(this)
                        .load(profileImageUrl)
                        .placeholder(R.drawable.app_logo)
                        .circleCrop()
                        .into(actionView)
                }
            }
            .addOnFailureListener { error ->
                Log.e("ProfileImage", "Failed to load profile image: ${error.message}")
            }
    }

    // Method to update bottom bar tab index without navigating
    fun updateBottomBarTab(index: Int) {
        // Prevent navigation when changing the tab index programmatically
        isUpdatingTabIndex = true

        // Update the tab index
        bottomBar.selectTabAt(index)

        // Allow navigation again
        isUpdatingTabIndex = false
    }

    fun navigate(index: Int) {
        when (index) {
            0 -> {
                navController.navigate(R.id.homeFragment)
                updateBottomBarTab(0)
            }

            1 -> {
                navController.navigate(R.id.reminderFragment)
                updateBottomBarTab(1)
            }

            2 -> {
                navController.navigate(R.id.searchFragment)
                updateBottomBarTab(2)
            }

            3 -> {
                navController.navigate(R.id.userGroupsFragment)
                updateBottomBarTab(3)
            }

            4 -> {
                navController.navigate(R.id.friendsFragment)
                updateBottomBarTab(4)
            }
        }
    }
}
