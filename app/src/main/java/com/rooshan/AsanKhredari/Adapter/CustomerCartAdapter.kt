package com.rooshan.AsanKhredari.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
) :
    RecyclerView.Adapter<CustomerCartAdapter.ViewHolderClass>() {
    private var db: DatabaseReference = FirebaseDatabase.getInstance().getReference("Customers")
    private var auth: FirebaseAuth = FirebaseAuth.getInstance()
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolderClass {
        val view =
            LayoutInflater.from(context).inflate(R.layout.customer_cart_item_layout, parent, false)
        return ViewHolderClass(view)
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: ViewHolderClass,
        position: Int
    ) {
        val data = dataList[position]
        holder.itmName.text = data.itemCartName
        holder.itemPrice.text = data.itemCartTotalPrice
        holder.itemQuantity.text = data.itemCartQuantity
        holder.itemUnit.text = data.itemCartUnit
        val userId = auth.currentUser?.uid.toString()
        holder.increaseBtn.setOnClickListener {
            handleIncrease(holder, data,userId)
        }
        holder.decreaseBtn.setOnClickListener {
            handleDecrease(holder, data,userId)
        }
    }

    private fun handleDecrease(holder: ViewHolderClass, data: CustomerCartDataClass,userId: String?) {
        val currentQuantity = holder.itemQuantity.text.toString().toInt()
        if (currentQuantity > 1) {
            val CartItemRef = db.child(userId.toString()).child(shopId.toString()).child("Cart").child(data.key)
            val updates = mapOf("quantity" to (currentQuantity - 1))
            CartItemRef.updateChildren(updates)
                holder.itemQuantity.text = (currentQuantity - 1).toString()
        }
    }

    private fun handleIncrease(holder: ViewHolderClass, data: CustomerCartDataClass,userId: String?) {
        val currentQuantity = holder.itemQuantity.text.toString().toInt()
        val CartItemRef = db.child(userId.toString()).child(shopId.toString()).child("Cart").child(data.key)
        val updates = mapOf("quantity" to (currentQuantity + 1))
        CartItemRef.updateChildren(updates)
        holder.itemQuantity.text = (currentQuantity + 1).toString()
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
    }
}