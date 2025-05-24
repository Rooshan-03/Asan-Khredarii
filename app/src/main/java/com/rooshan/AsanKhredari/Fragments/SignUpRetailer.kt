package com.rooshan.AsanKhredari.Fragments

import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.database.FirebaseDatabase
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentSignUpRetailerBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpRetailer : Fragment() {
    private var _binding: FragmentSignUpRetailerBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private val progressDialog by lazy { createProgressDialog() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpRetailerBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        sharedPreferences = requireActivity().getSharedPreferences("Details", MODE_PRIVATE)
        return binding.root
    }

    private fun createProgressDialog(): Dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.progress_dialoug)
        setCancelable(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            signUpSignInLink.setOnClickListener {
                progressDialog.show()
                findNavController().navigate(R.id.action_signUpRetailer_to_logIn)
            }

            signUpBtnRegister.setOnClickListener {
                progressDialog.show()
                if (validateInputs()) {
                    registerUser()
                }
            }
        }
    }

    private fun registerUser() {
        val email = binding.signUpEmail.text.toString()
        val password = binding.signUpPassword.text.toString()
        lifecycleScope.launch {
            try {
                val result = auth.createUserWithEmailAndPassword(email, password).await()
                val user = result.user ?: throw IllegalStateException("User creation failed")
                user.sendEmailVerification()?.await()

                val uid = user.uid
                val userData = mapOf(
                    "UserName" to binding.signUpUserName.text.toString(),
                    "ShopName" to binding.signUpShopName.text.toString(),
                    "Email" to email,
                    "Phone" to binding.signUpPhone.text.toString(),
                    "Location" to binding.signUpShopLocation.text.toString(),
                    "ShopType" to binding.signUpShopType.text.toString()
                )

                // Save to "Retailer" node
                database.reference.child("Retailers").child(uid).setValue(userData).await()

                // Save role data
                val roleData = userData + mapOf("Role" to "Retailer")
                database.reference.child("Roles").child(uid).setValue(roleData).await()

                saveDetails()
                clearAllFields()
                Toast.makeText(
                    requireContext(),
                    "Registered Successfully! Verify Email to login",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigate(R.id.action_signUpRetailer_to_logIn)
            } catch (e: Exception) {
                val message = when (e) {
                    is FirebaseAuthWeakPasswordException -> "Password is too weak"
                    is FirebaseAuthUserCollisionException -> "Email already in use"
                    else -> "Error: ${e.message}"
                }
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            } finally {
                progressDialog.dismiss()
            }
        }
    }

    private fun saveDetails() {
        sharedPreferences.edit().apply {
            putString("Role", "Retailer")
            putString("UserName", binding.signUpUserName.text.toString())
            putString("Email", binding.signUpEmail.text.toString())
            putString("Phone", binding.signUpPhone.text.toString())
            putString("ShopName", binding.signUpShopName.text.toString())
            putString("Location", binding.signUpShopLocation.text.toString())
            putString("ShopType", binding.signUpShopType.text.toString())
            apply()
        }
    }

    private fun clearAllFields() {
        binding.signUpUserName.setText("")
        binding.signUpShopName.setText("")
        binding.signUpEmail.setText("")
        binding.signUpPhone.setText("")
        binding.signUpPassword.setText("")
        binding.signUpShopLocation.setText("")
        binding.signUpShopType.setText("")
        binding.signUpConfirmPassword.setText("")
    }

    private fun validateInputs(): Boolean {
        with(binding) {
            if (signUpUserName.text.isNullOrEmpty()) {
                progressDialog.dismiss()
                signUpUserName.error = "Enter User Name"
                return false
            }
            if (signUpShopLocation.text.isNullOrEmpty()) {
                progressDialog.dismiss()
                signUpShopLocation.error = "Enter Shop Location"
                return false
            }
            if (signUpShopType.text.isNullOrEmpty()) {
                progressDialog.dismiss()
                signUpShopType.error = "Enter Shop Type"
                return false
            }
            if (signUpShopName.text.isNullOrEmpty()) {
                progressDialog.dismiss()
                signUpShopName.error = "Enter Shop Name"
                return false
            }
            if (signUpEmail.text.isNullOrEmpty()) {
                progressDialog.dismiss()
                signUpEmail.error = "Enter Email"
                return false
            }
            if (signUpPhone.text.isNullOrEmpty()) {
                progressDialog.dismiss()
                signUpPhone.error = "Enter Phone"
                return false
            }
            if (signUpPassword.text.isNullOrEmpty()) {
                progressDialog.dismiss()
                signUpPassword.error = "Enter Password"
                return false
            }
            if (signUpConfirmPassword.text.isNullOrEmpty()) {
                progressDialog.dismiss()
                signUpConfirmPassword.error = "Enter Confirm Password"
                return false
            }
            if (signUpPassword.text.toString() != signUpConfirmPassword.text.toString()) {
                progressDialog.dismiss()
                signUpConfirmPassword.error = "Passwords do not match"
                return false
            }
        }
        return true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
