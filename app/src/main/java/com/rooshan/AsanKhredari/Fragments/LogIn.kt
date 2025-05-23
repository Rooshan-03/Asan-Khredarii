package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context.MODE_PRIVATE
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentLogInBinding
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import androidx.core.content.edit
import com.google.firebase.firestore.FirebaseFirestoreException


class LogIn : Fragment() {
    private var _binding: FragmentLogInBinding? = null
    private val binding get() = _binding!!
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val progressDialog by lazy { createProgressDialog() }
    private lateinit var sharedPreferences: SharedPreferences
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentLogInBinding.inflate(inflater, container, false)
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
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
        HandleRoleDialoug()
        binding.apply {
            progressDialog.dismiss()
            forgotPassword.setOnClickListener {
                progressDialog.show()
                showForgetPasswordDialoug()
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
                            if (result.user == null) {
                                Toast.makeText(requireContext(), "Authentication failed", Toast.LENGTH_SHORT).show()
                            } else if (result.user?.isEmailVerified == true) {
                                val document = db.collection("Roles")
                                    .document(auth.currentUser?.uid.toString()).get().await()
                                if (document.exists()) {
                                    val role = document.getString("Role")
                                    when (role) {
                                        "Customers" -> {
                                            saveUserInfo(document, role)
                                            findNavController().navigate(R.id.action_logIn_to_customerHome)
                                        }
                                        "Retailer" -> {
                                            saveUserInfo(document, role)
                                            findNavController().navigate(R.id.action_logIn_to_retailerHome)
                                        }
                                        else -> Toast.makeText(requireContext(), "Register Yourself!", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    Toast.makeText(requireContext(), "User role data not found", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), "Please verify your email", Toast.LENGTH_SHORT).show()
                                binding.logInEmail.error = "Verify Email"
                                binding.logInPassword.setText("")
                                auth.signOut()
                            }
                        } catch (e: Exception) {
                            progressDialog.dismiss()
                            when (e) {
                                is FirebaseFirestoreException -> {
                                    if (e.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                        Toast.makeText(requireContext(), "Permission denied. Check Firestore rules.", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(requireContext(), "Firestore error: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                else -> Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
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
            }        }
    }

    private fun saveUserInfo(document: DocumentSnapshot, role: String) {
        sharedPreferences.edit {
            putString("Role", role)
            putString("UserName", document.getString("UserName"))
            putString("Email", document.getString("Email"))
            putString("Phone", document.getString("Phone"))
            if (role == "Retailer") {
                putString("ShopName", document.getString("ShopName"))
                putString("Location", document.getString("Location"))
                putString("ShopType", document.getString("ShopType"))
            }
        }
    }


    private fun showForgetPasswordDialoug() {
        val dialog = LayoutInflater.from(context).inflate(R.layout.forget_password_layout, null)
        val dialogBuilder = androidx.appcompat.app.AlertDialog.Builder(requireContext())
        dialogBuilder.setView(dialog)
        val email = dialog.findViewById<EditText>(R.id.etDialogEmail)
        val cancel = dialog.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnCancel)
        val confirm =
            dialog.findViewById<androidx.appcompat.widget.AppCompatButton>(R.id.btnConfirm)
        val alertDialog = dialogBuilder.create()
        progressDialog.dismiss()
        alertDialog.setCancelable(false)
        alertDialog.show()
        confirm.setOnClickListener {
            progressDialog.show()
            if (email.text.toString().isEmpty()) {
                progressDialog.dismiss()
                email.error = "Enter Email"
            } else {
                lifecycleScope.launch {
                    try {
                        auth.sendPasswordResetEmail(email.text.toString()).await()
                        progressDialog.dismiss()
                        Toast.makeText(
                            context,
                            "Email sent. Click Link to proceed",
                            Toast.LENGTH_SHORT
                        ).show()
                        alertDialog.dismiss()
                    } catch (e: Exception) {
                        progressDialog.dismiss()
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        alertDialog.dismiss()
                    }
                }
            }
        }
        cancel.setOnClickListener {
            progressDialog.dismiss()
            alertDialog.dismiss()
        }
    }


    private fun HandleRoleDialoug() {
        binding.apply {
            logInSignUpLink.setOnClickListener {
                val view = LayoutInflater.from(context).inflate(R.layout.role_dialoug, null)
                val dialog = AlertDialog.Builder(context).setView(view).create()
                dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                dialog.setCancelable(false)
                dialog.show()
                val retailerBtn = view.findViewById<MaterialButton>(R.id.btnRetailer)
                val customerBtn = view.findViewById<MaterialButton>(R.id.btnCustomer)

                retailerBtn.setOnClickListener {
                    findNavController().navigate(R.id.action_logIn_to_signUpRetailer)
                    dialog.dismiss()
                }
                customerBtn.setOnClickListener {
                    findNavController().navigate(R.id.action_logIn_to_signUpCustomer)
                    dialog.dismiss()
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}