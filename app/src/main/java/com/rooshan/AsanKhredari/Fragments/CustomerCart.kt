package com.rooshan.AsanKhredari.Fragments

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.rooshan.AsanKhredari.Adapter.CustomerCartAdapter
import com.rooshan.AsanKhredari.DataClass.CustomerCartDataClass
import com.rooshan.AsanKhredari.databinding.FragmentCustomerCartBinding

class CustomerCart : Fragment() {
    private var _binding: FragmentCustomerCartBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: CustomerCartAdapter
    private lateinit var dataList: MutableList<CustomerCartDataClass>

    private val db: DatabaseReference = FirebaseDatabase.getInstance().getReference("Customers")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        loadCart()
    }

    private fun setUpRecyclerView() {
        val shopId = arguments?.getString("shopId")
        dataList = mutableListOf()

        adapter = CustomerCartAdapter(
            shopId,
            dataList,
            requireContext()
        )
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    private fun loadCart() {
        val shopId = arguments?.getString("shopId")
        val userId = auth.currentUser?.uid ?: return

        if (shopId != null) {
            val cartRef = db.child(userId).child(shopId).child("Cart")
            cartRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dataList.clear()
                    for (itemSnapshot in snapshot.children) {
                        val key = itemSnapshot.key ?: continue
                        val itemCartName = itemSnapshot.child("itemName").getValue(String::class.java)
                        val itemCartTotalPrice =
                            itemSnapshot.child("totalPrice").getValue(Double::class.java)
                        val itemCartQuantity = itemSnapshot.child("quantity").value
                        val itemCartUnit = itemSnapshot.child("unit").getValue(String::class.java)

                        if (itemCartName != null && itemCartTotalPrice != null &&
                            itemCartQuantity != null && itemCartUnit != null) {

                            dataList.add(
                                CustomerCartDataClass(
                                    key = key,
                                    itemCartName = itemCartName,
                                    itemCartTotalPrice = itemCartTotalPrice.toString(),
                                    itemCartQuantity = itemCartQuantity.toString(),
                                    itemCartUnit = itemCartUnit
                                )
                            )
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: com.google.firebase.database.DatabaseError) {

                }
            })
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}