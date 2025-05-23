package com.rooshan.AsanKhredari.Adapter

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.rooshan.AsanKhredari.DataClass.CustomerShopItemsDataClass
import com.rooshan.AsanKhredari.R
import android.util.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

class CustomerShopItemsAdapter (
        private val context: Context,
        private val dataList: MutableList<CustomerShopItemsDataClass>
    ) : RecyclerView.Adapter<CustomerShopItemsAdapter.ViewHolderClass>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): ViewHolderClass {
            val view= LayoutInflater.from(parent.context).inflate(R.layout.product_item_customer_layout, null)
            return ViewHolderClass(view)
        }

        @OptIn(ExperimentalEncodingApi::class)
        @SuppressLint("DefaultLocale")
        override fun onBindViewHolder(
            holder: ViewHolderClass,
            position: Int
        ) {
            val currentItem= dataList[position]

            currentItem.imageBase64?.let { base64String ->
                try {
                    val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
                    val bitmap = BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.size)
                    holder.itemImg.setImageBitmap(bitmap)
                } catch (e: Exception) {
                    Log.e("Adapter", "Error decoding image for item ${currentItem.id}: ${e.message}", e)
                    holder.itemImg.setImageResource(android.R.drawable.ic_menu_camera)
                }
            } ?: holder.itemImg.setImageResource(android.R.drawable.ic_menu_camera)


            holder.itemName.text=currentItem.itemName
            holder.itemPrice.text = String.format("Rs%.1f", currentItem.price)
            holder.itemDeliveryPrice.text = String.format("Rs%.1f", currentItem.deliveryPrice)
            holder.itemQuantity.text=currentItem.quantity
            holder.itemUnit.text=currentItem.unit
            holder.itemTotalPrice.text = String.format("Rs%.1f", currentItem.totalPrice)
            holder.itemCartBtn.setOnClickListener {
            }
        }

        override fun getItemCount(): Int {
            return dataList.size
        }

        inner class ViewHolderClass(view: View): RecyclerView.ViewHolder(view) {
            val itemName: TextView=view.findViewById(R.id.itemName)
            val itemPrice: TextView=view.findViewById(R.id.itemPrice)
            val itemDeliveryPrice: TextView=view.findViewById(R.id.itemDeliveryPrice)
            val itemQuantity: TextView=view.findViewById(R.id.itemQuantity)
            val itemUnit: TextView=view.findViewById(R.id.itemUnit)
            val itemTotalPrice: TextView= view.findViewById(R.id.itemTotalPrice)
            val itemCartBtn: Button =view.findViewById(R.id.addToCartButton)
            val itemImg : ImageView = view.findViewById(R.id.itemImg)
        }

    }
