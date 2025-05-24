package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
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
    private lateinit var db: FirebaseDatabase
    private lateinit var sharedPreferences: SharedPreferences
    private val progressDialog by lazy { createProgressDialog() }

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
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
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
            val userName = sharedPreferences.getString("UserName", null)
            Log.d("Home", "ShopName: $shopName, UserName: $userName")
            if (ShopName == null || userName == null) {
                Toast.makeText(context, "Null", Toast.LENGTH_SHORT).show()
            }
            name.text = userName
            shopName.text = ShopName
            (activity as? MainActivity)?.setSupportActionBar(toolbar)

            dataList = mutableListOf()
            adapter = RetailerHomeAdapter(requireContext(), dataList)
            recyclerView.layoutManager = LinearLayoutManager(requireContext())
            recyclerView.adapter = adapter
            db = FirebaseDatabase.getInstance()
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
            val newName =
                view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editName)
            val cancel =
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel)
            val editNameBtn =
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.editNameBtn)
            builder.setView(view)
            builder.setTitle("Edit Name")
            builder.setMessage("Enter new name")
            val dialog = builder.create()
            dialog.show()

            editNameBtn.setOnClickListener {
                val nameInput = newName.text.toString().trim()
                if (nameInput.isNotEmpty()) {
                    db.reference.child("Retailers").child(auth.currentUser?.uid ?: "")
                        .child("UserName")
                        .setValue(nameInput)
                        .addOnSuccessListener {
                            sharedPreferences.edit {
                                putString("UserName", nameInput)
                            }
                            binding.name.text = nameInput
                            Toast.makeText(context, "Name Updated", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                context, "Error updating name: ${e.message}", Toast.LENGTH_SHORT
                            ).show()
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
        binding.editShopName.setOnClickListener { // Fixed: Use editShopName instead of editName
            val builder = AlertDialog.Builder(context)
            val view = LayoutInflater.from(context).inflate(R.layout.update_name, null)
            val newShopName =
                view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editName)
            val cancel =
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel)
            val editNameBtn =
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.editNameBtn)
            builder.setView(view)
            builder.setTitle("Edit Shop Name")
            builder.setMessage("Enter new shop name")
            val dialog = builder.create()
            dialog.show()

            editNameBtn.setOnClickListener {
                val shopNameInput = newShopName.text.toString().trim()
                if (shopNameInput.isNotEmpty()) {
                    db.reference.child("Retailers").child(auth.currentUser?.uid ?: "")
                        .child("ShopName")
                        .setValue(shopNameInput)
                        .addOnSuccessListener {
                            sharedPreferences.edit {
                                putString("ShopName", shopNameInput)
                            }
                            binding.shopName.text = shopNameInput
                            Toast.makeText(context, "Shop Name Updated", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                context, "Error updating shop name: ${e.message}", Toast.LENGTH_SHORT
                            ).show()
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
                    Toast.makeText(requireContext(), "Signed out successfully", Toast.LENGTH_SHORT).show()
                    findNavController().navigate(R.id.action_retailerHome_to_logIn)
                    dialog.dismiss()
                } catch (e: Exception) {
                    progressDialog.dismiss()
                    Toast.makeText(
                        requireContext(), "Error signing out: ${e.message}", Toast.LENGTH_LONG
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
                    context, "Email does not match the signed-in account", Toast.LENGTH_SHORT
                    ,
                ).show()
                return@setOnClickListener
            }

            if (isAdded) {
                progressDialog.show()
                CoroutineScope(Dispatchers.Main).launch {
                    try {
                        val credential = EmailAuthProvider.getCredential(emailInput, password)
                        currentUser.reauthenticate(credential).await()
                        db.reference.child("Retailers").child(auth.uid.toString())
                            .removeValue()
                            .await()
                        currentUser.delete().await()
                        sharedPreferences.edit { clear() }
                        withContext(Dispatchers.Main) {
                            if (isAdded) {
                                progressDialog.dismiss()
                                Toast.makeText(
                                    context, "Account deleted successfully", Toast.LENGTH_SHORT
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
            db.reference.child("Retailers").child(auth.uid.toString()).child("items")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (isAdded) {
                            progressDialog.dismiss()
                            Log.d("Home", "Snapshot received, children count: ${snapshot.childrenCount}")
                            dataList.clear()
                            for (document in snapshot.children) {
                                try {
                                    // Log raw document data
                                    Log.d("Home", "Document: ${document.key}, Data: ${document.value}")

                                    // Handle quantity
                                    val quantityValue = document.child("quantity").value
                                    val quantity = when (quantityValue) {
                                        is Long -> quantityValue.toString()
                                        is String -> quantityValue
                                        is Double -> quantityValue.toInt().toString()
                                        is Int -> quantityValue.toString()
                                        is Number -> quantityValue.toInt().toString()
                                        else -> null
                                    }

                                    // Handle price
                                    val priceValue = document.child("price").value
                                    val price = when (priceValue) {
                                        is Double -> priceValue
                                        is Long -> priceValue.toDouble()
                                        is Int -> priceValue.toDouble()
                                        is Number -> priceValue.toDouble()
                                        else -> null
                                    }

                                    // Handle deliveryPrice
                                    val deliveryPriceValue = document.child("deliveryPrice").value
                                    val deliveryPrice = when (deliveryPriceValue) {
                                        is Double -> deliveryPriceValue
                                        is Long -> deliveryPriceValue.toDouble()
                                        is Int -> deliveryPriceValue.toDouble()
                                        is Number -> deliveryPriceValue.toDouble()
                                        else -> null
                                    }

                                    // Handle totalPrice
                                    val totalPriceValue = document.child("totalPrice").value
                                    val totalPrice = when (totalPriceValue) {
                                        is Double -> totalPriceValue
                                        is Long -> totalPriceValue.toDouble()
                                        is Int -> totalPriceValue.toDouble()
                                        is Number -> totalPriceValue.toDouble()
                                        else -> null
                                    }

                                    val data = RetailerItemDataClass(
                                        id = document.key ?: "",
                                        itemName = document.child("itemName").getValue(String::class.java) ?: "",
                                        price = price,
                                        deliveryPrice = deliveryPrice,
                                        quantity = quantity,
                                        totalPrice = totalPrice,
                                        unit = document.child("unit").getValue(String::class.java) ?: ""
                                    )
                                    if (data.itemName.isNullOrEmpty() || data.price == null || data.deliveryPrice == null || data.quantity == null) {
                                        Log.w(
                                            "Home",
                                            "Skipping invalid item: ${document.key}, " +
                                                    "itemName=${data.itemName}, price=${data.price}, " +
                                                    "deliveryPrice=${data.deliveryPrice}, quantity=${data.quantity}, " +
                                                    "totalPrice=${data.totalPrice}, unit=${data.unit}"
                                        )
                                    } else {
                                        dataList.add(data)
                                        Log.d("Home", "Added item: ${data.itemName}")
                                    }
                                } catch (ex: Exception) {
                                    Log.e(
                                        "Home",
                                        "Error deserializing item ${document.key}: ${ex.message}",
                                        ex
                                    )
                                    Toast.makeText(
                                        context,
                                        "Error deserializing item: ${document.key}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                            Log.d("Home", "Data list size: ${dataList.size}")
                            adapter.notifyDataSetChanged()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        if (isAdded) {
                            progressDialog.dismiss()
                            Log.e("Home", "Error loading items: ${error.message}", error.toException())
                            Toast.makeText(
                                context, "Error loading items: ${error.message}", Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog.dismiss()
        _binding = null
    }
}