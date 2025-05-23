package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Adapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.edit
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rooshan.AsanKhredari.Adapter.RetailerHomeAdapter
import com.rooshan.AsanKhredari.DataClass.RetailerItemDataClass
import com.rooshan.AsanKhredari.MainActivity
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentRetailerHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext


class RetailerHome : Fragment() {
    private var _binding: FragmentRetailerHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: RetailerHomeAdapter
    private lateinit var dataList: MutableList<RetailerItemDataClass>
    private lateinit var db: FirebaseFirestore
    private lateinit var sharedPreferences: SharedPreferences
    private val progressDialog by lazy { createProgressDialog() }

    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            currentImageCallback?.invoke(uri)
        }
    private var currentImageCallback: ((Uri?) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        setHasOptionsMenu(true)
    }

    @Deprecated("Deprecated in Java")
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.option_menu, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRetailerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun createProgressDialog(): Dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.progress_dialoug)
        setCancelable(false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            ChangeYourName()
            ChangeYourShopName()

            sharedPreferences = requireContext().getSharedPreferences("Details", 0)
            val ShopName = sharedPreferences.getString("ShopName", null)
            val UserName = sharedPreferences.getString("UserName", null)
            Log.d("Home", "ShopName: $ShopName, UserName: $UserName")
            if (ShopName == null || UserName == null) {
                Toast.makeText(context, "Null", Toast.LENGTH_SHORT).show()
            }
            name.text = UserName
            shopName.text = ShopName
            (activity as? MainActivity)?.setSupportActionBar(toolbar)

            dataList = mutableListOf()
            adapter = RetailerHomeAdapter(requireContext(), dataList)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
            db = FirebaseFirestore.getInstance()
            adapter.setImagePickerCallback(object : RetailerHomeAdapter.ImagePickerCallback {
                override fun pickImageForUpdate(onImagePicked: (Uri?) -> Unit) {
                    currentImageCallback = onImagePicked
                    pickImageLauncher.launch("image/*")
                }
            })
            if (auth.currentUser != null) {
                progressDialog.show()
                loadData()
            } else {
                Toast.makeText(context, "Please log in to view items", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ChangeYourName() {
        binding.editName.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val view = LayoutInflater.from(context).inflate(R.layout.update_name, null)
            val newName = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editName)
            val cancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel)
            val editNameBtn = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.editNameBtn)
            builder.setView(view)
            builder.setTitle("Edit Name")
            builder.setMessage("Enter new name")
            val dialog = builder.create()
            dialog.show()

            editNameBtn.setOnClickListener {
                val nameInput = newName.text.toString().trim()
                if (nameInput.isNotEmpty()) {
                    db.collection("Retailer").document(auth.currentUser?.uid ?: "")
                        .update("UserName", nameInput)
                        .addOnSuccessListener {
                            sharedPreferences.edit {
                                putString("UserName", nameInput)
                            }
                            binding.name.text = nameInput
                            Toast.makeText(context, "Name Updated", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error updating name: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Please enter a valid name", Toast.LENGTH_SHORT).show()
                }
            }
            cancel.setOnClickListener {
                dialog.dismiss()
            }
        }
    }

    private fun ChangeYourShopName() {
        binding.editName.setOnClickListener {
            val builder = AlertDialog.Builder(context)
            val view = LayoutInflater.from(context).inflate(R.layout.update_name, null)
            val newShopName = view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editName)
            val cancel = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel)
            val editNameBtn = view.findViewById<com.google.android.material.button.MaterialButton>(R.id.editNameBtn)
            builder.setView(view)
            builder.setTitle("Edit Shop Name")
            builder.setMessage("Enter new shop name")
            val dialog = builder.create()
            dialog.show()
            editNameBtn.setOnClickListener {
                val shopNameInput = newShopName.text.toString().trim()
                if (shopNameInput.isNotEmpty()) {
                    db.collection("Retailer").document(auth.currentUser?.uid ?: "")
                        .update("UserName", shopNameInput)
                        .addOnSuccessListener {
                            sharedPreferences.edit {
                                putString("ShopName", shopNameInput)
                            }
                            binding.shopName.text = shopNameInput
                            Toast.makeText(context, "Shop Name Updated", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(context, "Error updating shop name: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(context, "Please enter a valid shop name", Toast.LENGTH_SHORT).show()
                }
            }
            cancel.setOnClickListener {
                dialog.dismiss()
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.signOut -> {
                implementSignOut()
            }

            R.id.deleteAccount -> {
                implementDeleteAccount()
            }

            R.id.trackRecord -> {
                Toast.makeText(
                    context,
                    "It will be Implemented Once Customer App is complete",
                    Toast.LENGTH_SHORT
                ).show()
                findNavController().navigate(R.id.action_retailerHome_to_retailerTrackRecord)
            }

            R.id.addItemBtn -> {
                if (auth.currentUser == null) {
                    findNavController().navigate(R.id.action_retailerHome_to_logIn)
                } else {
                    findNavController().navigate(R.id.action_retailerHome_to_retailerAddItem)
                }
            }
        }
        return true
    }

    private fun implementSignOut() {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Sign Out")
        builder.setMessage("Are you sure you want to sign out?")
        builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
        builder.setCancelable(true)
        builder.setPositiveButton("Sign Out") { dialog, _ ->
            if (isAdded) {
                progressDialog.show()
                try {
                    auth.signOut()
                    sharedPreferences.edit { clear() }
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT)
                        .show()
                    findNavController().navigate(R.id.action_retailerHome_to_logIn)
                    dialog.dismiss()
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Error signing out: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    dialog.dismiss()
                }
            }
        }
        val dialog = builder.create()
        dialog.show()
    }

    @SuppressLint("MissingInflatedId")
    private fun implementDeleteAccount() {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(context, "No user is signed in", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Delete Account")
        builder.setMessage("This will permanently delete your account and all associated data. Are you sure?")
        val view = LayoutInflater.from(context).inflate(R.layout.delete_account_layout, null)
        val pass =
            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.pass)
        val email =
            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.email)
        val deleteAccount =
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.deleteAccount)
        val cancel =
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel)
        builder.setView(view)
        val dialog = builder.create()
        dialog.show()

        cancel.setOnClickListener {
            dialog.dismiss()
        }

        deleteAccount.setOnClickListener {
            val emailInput = email.text.toString().trim()
            val password = pass.text.toString().trim()

            if (emailInput.isEmpty()) {
                Toast.makeText(context, "Please enter your email", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                Toast.makeText(context, "Please enter your password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (emailInput != currentUser.email) {
                Toast.makeText(
                    context,
                    "Email does not match the signed-in account",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            if (isAdded) {
                progressDialog.show()
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val credential = EmailAuthProvider.getCredential(emailInput, password)
                        currentUser.reauthenticate(credential).await()
                        db.collection("Retailer").document(auth.uid.toString())
                            .delete()
                            .await()
                        currentUser.delete().await()
                        sharedPreferences.edit { clear() }
                        withContext(Dispatchers.Main) {
                            if (isAdded) {
                                progressDialog.dismiss()
                                Toast.makeText(
                                    context,
                                    "Account deleted successfully",
                                    Toast.LENGTH_SHORT
                                ).show()
                                dialog.dismiss()
                                findNavController().navigate(R.id.action_retailerHome_to_logIn)
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            if (isAdded) {
                                progressDialog.dismiss()
                                val message = when {
                                    e.message?.contains("credential is incorrect") == true -> "Incorrect password. Please try again."
                                    e.message?.contains("no user record") == true -> "No user found."
                                    else -> "Error: ${e.message}"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun loadData() {
        if (isAdded) {
            progressDialog.show()
            db.collection("Retailer").document(auth.uid.toString()).collection("items")
                .addSnapshotListener { snapshots, e ->
                    if (isAdded) {
                        progressDialog.dismiss()
                        if (e != null) {
                            Log.e("Home", "Error loading items: ${e.message}", e)
                            Toast.makeText(
                                context,
                                "Error loading items: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                            return@addSnapshotListener
                        }
                        dataList.clear()
                        snapshots?.documents?.forEach { document ->
                            try {
                                val quantityValue = document.get("quantity")
                                val quantity = when (quantityValue) {
                                    is Long -> quantityValue.toString()
                                    is String -> quantityValue
                                    else -> null
                                }
                                val data = RetailerItemDataClass(
                                    id = document.id,
                                    itemName = document.getString("itemName") ?: "",
                                    price = document.getDouble("price"),
                                    deliveryPrice = document.getDouble("deliveryPrice"),
                                    quantity = quantity,
                                    totalPrice = document.getDouble("totalPrice"),
                                    unit = document.getString("unit") ?: "",
                                    imageBase64 = document.getString("imageBase64")
                                )
                                if (data.itemName.isNullOrEmpty() || data.price == null || data.deliveryPrice == null || data.quantity == null) {
                                    Log.w("Home", "Skipping invalid item: ${document.id}")
                                } else {
                                    dataList.add(data)
                                }
                            } catch (ex: Exception) {
                                Log.e(
                                    "Home",
                                    "Error deserializing item ${document.id}: ${ex.message}",
                                    ex
                                )
                                Toast.makeText(
                                    context,
                                    "Error deserializing item: ${document.id}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        adapter.notifyDataSetChanged()
                    }
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog.dismiss()
        _binding = null
    }
}