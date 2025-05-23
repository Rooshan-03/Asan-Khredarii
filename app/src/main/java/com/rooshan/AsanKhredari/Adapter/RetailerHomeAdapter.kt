package com.rooshan.AsanKhredari.Adapter

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.MaterialAutoCompleteTextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rooshan.AsanKhredari.DataClass.RetailerItemDataClass
import com.rooshan.AsanKhredari.R
import java.io.ByteArrayOutputStream
import kotlin.collections.getOrNull

class RetailerHomeAdapter(private val context: Context, private var dataList: MutableList<RetailerItemDataClass>) :
    RecyclerView.Adapter<RetailerHomeAdapter.ViewHolderClass>() {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val progressDialog by lazy { createProgressDialog() }

    interface ImagePickerCallback {
        fun pickImageForUpdate(onImagePicked: (Uri?) -> Unit)
    }

    private fun createProgressDialog(): Dialog = Dialog(context).apply {
        setContentView(R.layout.progress_dialoug)
        setCancelable(false)
    }

    private var imagePickerCallback: ImagePickerCallback? = null

    fun setImagePickerCallback(callback: ImagePickerCallback) {
        this.imagePickerCallback = callback
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClass {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.retailer_item_layout, parent, false)
        return ViewHolderClass(view)
    }

    override fun onBindViewHolder(holder: ViewHolderClass, position: Int) {
        val currentItem = dataList.getOrNull(position) ?: return
        currentItem.imageBase64?.let { base64String ->
            try {
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                holder.img.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("Adapter", "Error decoding image for item ${currentItem.id}: ${e.message}", e)
                holder.img.setImageResource(android.R.drawable.ic_menu_camera)
            }
        } ?: holder.img.setImageResource(android.R.drawable.ic_menu_camera)

        holder.name.text = currentItem.itemName ?: "N/A"
        holder.price.text = currentItem.price?.toString() ?: "N/A"
        holder.deliveryPrice.text = currentItem.deliveryPrice?.toString() ?: "N/A"
        holder.quantity.text = currentItem.quantity?.toString() ?: "N/A"
        holder.unit.text = currentItem.unit ?: "N/A"
        holder.totalPrice.text = currentItem.totalPrice?.toString() ?: "N/A"
        holder.moreIcon.setOnClickListener { view ->
            showPopupMenu(view, position, currentItem)
        }
    }

    private fun showPopupMenu(view: View, position: Int, data: RetailerItemDataClass) {
        val popupMenu = PopupMenu(context, view)
        popupMenu.menuInflater.inflate(R.menu.popup_menu, popupMenu.menu)
        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.menuUpdate -> {
                    UpdateRecord(data, position)
                    true
                }

                R.id.menuDelete -> {
                    DeleteRecord(data, position)
                    true
                }

                else -> false
            }
        }
        popupMenu.show()
    }

    private fun DeleteRecord(data: RetailerItemDataClass, position: Int) {
        if (auth.currentUser == null) {
            Toast.makeText(context, "Please log in to delete items", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(context).apply {
            setTitle("Delete Item")
            setMessage("Are you sure you want to delete this item?")
            setPositiveButton("Delete") { _, _ ->
                progressDialog.show()
                Toast.makeText(context, "Deleting...", Toast.LENGTH_SHORT).show()
                data.id?.let { id ->
                    db.collection("Retailer").document(auth.uid.toString()).collection("items")
                        .document(id)
                        .delete()
                        .addOnSuccessListener {
                            if (position >= 0 && position < dataList.size) {
                                dataList.removeAt(position)
                                notifyItemRemoved(position)
                                notifyItemRangeChanged(position, dataList.size)
                                progressDialog.dismiss()
                                Toast.makeText(context, "Item Deleted", Toast.LENGTH_SHORT).show()
                            } else {
                                progressDialog.dismiss()
                                Log.w(
                                    "Adapter",
                                    "Invalid position $position for deletion, list size: ${dataList.size}"
                                )
                                Toast.makeText(
                                    context,
                                    "Item deleted but UI not updated",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                        .addOnFailureListener { e ->
                            progressDialog.dismiss()
                            Toast.makeText(
                                context,
                                "Error deleting: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                } ?: Toast.makeText(context, "Item ID is missing", Toast.LENGTH_SHORT).show()
            }
            setNegativeButton("Cancel", null)
            show()
        }
    }

    @SuppressLint("MissingInflatedId")
    private fun UpdateRecord(data: RetailerItemDataClass, position: Int) {
        if (auth.currentUser == null) {
            Toast.makeText(context, "Please log in to update items", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Update Item")
        val view = LayoutInflater.from(context).inflate(R.layout.update_retailer_data_layout, null)

        val updateName =
            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editItemName)
        val updatePrice =
            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editItemPrice)
        val updateDeliveryPrice =
            view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editDeliveryPrice)
        val updateImg = view.findViewById<ImageView>(R.id.itemImage)
        val btnChangeImage =
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnChangeImage)
        val btnCancel =
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnCancel)
        val updateQuantity = view.findViewById<EditText>(R.id.item_quantity_input)
        val updateUnit = view.findViewById<MaterialAutoCompleteTextView>(R.id.item_unit_spinner)
        val btnUpdateItem =
            view.findViewById<com.google.android.material.button.MaterialButton>(R.id.btnUpdateItem)
        val units = listOf("kg", "g", "Dozen", "Liters", "Piece")
        val adapter = ArrayAdapter(context, android.R.layout.simple_dropdown_item_1line, units)
        updateUnit.setAdapter(adapter)

        updateName.setText(data.itemName ?: "")
        updatePrice.setText(data.price?.toString() ?: "")
        updateDeliveryPrice.setText(data.deliveryPrice?.toString() ?: "")
        updateQuantity.setText(data.quantity?.toString() ?: "")
        updateUnit.setText(data.unit ?: "", false) // Set without triggering dropdown

        data.imageBase64?.let { base64String ->
            try {
                val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                updateImg.setImageBitmap(bitmap)
            } catch (e: Exception) {
                Log.e("Adapter", "Error decoding image for update: ${e.message}", e)
                updateImg.setImageResource(android.R.drawable.ic_menu_camera)
            }
        } ?: updateImg.setImageResource(android.R.drawable.ic_menu_camera)

        var newImageBase64: String? = null

        btnChangeImage.setOnClickListener {
            imagePickerCallback?.pickImageForUpdate { uri ->
                uri?.let {
                    updateImg.setImageURI(it)
                    newImageBase64 = encodeImage(it)
                    if (newImageBase64 == null) {
                        Toast.makeText(context, "Failed to encode new image", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            } ?: run {
                Toast.makeText(context, "Image picker not available", Toast.LENGTH_SHORT).show()
            }
        }

        builder.setView(view)
        var dialog: AlertDialog? = null
        btnUpdateItem.setOnClickListener {
            progressDialog.show()
            if (auth.currentUser == null) {
                progressDialog.dismiss()
                Toast.makeText(context, "Please log in to update items", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val name = updateName.text.toString().trim()
            val priceText = updatePrice.text.toString().trim()
            val deliveryPriceText = updateDeliveryPrice.text.toString().trim()
            val quantityText = updateQuantity.text.toString().trim()
            val unit = updateUnit.text.toString().trim()

            if (name.isEmpty() || priceText.isEmpty() || deliveryPriceText.isEmpty() || quantityText.isEmpty() || unit.isEmpty()) {
                progressDialog.dismiss()
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val price = priceText.toDoubleOrNull()
            val deliveryPrice = deliveryPriceText.toDoubleOrNull()
            val quantity = quantityText.toIntOrNull()
            if (price == null || deliveryPrice == null || quantity == null) {
                progressDialog.dismiss()
                Toast.makeText(
                    context,
                    "Please enter valid numbers for price, delivery price, and quantity",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Validate unit
            if (unit !in units) {
                progressDialog.dismiss()
                Toast.makeText(
                    context,
                    "Please select a valid unit (kg, Dozen, Liters, Piece)",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            val totalPrice = price + deliveryPrice
            if (data.id == null) {
                progressDialog.dismiss()
                Toast.makeText(context, "Item ID is missing", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val updates = mutableMapOf<String, Any?>(
                "itemName" to name,
                "price" to price,
                "deliveryPrice" to deliveryPrice,
                "quantity" to quantity,
                "unit" to unit,
                "totalPrice" to totalPrice
            )
            if (newImageBase64 != null) {
                updates["imageBase64"] = newImageBase64
            } else if (data.imageBase64 != null) {
                updates["imageBase64"] = data.imageBase64
            }

            Log.d("Adapter", "Updating item ${data.id} with unit: $unit")
            db.collection("Retailer").document(auth.uid.toString()).collection("items")
                .document(data.id!!)
                .update(updates)
                .addOnSuccessListener {
                    progressDialog.dismiss()
                    Log.d(
                        "Adapter",
                        "Item updated successfully in Firestore: ${data.id}, unit: $unit"
                    )
                    if (position >= 0 && position < dataList.size) {
                        dataList[position] = RetailerItemDataClass(
                            id = data.id,
                            itemName = name,
                            price = price,
                            deliveryPrice = deliveryPrice,
                            quantity = quantity.toString(),
                            unit = unit,
                            totalPrice = totalPrice,
                            imageBase64 = newImageBase64 ?: data.imageBase64
                        )
                        notifyItemChanged(position)
                        Toast.makeText(context, "Item Updated", Toast.LENGTH_SHORT).show()
                    } else {
                        progressDialog.dismiss()
                        Log.w(
                            "Adapter",
                            "Invalid position $position for update, list size: ${dataList.size}"
                        )
                        Toast.makeText(
                            context,
                            "Item updated in Firestore but UI not refreshed",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                    dialog?.dismiss()
                }
                .addOnFailureListener { e ->
                    progressDialog.dismiss()
                    Log.e("Adapter", "Error updating item: ${e.message}", e)
                    Toast.makeText(context, "Error updating: ${e.message}", Toast.LENGTH_LONG)
                        .show()
                }
        }

        btnCancel.setOnClickListener {
            dialog?.dismiss()
        }

        dialog = builder.create()
        dialog.show()
    }

    private fun encodeImage(uri: Uri): String? {
        return try {
            val inputStream = context?.contentResolver?.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val baos = ByteArrayOutputStream()
            bitmap?.compress(Bitmap.CompressFormat.JPEG, 80, baos)
            val byteArray = baos.toByteArray()
            Base64.encodeToString(byteArray, Base64.DEFAULT)
        } catch (e: Exception) {
            Log.e("Adapter", "Error encoding image from URI $uri: ${e.message}", e)
            null
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }


    class ViewHolderClass(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val img: ImageView = itemView.findViewById(R.id.itemImg)
        val name: TextView = itemView.findViewById(R.id.itemName)
        val price: TextView = itemView.findViewById(R.id.itemPrice)
        val deliveryPrice: TextView = itemView.findViewById(R.id.itemDeliveryPrice)
        val quantity: TextView = itemView.findViewById(R.id.itemQuantity)
        val unit: TextView = itemView.findViewById(R.id.itemUnit)
        val totalPrice: TextView = itemView.findViewById(R.id.itemTotalPrice)
        val moreIcon: ImageView = itemView.findViewById(R.id.more_icon)
    }
}