package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.FirebaseDatabase
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentLogInBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class LogIn : Fragment() {
    private var _binding: FragmentLogInBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private val progressDialog by lazy { createProgressDialog() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLogInBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        sharedPreferences = requireActivity().getSharedPreferences("Details", MODE_PRIVATE)
        return binding.root
    }

    private fun createProgressDialog(): Dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.progress_dialoug)
        setCancelable(false)
    }

    private fun validateInputs(): Boolean {
        if (binding.logInEmail.text.toString().isEmpty()) {
            progressDialog.dismiss()
            binding.logInEmail.error = "Enter Email"
            return false
        }
        if (binding.logInPassword.text.toString().isEmpty()) {
            progressDialog.dismiss()
            binding.logInPassword.error = "Enter Password"
            return false
        }
        return true
    }

    @SuppressLint("MissingInflatedId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        HandleRoleDialog()
        binding.apply {
            progressDialog.dismiss()
            forgotPassword.setOnClickListener {
                progressDialog.show()
                showForgetPasswordDialog()
            }
            logInBtnLogin.setOnClickListener {
                progressDialog.show()
                lifecycleScope.launch {
                    if (validateInputs()) {
                        logInBtnLogin.isEnabled = false
                        try {
                            val result = auth.signInWithEmailAndPassword(
                                binding.logInEmail.text.toString(),
                                binding.logInPassword.text.toString()
                            ).await()

                            val user = result.user
                            if (user == null) {
                                Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                            } else if (user.isEmailVerified) {
                                val snapshot = database.reference
                                    .child("Roles")
                                    .child(user.uid)
                                    .get().await()

                                if (snapshot.exists()) {
                                    val role = snapshot.child("Role").value as? String
                                    if (role != null) {
                                        saveUserInfo(snapshot, role)
                                        val action = when (role) {
                                            "Customers" -> R.id.action_logIn_to_customerHome
                                            "Retailer" -> R.id.action_logIn_to_retailerHome
                                            else -> {
                                                Toast.makeText(requireContext(), "Unknown role", Toast.LENGTH_SHORT).show()
                                                null
                                            }
                                        }
                                        action?.let { findNavController().navigate(it) }
                                    } else {
                                        Toast.makeText(requireContext(), "Role not found", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), "Please verify your email", Toast.LENGTH_SHORT).show()
                                binding.logInEmail.error = "Verify Email"
                                binding.logInPassword.setText("")
                                auth.signOut()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            binding.logInEmail.error = e.message
                            binding.logInPassword.setText("")
                        } finally {
                            logInBtnLogin.isEnabled = true
                            progressDialog.dismiss()
                        }
                    } else {
                        progressDialog.dismiss()
                    }
                }
            }
        }
    }

    private fun saveUserInfo(snapshot: DataSnapshot, role: String) {
        sharedPreferences.edit {
            putString("Role", role)
            putString("UserName", snapshot.child("UserName").value?.toString())
            putString("Email", snapshot.child("Email").value?.toString())
            putString("Phone", snapshot.child("Phone").value?.toString())
            if (role == "Retailer") {
                putString("ShopName", snapshot.child("ShopName").value?.toString())
                putString("Location", snapshot.child("Location").value?.toString())
                putString("ShopType", snapshot.child("ShopType").value?.toString())
            }
        }
    }

    private fun showForgetPasswordDialog() {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.forget_password_layout, null)
        val dialogBuilder = AlertDialog.Builder(requireContext()).setView(dialogView)
        val emailInput = dialogView.findViewById<EditText>(R.id.etDialogEmail)
        val cancelBtn = dialogView.findViewById<MaterialButton>(R.id.btnCancel)
        val confirmBtn = dialogView.findViewById<MaterialButton>(R.id.btnConfirm)
        val alertDialog = dialogBuilder.create()

        progressDialog.dismiss()
        alertDialog.setCancelable(false)
        alertDialog.show()

        confirmBtn.setOnClickListener {
            progressDialog.show()
            val email = emailInput.text.toString()
            if (email.isEmpty()) {
                progressDialog.dismiss()
                emailInput.error = "Enter Email"
            } else {
                lifecycleScope.launch {
                    try {
                        auth.sendPasswordResetEmail(email).await()
                        Toast.makeText(context, "Email sent. Click link to reset password", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    } catch (e: Exception) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    } finally {
                        progressDialog.dismiss()
                    }
                }
            }
        }

        cancelBtn.setOnClickListener {
            progressDialog.dismiss()
            alertDialog.dismiss()
        }
    }

    private fun HandleRoleDialog() {
        binding.logInSignUpLink.setOnClickListener {
            val view = LayoutInflater.from(context).inflate(R.layout.role_dialoug, null)
            val dialog = AlertDialog.Builder(context).setView(view).create()
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.setCancelable(false)
            dialog.show()

            view.findViewById<MaterialButton>(R.id.btnRetailer).setOnClickListener {
                findNavController().navigate(R.id.action_logIn_to_signUpRetailer)
                dialog.dismiss()
            }
            view.findViewById<MaterialButton>(R.id.btnCustomer).setOnClickListener {
                findNavController().navigate(R.id.action_logIn_to_signUpCustomer)
                dialog.dismiss()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
