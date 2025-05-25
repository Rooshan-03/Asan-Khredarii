package com.rooshan.AsanKhredari.Fragments

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentRetailerAddItemBinding

class RetailerAddItem : Fragment() {
    private var _binding: FragmentRetailerAddItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var retailerRef: DatabaseReference
    private val progressDialog by lazy { createProgressDialog() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        retailerRef = database.getReference("Retailers")
    }

    private fun createProgressDialog(): Dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.progress_dialoug)
        setCancelable(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentRetailerAddItemBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            val units = listOf("kg", "g", "Dozen", "Liters", "Piece")
            val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, units)
            itemUnitInput.setAdapter(adapter)

            val backToHomePage = view.findViewById<ImageView>(R.id.backToHomePage)
            backToHomePage.setOnClickListener {
                findNavController().navigate(R.id.action_retailerAddItem_to_retailerHome)
            }

            clearDataButton.setOnClickListener {
                binding.itemNameInput.text?.clear()
                binding.itemPriceInput.text?.clear()
                binding.itemDeliveryPriceInput.text?.clear()
                binding.itemQuantityInput.text?.clear()
                binding.itemUnitInput.text?.clear()
            }

            addItemButton.setOnClickListener {
                progressDialog.show()
                saveItem()
            }
        }
    }

    private fun saveItem() {
        val itemName = binding.itemNameInput.text.toString().trim()
        val priceText = binding.itemPriceInput.text.toString().trim()
        val deliveryPriceText = binding.itemDeliveryPriceInput.text.toString().trim()
        val quantityText = binding.itemQuantityInput.text.toString().trim()
        val unit = binding.itemUnitInput.text.toString().trim()

        // Validate all required fields
        if (itemName.isEmpty()) {
            progressDialog.dismiss()
            if (isAdded) {
                Toast.makeText(context, "Enter item name", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (quantityText.isEmpty()) {
            progressDialog.dismiss()
            if (isAdded) {
                Toast.makeText(context, "Enter quantity", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (unit.isEmpty()) {
            progressDialog.dismiss()
            if (isAdded) {
                Toast.makeText(context, "Enter unit", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (priceText.isEmpty()) {
            progressDialog.dismiss()
            if (isAdded) {
                Toast.makeText(context, "Enter price", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (deliveryPriceText.isEmpty()) {
            progressDialog.dismiss()
            if (isAdded) {
                Toast.makeText(context, "Enter delivery price", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val price = priceText.toDoubleOrNull()
        val deliveryPrice = deliveryPriceText.toDoubleOrNull()

        if (price == null) {
            progressDialog.dismiss()
            if (isAdded) {
                Toast.makeText(context, "Invalid price", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (deliveryPrice == null) {
            progressDialog.dismiss()
            if (isAdded) {
                Toast.makeText(context, "Invalid delivery price", Toast.LENGTH_SHORT).show()
            }
            return
        }
        if (quantityText <= "0") {
            progressDialog.dismiss()
            if (isAdded) {
                Toast.makeText(context, "Invalid quantity", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (auth.currentUser == null) {
            progressDialog.dismiss()
            if (isAdded) {
                Toast.makeText(context, "Please sign in", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val totalPrice = price + deliveryPrice
        val itemData = mapOf(
            "itemName" to itemName,
            "price" to price,
            "deliveryPrice" to deliveryPrice,
            "quantity" to quantityText,
            "unit" to unit,
            "totalPrice" to totalPrice
        )

        // Generate a new unique key for the item
        val newItemKey = retailerRef.child(auth.uid!!).child("items").push().key

        if (newItemKey != null) {
            retailerRef.child(auth.uid!!).child("items").child(newItemKey)
                .setValue(itemData)
                .addOnSuccessListener {
                    Log.d("ItemData", "Item added successfully with ID: $newItemKey")
                    if (isAdded) {
                        Toast.makeText(context, "Item added", Toast.LENGTH_SHORT).show()
                    }
                    progressDialog.dismiss()
                    findNavController().navigate(R.id.action_retailerAddItem_to_retailerHome)

                    // Reset form
                    binding.itemNameInput.text?.clear()
                    binding.itemPriceInput.text?.clear()
                    binding.itemDeliveryPriceInput.text?.clear()
                    binding.itemQuantityInput.text?.clear()
                    binding.itemUnitInput.text?.clear()
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Log.e("ItemData", "Error saving item: ${e.message}", e)
                    if (isAdded) {
                        Toast.makeText(context, "Failed to save item", Toast.LENGTH_SHORT).show()
                    }
                }
        } else {
            progressDialog.dismiss()
            if (isAdded) {
                Toast.makeText(context, "Failed to generate item ID", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog.dismiss()
        _binding = null
    }
}