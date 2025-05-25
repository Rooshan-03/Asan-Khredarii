package com.rooshan.AsanKhredari.Fragments

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener
import com.rooshan.AsanKhredari.Adapter.RetailerPendingOrderAdapter
import com.rooshan.AsanKhredari.DataClass.RetailerPendingOrderDataClass
import com.rooshan.AsanKhredari.databinding.FragmentRetailerPendingOrdersBinding
import android.widget.Toast

class RetailerPendingOrders : Fragment() {
    private var _binding: FragmentRetailerPendingOrdersBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var dataList: MutableList<RetailerPendingOrderDataClass>
    private lateinit var adapter: RetailerPendingOrderAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentRetailerPendingOrdersBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        auth.addAuthStateListener {
            if (it.currentUser != null) {
                loadOrders()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        loadOrders()
    }

    private fun setupRecyclerView() {
        dataList = mutableListOf()
        adapter = RetailerPendingOrderAdapter(requireContext(), dataList)
        binding.recyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerView.adapter = adapter
    }

    private fun loadOrders() {
        if (auth.currentUser == null) {
            Toast.makeText(requireContext(), "Please log in to view orders", Toast.LENGTH_SHORT).show()
            binding.recyclerView.visibility = View.GONE
            return
        }

        val currentUserId = auth.currentUser!!.uid
        val ordersRef = FirebaseDatabase.getInstance().reference
            .child("Retailers").child(currentUserId).child("Orders")
        Log.d("RetailerPendingOrders", "Orders reference: $ordersRef")
        ordersRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                dataList.clear()
                Log.d("RetailerPendingOrders", "Snapshot: ${dataSnapshot.value}")
                for (orderSnapshot in dataSnapshot.children) {
                    val order = orderSnapshot.getValue(RetailerPendingOrderDataClass::class.java)
                    Log.d("RetailerPendingOrders", "Order: $order")
                    order?.let { dataList.add(it) }
                }
                if (dataList.isNotEmpty()) {
                    binding.recyclerView.visibility = View.VISIBLE
                } else {
                    binding.recyclerView.visibility = View.GONE
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to load orders: ${error.message}", Toast.LENGTH_SHORT).show()
                binding.recyclerView.visibility = View.GONE
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        auth.removeAuthStateListener { /* No-op */ }
        _binding = null
    }
}