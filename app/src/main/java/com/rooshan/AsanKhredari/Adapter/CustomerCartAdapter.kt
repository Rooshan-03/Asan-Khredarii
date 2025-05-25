package com.rooshan.AsanKhredari.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.rooshan.AsanKhredari.DataClass.CustomerCartDataClass
import com.rooshan.AsanKhredari.R

class CustomerCartAdapter(
    private val shopId: String?,
    private var dataList: MutableList<CustomerCartDataClass>,
    private val context: Context
) : RecyclerView.Adapter<CustomerCartAdapter.ViewHolderClass>() {
    private var db: DatabaseReference = FirebaseDatabase.getInstance().getReference("Customers")
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClass {
        val view = LayoutInflater.from(context).inflate(R.layout.customer_cart_item_layout, parent, false)
        return ViewHolderClass(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolderClass, position: Int) {
        val data = dataList[position]
        holder.itmName.text = data.itemCartName
        try {
            val totalPrice = data.itemUnitPrice.toDouble() * data.itemCartQuantity.toInt()
            holder.itemPrice.text = String.format("%.2f", totalPrice)
        } catch (e: NumberFormatException) {
            Log.e("CustomerCartAdapter", "Error parsing price or quantity: ${e.message}")
            holder.itemPrice.text = "0.00"
        }
        holder.itemQuantity.text = data.itemCartQuantity
        holder.itemUnit.text = data.itemCartUnit
        val userId = auth.currentUser?.uid
        holder.menu.setOnClickListener {
            val popupMenu = PopupMenu(context, holder.menu)
            popupMenu.menuInflater.inflate(R.menu.cart_menu, popupMenu.menu)
            popupMenu.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    R.id.deleteItem -> {
                        deleteItemFromCart(data, holder)
                        true
                    }
                    else -> false
                }
            }
            popupMenu.show()
        }
        holder.increaseBtn.setOnClickListener {
            handleIncrease(holder, data, userId)
        }
        holder.decreaseBtn.setOnClickListener {
            handleDecrease(holder, data, userId)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleDecrease(holder: ViewHolderClass, data: CustomerCartDataClass, userId: String?) {
        if (userId == null) {
            Log.e("CustomerCartAdapter", "User is not authenticated")
            return
        }
        try {
            val currentQuantity = holder.itemQuantity.text.toString().toInt()
            if (currentQuantity > 1) {
                val newQuantity = currentQuantity - 1
                val unitPrice = data.itemUnitPrice.toDouble()
                val newTotalPrice = unitPrice * newQuantity
                val cartItemRef = db.child(userId).child(shopId.toString()).child("Cart").child(data.key)
                val updates = mapOf("quantity" to newQuantity, "totalPrice" to unitPrice)
                cartItemRef.updateChildren(updates).addOnSuccessListener {
                    Log.d("CustomerCartAdapter", "Decreased item ${data.key}: quantity=$newQuantity")
                }.addOnFailureListener {
                    Log.e("CustomerCartAdapter", "Failed to decrease item ${data.key}: ${it.message}")
                }
                holder.itemQuantity.text = newQuantity.toString()
                holder.itemPrice.text = String.format("%.2f", newTotalPrice)
            }
        } catch (e: NumberFormatException) {
            Log.e("CustomerCartAdapter", "Error parsing quantity or price: ${e.message}")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun handleIncrease(holder: ViewHolderClass, data: CustomerCartDataClass, userId: String?) {
        if (userId == null) {
            Log.e("CustomerCartAdapter", "User is not authenticated")
            return
        }
        try {
            val currentQuantity = holder.itemQuantity.text.toString().toInt()
            val newQuantity = currentQuantity + 1
            val unitPrice = data.itemUnitPrice.toDouble()
            val newTotalPrice = unitPrice * newQuantity
            val cartItemRef = db.child(userId).child(shopId.toString()).child("Cart").child(data.key)
            val updates = mapOf("quantity" to newQuantity, "totalPrice" to unitPrice)
            cartItemRef.updateChildren(updates).addOnSuccessListener {
                Log.d("CustomerCartAdapter", "Increased item ${data.key}: quantity=$newQuantity")
            }.addOnFailureListener {
                Log.e("CustomerCartAdapter", "Failed to increase item ${data.key}: ${it.message}")
            }
            holder.itemQuantity.text = newQuantity.toString()
            holder.itemPrice.text = String.format("%.2f", newTotalPrice)
        } catch (e: NumberFormatException) {
            Log.e("CustomerCartAdapter", "Error parsing quantity or price: ${e.message}")
        }
    }

    private fun deleteItemFromCart(data: CustomerCartDataClass, holder: ViewHolderClass) {
        val userId = auth.currentUser?.uid ?: return
        val cartItemRef = db.child(userId).child(shopId.toString()).child("Cart").child(data.key)
        cartItemRef.removeValue().addOnSuccessListener {
            Log.d("CustomerCartAdapter", "Deleted item ${data.key}")
            val position = holder.adapterPosition
            if (position != RecyclerView.NO_POSITION) {
                dataList.removeAt(position)
            } else {
                notifyItemRemoved(position)
            }
        }.addOnFailureListener {
            Log.e("CustomerCartAdapter", "Failed to delete item ${data.key}: ${it.message}")
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolderClass(view: View) : RecyclerView.ViewHolder(view) {
        val itmName: TextView = view.findViewById(R.id.itemCartName)
        val itemPrice: TextView = view.findViewById(R.id.itemCartTotalPrice)
        val itemQuantity: TextView = view.findViewById(R.id.itemCartQuantity)
        val itemUnit: TextView = view.findViewById(R.id.itemCartUnit)
        val increaseBtn: TextView = view.findViewById(R.id.btnIncrease)
        val decreaseBtn: TextView = view.findViewById(R.id.btnDecrease)
        val menu: View = view.findViewById(R.id.moreImg)
    }
}