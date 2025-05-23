package com.rooshan.AsanKhredari.Fragments

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentRetailerAddItemBinding
import java.io.ByteArrayOutputStream


class RetailerAddItem : Fragment() {
    private var _binding: FragmentRetailerAddItemBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private var selectedImageUri: Uri? = null
    private var imageBase64: String? = null
    private val progressDialog by lazy { createProgressDialog() }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let { uri ->
                binding.itemPicture.setImageURI(uri)
                imageBase64 = encodeImage(uri)
                if (imageBase64 == null) {
                    Toast.makeText(context, "Failed to encode image", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
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
            val units=listOf("kg","g","Dozen","Liters","Piece")
            val adapter=ArrayAdapter(requireContext(),android.R.layout.simple_dropdown_item_1line,units)
            itemUnitInput.setAdapter(adapter)
            val backToHomePage = view.findViewById<ImageView>(R.id.backToHomePage)
            backToHomePage.setOnClickListener {
                findNavController().navigate(R.id.action_retailerAddItem_to_retailerHome)
            }
            itemPicture.setOnClickListener {
                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                }
                pickImageLauncher.launch(intent)
            }
            clearDataButton.setOnClickListener {
                binding.itemNameInput.text?.clear()
                binding.itemPriceInput.text?.clear()
                binding.itemDeliveryPriceInput.text?.clear()
                binding.itemQuantityInput.text?.clear()
                binding.itemUnitInput.text?.clear()
                binding.itemPicture.setImageResource(R.drawable.baseline_camera_24)
                selectedImageUri = null
                imageBase64 = null
            }

            addItemButton.setOnClickListener {
                progressDialog.show()
                saveItem()
            }
        }
    }

    private fun encodeImage(uri: Uri): String? {
        return try {
            val inputStream = requireContext().contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val baos = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, baos) // Reduced quality for smaller size
            val byteArray = baos.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("ItemData", "Error encoding image from URI $uri: ${e.message}", e)
            null
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
            Toast.makeText(context, "Please enter item name", Toast.LENGTH_SHORT).show()
            return
        }
        if (quantityText.isEmpty()) {
            progressDialog.dismiss()
            Toast.makeText(context, "Please enter quantity", Toast.LENGTH_SHORT).show()
            return
        }
        if (unit.isEmpty()) {
            progressDialog.dismiss()
            Toast.makeText(context, "Please enter unit", Toast.LENGTH_SHORT).show()
            return
        }
        if (priceText.isEmpty()) {
            progressDialog.dismiss()
            Toast.makeText(context, "Please enter price", Toast.LENGTH_SHORT).show()
            return
        }
        if (deliveryPriceText.isEmpty()) {
            progressDialog.dismiss()
            Toast.makeText(context, "Please enter delivery price", Toast.LENGTH_SHORT).show()
            return
        }
        if (selectedImageUri == null) {
            progressDialog.dismiss()
            Toast.makeText(context, "Please select an image", Toast.LENGTH_SHORT).show()
            return
        }
        if (imageBase64 == null) {
            progressDialog.dismiss()
            Toast.makeText(context, "Failed to encode image", Toast.LENGTH_SHORT).show()
            return
        }

        val price = priceText.toDoubleOrNull()
        val deliveryPrice = deliveryPriceText.toDoubleOrNull()

        if (price == null) {
            progressDialog.dismiss()
            Toast.makeText(context, "Please enter a valid price", Toast.LENGTH_SHORT).show()
            return
        }
        if (deliveryPrice == null) {
            progressDialog.dismiss()
            Toast.makeText(context, "Please enter a valid delivery price", Toast.LENGTH_SHORT).show()
            return
        }
        if (quantityText<= "0"){
            progressDialog.dismiss()
            Toast.makeText(context, "Please enter a valid quantity", Toast.LENGTH_SHORT).show()
            return
        }
        // Ensure imageBase64 is set (or null if no image)
        if (selectedImageUri != null && imageBase64 == null) {
            progressDialog.dismiss()
            Toast.makeText(context, "Image encoding failed, item will be saved without image", Toast.LENGTH_SHORT).show()
        }

        if (auth.currentUser == null) {
            progressDialog.dismiss()
            Toast.makeText(context, "Please log in to add items", Toast.LENGTH_SHORT).show()
            return
        }
        val totalPrice = price + deliveryPrice
        val data = hashMapOf(
            "itemName" to itemName,
            "price" to price,
            "deliveryPrice" to deliveryPrice,
            "quantity" to quantityText,
            "unit" to unit,
            "totalPrice" to totalPrice,
            "imageBase64" to imageBase64 // Can be null if no image or encoding failed
        )

        db.collection("Retailer").document(auth.uid.toString()).collection("items")
            .add(data)
            .addOnSuccessListener { documentReference ->
                Log.d("ItemData", "Item added successfully with ID: ${documentReference.id}")
                Toast.makeText(context, "Item added successfully", Toast.LENGTH_SHORT).show()
                progressDialog.dismiss()
                findNavController().navigate(R.id.action_retailerAddItem_to_retailerHome)
                // Reset form
                binding.itemNameInput.text?.clear()
                binding.itemPriceInput.text?.clear()
                binding.itemDeliveryPriceInput.text?.clear()
                binding.itemQuantityInput.text?.clear()
                binding.itemUnitInput.text?.clear()
                binding.itemPicture.setImageResource(R.drawable.baseline_camera_24)
                selectedImageUri = null
                imageBase64 = null
            }
            .addOnFailureListener { e ->
                progressDialog.dismiss()
                Log.e("ItemData", "Error saving item: ${e.message}", e)
                Toast.makeText(context, "Error saving item: ${e.message}", Toast.LENGTH_LONG).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        progressDialog.dismiss()
        _binding = null
    }
}