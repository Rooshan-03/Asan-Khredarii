package com.rooshan.AsanKhredari.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.rooshan.AsanKhredari.DataClass.RetailerPendingOrderDataClass
import com.rooshan.AsanKhredari.R

class RetailerPendingOrderAdapter(
    var context: Context,
    var dataList: MutableList<RetailerPendingOrderDataClass>
) :
    RecyclerView.Adapter<RetailerPendingOrderAdapter.ViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RetailerPendingOrderAdapter.ViewHolder {
        val view = LayoutInflater.from(context)
            .inflate(R.layout.retailer_pending_orders_layout, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: RetailerPendingOrderAdapter.ViewHolder,
        position: Int
    ) {
        val data = dataList[position]
        holder.Name.text = data.userId
        holder.Price.text = data.price
        holder.TimeStamp.text = data.timestamp
        holder.AcceptBtn.setOnClickListener {
            // Handle accept button click
        }
        holder.RejectBtn.setOnClickListener {
            // Handle reject button click
        }
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val Name = view.findViewById<TextView>(R.id.userName)
        val Price = view.findViewById<TextView>(R.id.price)
        val TimeStamp = view.findViewById<TextView>(R.id.timeStamp)
        val AcceptBtn: MaterialButton = view.findViewById(R.id.acceptBtn)
        val RejectBtn: MaterialButton = view.findViewById(R.id.RejectBtn)
    }
}