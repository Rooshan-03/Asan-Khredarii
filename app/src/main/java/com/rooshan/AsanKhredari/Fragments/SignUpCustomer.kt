package com.rooshan.AsanKhredari.Fragments

import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentSignUpCustomerBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SignUpCustomer : Fragment() {


    private var _binding: FragmentSignUpCustomerBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: FirebaseFirestore
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignUpCustomerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        auth = FirebaseAuth.getInstance()
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", 0)
        binding.apply {
            signUpSignInLink.setOnClickListener {
                findNavController().navigate(R.id.action_signUpCustomer_to_logIn)
            }
            signUpBtnRegister.setOnClickListener {
                if (validateInput()) {
                    performSignUp()
                }
            }
        }
    }

    private fun performSignUp() {
        binding.apply {
            val email = signUpEmail.text.toString()
            val username = signUpUserName.text.toString()
            val phone = signUpPhone.text.toString()
            val password = signUpPassword.text.toString()

            lifecycleScope.launch {
                try {
                    auth = FirebaseAuth.getInstance()
                    db = FirebaseFirestore.getInstance()
                    val result = auth.createUserWithEmailAndPassword(email, password).await()
                    val user = result.user
                    if (user != null) {
                        user.sendEmailVerification().await()
                        saveDetails()
                        findNavController().navigate(R.id.action_signUpCustomer_to_logIn)
                        Toast.makeText(
                            requireContext(),
                            "Registered Successfully! Verify Email to login",
                            Toast.LENGTH_LONG
                        ).show()
                        var userData = hashMapOf(
                            "Username" to username,
                            "Phone" to phone,
                            "Email" to email
                        )
                        db.collection("Customers").document(user.uid).set(userData).await()
                        db.collection("Roles").document(user.uid).set(
                            hashMapOf(
                                "Role" to "Customers",
                                "UserName" to binding.signUpUserName.text.toString(),
                                "Email" to binding.signUpEmail.text.toString(),
                                "Phone" to binding.signUpPhone.text.toString()
                            )
                        ).await()
                        clearFields()
                        findNavController().navigate(R.id.action_signUpCustomer_to_logIn)
                    }
                } catch (e: Exception) {
                    Toast.makeText(
                        requireContext(),
                        "Sign-up failed: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun CoroutineScope.clearFields() {
        binding.apply {
            signUpUserName.text?.clear()
            signUpEmail.text?.clear()
            signUpPhone.text?.clear()
            signUpPassword.text?.clear()
            signUpConfirmPassword.text?.clear()
        }
    }

    private fun CoroutineScope.saveDetails() {
        var editor = sharedPreferences.edit()
        editor.putString("UserName", binding.signUpUserName.text.toString())
        editor.putString("Email", binding.signUpEmail.text.toString())
        editor.putString("Phone", binding.signUpPhone.text.toString())
        editor.putString("Role", "Customers")
        editor.apply()
    }

    private fun validateInput(): Boolean {
        var isValid = true
        binding.apply {
            val email = signUpEmail.text.toString()
            val username = signUpUserName.text.toString()
            val phone = signUpPhone.text.toString()
            val password = signUpPassword.text.toString()
            val confirmPassword = signUpConfirmPassword.text.toString()

            if (email.isEmpty()) {
                signUpEmail.error = "Email is required"
                isValid = false
            }
            if (username.isEmpty()) {
                signUpUserName.error = "Username is required"
                isValid = false
            }
            if (phone.isEmpty()) {
                signUpPhone.error = "Phone number is required"
                isValid = false
            } else if (!phone.startsWith("+")) {
                signUpPhone.error = "Phone number must include country code (e.g., +92)"
                isValid = false
            }
            if (password.isEmpty()) {
                signUpPassword.error = "Password is required"
                isValid = false
            }
            if (confirmPassword.isEmpty()) {
                signUpConfirmPassword.error = "Confirm password is required"
                isValid = false
            }
            if (password != confirmPassword) {
                signUpConfirmPassword.error = "Passwords do not match"
                isValid = false
            }
        }
        return isValid
    }
}