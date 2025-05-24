package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase // Import for Realtime Database
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentSplashScreenBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@SuppressLint("CustomSplashScreen")
class SplashScreen : Fragment() {
    private var _binding: FragmentSplashScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseDatabase // Change to FirebaseDatabase
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSplashScreenBinding.inflate(inflater, container, false)
        Log.d("SplashScreen", "View created, layout inflated: ${binding.root.id}")

        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        // Initialize Firebase Realtime Database
        db = FirebaseDatabase.getInstance()
        // Initialize SharedPreferences
        sharedPreferences = requireActivity().getSharedPreferences("Details", MODE_PRIVATE)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("SplashScreen", "Starting splash delay")
                delay(2000) // Delay for 2 seconds

                val userId = auth.currentUser?.uid

                // Check if a user is logged in
                if (userId == null) {
                    // No user logged in, navigate to LogIn screen
                    findNavController().navigate(R.id.action_splashScreen_to_logIn)
                    return@launch // Exit the coroutine
                }

                // Reference to the user's role in Realtime Database
                // The path would be "Roles/{userId}/Role"
                val roleRef = db.getReference("Roles").child(userId).child("Role")

                // Fetch the role data from Realtime Database
                val dataSnapshot = roleRef.get().await()
                val role = dataSnapshot.getValue(String::class.java) // Get the string value

                // Check the role and navigate accordingly
                if (role.isNullOrEmpty()) {
                    // Role is not found or empty, navigate to LogIn
                    Log.d("SplashScreen", "Role not found or empty for user $userId, navigating to LogIn")
                    findNavController().navigate(R.id.action_splashScreen_to_logIn)
                } else {
                    // Role found, save it to SharedPreferences
                    sharedPreferences.edit().putString("Role", role).apply()
                    Log.d("SplashScreen", "Role '$role' found for user $userId, navigating to respective home screen")

                    // Navigate based on the role
                    if (role == "Retailer") {
                        findNavController().navigate(R.id.action_splashScreen_to_retailerHome)
                    } else if (role == "Customers") {
                        findNavController().navigate(R.id.action_splashScreen_to_customerHome)
                    } else {
                        // Handle unexpected roles or default to login
                        Log.w("SplashScreen", "Unexpected role '$role' for user $userId, navigating to LogIn")
                        findNavController().navigate(R.id.action_splashScreen_to_logIn)
                    }
                }
            } catch (e: Exception) {
                // Log any errors that occur during the process
                Log.e("SplashScreen", "Error in splash screen: ${e.message}", e)
                // In case of an error (e.g., network issue, database read failure),
                // it's often best to navigate to the login screen or a generic error screen.
                findNavController().navigate(R.id.action_splashScreen_to_logIn)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Clear the binding reference to avoid memory leaks
    }
}
