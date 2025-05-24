package com.rooshan.AsanKhredari.Adapter

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rooshan.AsanKhredari.DataClass.CustomerHomeDataClass
import com.rooshan.AsanKhredari.R

class CustomerHomeAdapter(
    private val context: Context,
    private var dataList: MutableList<CustomerHomeDataClass>
) : RecyclerView.Adapter<CustomerHomeAdapter.ViewHolderClass>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolderClass {
        val view = LayoutInflater.from(context).inflate(R.layout.customer_shop_item_layout, parent, false)
        return ViewHolderClass(view)
    }

    override fun onBindViewHolder(holder: ViewHolderClass, position: Int) {
        val currentItem = dataList[position]
        holder.shopName.text = currentItem.ShopName
        holder.shopType.text = currentItem.ShopType
        holder.shopAddress.text = currentItem.ShopAddress
        holder.item.setOnClickListener {
            val bundle = Bundle().apply {
                putString("id", currentItem.id)
                putString("ShopName", currentItem.ShopName)
                putString("ShopType", currentItem.ShopType)
                putString("ShopAddress", currentItem.ShopAddress)
            }
            it.findNavController().navigate(R.id.action_customerHome_to_customerShopItems, bundle)
        }
    }

    override fun getItemCount(): Int = dataList.size

    fun updateList(newList: MutableList<CustomerHomeDataClass>) {
        dataList = newList
        notifyDataSetChanged()
    }

    class ViewHolderClass(view: View) : RecyclerView.ViewHolder(view) {
        val shopType: TextView = view.findViewById(R.id.shopType)
        val shopName: TextView = view.findViewById(R.id.shopName)
        val shopAddress: TextView = view.findViewById(R.id.location)
        val item: View = view.findViewById(R.id.item)
    }
}
