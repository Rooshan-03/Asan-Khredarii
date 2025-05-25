package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.rooshan.AsanKhredari.Adapter.CustomerCartAdapter
import com.rooshan.AsanKhredari.DataClass.CustomerCartDataClass
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentCustomerCartBinding

class CustomerCart : Fragment() {
    private var _binding: FragmentCustomerCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CustomerCartAdapter
    private lateinit var dataList: MutableList<CustomerCartDataClass>
    private val db: DatabaseReference = FirebaseDatabase.getInstance().getReference("Customers")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var cartListener: ValueEventListener? = null

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
        goToItemPage()
    }

    private fun goToItemPage() {
        binding.backBtn.setOnClickListener {
            val id = arguments?.getString("shopId")
            if (id.isNullOrEmpty()) return@setOnClickListener

            val userRef = FirebaseDatabase.getInstance().getReference("Retailers").child(id)
            userRef.get().addOnSuccessListener { snapshot ->
                Log.d("ShopID", "Trying to fetch shop with ID: $id")
                if (snapshot.exists()) {
                    val name = snapshot.child("ShopName").getValue(String::class.java) ?: "Unknown Name"
                    val type = snapshot.child("ShopType").getValue(String::class.java) ?: "Unknown Type"
                    val address = snapshot.child("Location").getValue(String::class.java) ?: "Unknown Address"

                    Log.d("RetailerInfo", "Name: $name, Type: $type, Address: $address")

                    val bundle = Bundle().apply {
                        putString("id", id)
                        putString("ShopName", name)
                        putString("ShopType", type)
                        putString("ShopAddress", address)
                    }

                    findNavController().navigate(R.id.action_customerCart_to_customerShopItems, bundle)
                } else {
                    Log.e("RetailerInfo", "Snapshot does not exist for id: $id")
                }
            }.addOnFailureListener {
                Log.e("RetailerInfo", "Failed to retrieve shop info", it)
            }
        }
    }


    private fun setUpRecyclerView() {
        val shopId = arguments?.getString("shopId")
        dataList = mutableListOf()
        adapter = CustomerCartAdapter(shopId, dataList, requireContext())
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter
    }

    @SuppressLint("DefaultLocale")
    private fun loadCart() {
        val shopId = arguments?.getString("shopId")
        val userId = auth.currentUser?.uid ?: return

        if (shopId != null) {
            val cartRef = db.child(userId).child(shopId).child("Cart")
            cartListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dataList.clear()
                    var grandTotal = 0.0
                    for (itemSnapshot in snapshot.children) {
                        val key = itemSnapshot.key ?: continue
                        val itemCartName = itemSnapshot.child("itemName").getValue(String::class.java)
                        val unitPrice = itemSnapshot.child("totalPrice").getValue(Double::class.java)
                        val quantity = itemSnapshot.child("quantity").getValue(Long::class.java)
                        val itemCartUnit = itemSnapshot.child("unit").getValue(String::class.java)

                        if (itemCartName != null && unitPrice != null && quantity != null && itemCartUnit != null) {
                            val item = CustomerCartDataClass(
                                key = key,
                                itemCartName = itemCartName,
                                itemUnitPrice = unitPrice.toString(),
                                itemCartQuantity = quantity.toString(),
                                itemCartUnit = itemCartUnit
                            )
                            dataList.add(item)
                            grandTotal += unitPrice * quantity
                            Log.d("CustomerCart", "Added item: $itemCartName, Price: $unitPrice, Quantity: $quantity")
                        } else {
                            Log.w("CustomerCart", "Skipping item $key due to missing fields")
                        }
                    }
                    _binding?.let { binding ->
                        binding.orderBtnWithPrice.text = String.format("Place Order (Rs%.2f)", grandTotal)
                        binding.emptyStateView.visibility = if (dataList.isEmpty()) View.VISIBLE else View.GONE
                        binding.recyclerView.visibility = if (dataList.isEmpty()) View.GONE else View.VISIBLE
                    }
                    adapter.notifyDataSetChanged() // Notify adapter of full data change
                    Log.d("CustomerCart", "DataList size: ${dataList.size}, GrandTotal: $grandTotal")
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("CustomerCart", "Database error: ${error.message}")
                    _binding?.emptyStateView?.visibility = View.VISIBLE
                }
            }
            cartListener?.let { cartRef.addValueEventListener(it) }
        } else {
            Log.e("CustomerCart", "ShopId is null")
            _binding?.emptyStateView?.visibility = View.VISIBLE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        val shopId = arguments?.getString("shopId")
        val userId = auth.currentUser?.uid
        if (shopId != null && userId != null && cartListener != null) {
            db.child(userId).child(shopId).child("Cart").removeEventListener(cartListener!!)
        }
        cartListener = null
        _binding = null
    }
}