package com.rooshan.AsanKhredari.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.rooshan.AsanKhredari.DataClass.CustomerShopItemsDataClass
import com.rooshan.AsanKhredari.R

class CustomerShopItemsAdapter(
    private val shopId: String?,
    private val context: Context,
    private val dataList: MutableList<CustomerShopItemsDataClass>
) : RecyclerView.Adapter<CustomerShopItemsAdapter.ViewHolderClass>() {
    private var db: DatabaseReference = FirebaseDatabase.getInstance().getReference("Customers")
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClass {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.product_item_customer_layout, parent, false)
        return ViewHolderClass(view)
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: ViewHolderClass, position: Int) {
        val currentItem = dataList[position]
        holder.itemName.text = currentItem.itemName ?: "Unknown Item"
        holder.itemPrice.text = String.format("Rs%.1f", currentItem.price ?: 0.0)
        holder.itemDeliveryPrice.text = String.format("Rs%.1f", currentItem.deliveryPrice ?: 0.0)
        holder.itemQuantity.text = currentItem.quantity?.toString() ?: "0"
        holder.itemUnit.text = currentItem.unit ?: "N/A"
        holder.itemTotalPrice.text = String.format("Rs%.1f", currentItem.totalPrice ?: 0.0)
        holder.itemCartBtn.setOnClickListener {
            storeToRTDB(currentItem)
        }
    }

    override fun getItemCount(): Int = dataList.size

    inner class ViewHolderClass(view: View) : RecyclerView.ViewHolder(view) {
        val itemName: TextView = view.findViewById(R.id.itemName)
        val itemPrice: TextView = view.findViewById(R.id.itemPrice)
        val itemDeliveryPrice: TextView = view.findViewById(R.id.itemDeliveryPrice)
        val itemQuantity: TextView = view.findViewById(R.id.itemQuantity)
        val itemUnit: TextView = view.findViewById(R.id.itemUnit)
        val itemTotalPrice: TextView = view.findViewById(R.id.itemTotalPrice)
        val itemCartBtn: Button = view.findViewById(R.id.addToCartButton)
    }

    private fun CustomerShopItemsAdapter.storeToRTDB(
        currentItem: CustomerShopItemsDataClass
    ) {
        val itemKey = currentItem.key
        if (itemKey == null) {
            Toast.makeText(context, "Invalid item key", Toast.LENGTH_SHORT).show()
            return
        }
        val userId = auth.currentUser?.uid
        val cartRef = db.child(userId.toString()).child(shopId.toString()).child("Cart").child(itemKey)

        cartRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                Toast.makeText(context, "Item already in cart", Toast.LENGTH_SHORT).show()
            } else {
                cartRef.setValue(currentItem)
                    .addOnSuccessListener {
                        Toast.makeText(context, "Item added to cart", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Failed to add item", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error checking cart", Toast.LENGTH_SHORT).show()
        }
    }

}