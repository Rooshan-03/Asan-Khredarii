package com.rooshan.AsanKhredari.Adapter

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.rooshan.AsanKhredari.DataClass.CustomerHomeDataClass
import com.rooshan.AsanKhredari.R

class CustomerHomeAdapter(
    private val context: Context, private var dataList: MutableList<CustomerHomeDataClass>
) : RecyclerView.Adapter<CustomerHomeAdapter.ViewHolderClass>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolderClass {
        val view = View.inflate(context, R.layout.customer_shop_item_layout, null)
        return ViewHolderClass(view)
    }

    override fun onBindViewHolder(
        holder: ViewHolderClass,
        position: Int
    ) {
        var currentItem = dataList[position]
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

    override fun getItemCount(): Int {
        return dataList.size
    }

    class ViewHolderClass(view: View) : RecyclerView.ViewHolder(view) {
        val shopType = view.findViewById<TextView>(R.id.shopType)
        val shopName = view.findViewById<TextView>(R.id.shopName)
        val shopAddress = view.findViewById<TextView>(R.id.location)
        val item = view.findViewById<View>(R.id.item)
    }
}
