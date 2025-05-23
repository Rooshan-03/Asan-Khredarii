package com.rooshan.AsanKhredari.Fragments

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
import com.google.firebase.firestore.FirebaseFirestore
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentSplashScreenBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SplashScreen : Fragment() {
    private var _binding: FragmentSplashScreenBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentSplashScreenBinding.inflate(inflater, container, false)
        Log.d("SplashScreen", "View created, layout inflated: ${binding.root.id}")
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        sharedPreferences = requireActivity().getSharedPreferences("Details", MODE_PRIVATE)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Log.d("SplashScreen", "Starting splash delay")
                delay(2000)
                val userId = auth.currentUser?.uid
                val document = db.collection("Roles").document(userId.toString()).get().await()
                val role = document.getString("Role")
                if (role.isNullOrEmpty()) {
                    findNavController().navigate(R.id.action_splashScreen_to_logIn)
                } else {
                    sharedPreferences.edit().putString("Role", role).apply()
                    if (role == "Retailer") {
                        findNavController().navigate(R.id.action_splashScreen_to_retailerHome)
                    } else if (role == "Customers") {
                        findNavController().navigate(R.id.action_splashScreen_to_customerHome)
                    }
                }
            } catch (e: Exception) {
                Log.e("SplashScreen", "Error in splash screen: ${e.message}", e)
            }
        }
//        viewLifecycleOwner.lifecycleScope.launch {
//            try {
//                Log.d("SplashScreen", "Starting splash delay")
//                delay(5000) // Extended to 5 seconds for testing
//                Log.d("SplashScreen", "Delay complete")
//
//                val navController = findNavController()
//                Log.d("SplashScreen", "Current destination: ${navController.currentDestination?.id}, Expected: ${R.id.splashScreen}")
//
//                if (navController.currentDestination?.id == R.id.splashScreen) {
//                    val user = auth.currentUser
//                    Log.d("SplashScreen", "User: ${user?.uid}, EmailVerified: ${user?.isEmailVerified}")
//
//                    if (user != null && user.isEmailVerified) {
//                        if (role.isNullOrEmpty()) {
//                            Log.d("SplashScreen", "No role found, navigating to login")
//                            navController.navigate(R.id.action_splashScreen_to_logIn)
//                        } else {
//                            Log.d("SplashScreen", "Navigating based on role: $role")
//                            if (role == "Retailer") {
//                                navController.navigate(R.id.action_splashScreen_to_retailerHome)
//                            } else {
//                                navController.navigate(R.id.action_splashScreen_to_customerHome)
//                            }
//                        }
//                    } else {
//                        Log.d("SplashScreen", "No user or email not verified, navigating to login")
//                        navController.navigate(R.id.action_splashScreen_to_logIn)
//                    }
//                } else {
//                    Log.d("SplashScreen", "Unexpected destination, skipping navigation")
//                }
//            } catch (e: Exception) {
//                Log.e("SplashScreen", "Error in splash screen: ${e.message}", e)
//            }
//        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}