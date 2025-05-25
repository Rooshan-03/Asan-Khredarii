package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.rooshan.AsanKhredari.Adapter.CustomerShopItemsAdapter
import com.rooshan.AsanKhredari.DataClass.CustomerShopItemsDataClass
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentCustomerShopItemsBinding

class CustomerShopItems : Fragment() {
    private var _binding: FragmentCustomerShopItemsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase
    private lateinit var adapter: CustomerShopItemsAdapter
    private lateinit var dataList: MutableList<CustomerShopItemsDataClass>
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCustomerShopItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            auth = FirebaseAuth.getInstance()
            database = FirebaseDatabase.getInstance()
            if (auth.currentUser != null) {
                goToCart()
                setUpDashboard()
                setUpRecyclerView()
                loadShopItem()
                val currentUid = auth.uid.toString()
                Log.d("Home", "Authenticated user UID: $currentUid")
            } else {
                Log.w("Home", "No authenticated user found")
                Toast.makeText(context, "Please log in to view items", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun goToCart() {
        binding.cartIcon.setOnClickListener {
            val shopId= arguments?.getString("id")
            findNavController().navigate(R.id.action_customerShopItems_to_customerCart, bundleOf("shopId" to shopId))
        }
    }

    private fun setUpDashboard() {
        val shopName = arguments?.getString("ShopName")
        val shopType = arguments?.getString("ShopType")
        val shopAddress = arguments?.getString("ShopAddress")
        binding.shopName.text = shopName ?: "Unknown Shop"
        binding.shopAddress.text = shopAddress ?: "Unknown Address"
        binding.shopType.text = shopType ?: "Unknown Type"
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun loadShopItem() {
        val retailerId = arguments?.getString("id")
        if (auth.currentUser != null && retailerId != null) {
            val itemsRef = database.reference.child("Retailers").child(retailerId).child("items")

            itemsRef.addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    dataList.clear()
                    Log.d("FirebaseData", "Snapshot: ${snapshot.value}")

                    if (!snapshot.exists()) {
                        binding.emptyStateText.visibility = View.VISIBLE
                        binding.itemsRecyclerView.visibility = View.GONE
                        return
                    }

                    for (itemSnapshot in snapshot.children) {
                        try {
                            val idString = itemSnapshot.key ?: continue
                            val itemName =
                                itemSnapshot.child("itemName").getValue(String::class.java)
                            val itemPrice = itemSnapshot.child("price").getValue(Double::class.java)
                            val itemDeliveryPrice =
                                itemSnapshot.child("deliveryPrice").getValue(Double::class.java)
                            val quantityRaw = itemSnapshot.child("quantity").value
                            val quantity = when (quantityRaw) {
                                is Long -> quantityRaw
                                is String -> quantityRaw.toLongOrNull()
                                else -> null
                            }
                            val unit = itemSnapshot.child("unit").getValue(String::class.java)
                            val total =
                                itemSnapshot.child("totalPrice").getValue(Double::class.java)
                            val key = itemSnapshot.key ?: continue

                            if (itemName != null && itemPrice != null && itemDeliveryPrice != null &&
                                quantity != null && unit != null && total != null
                            ) {
                                dataList += CustomerShopItemsDataClass(
                                    key = key,
                                    itemName = itemName,
                                    price = itemPrice,
                                    deliveryPrice = itemDeliveryPrice,
                                    quantity = quantity,
                                    totalPrice = total,
                                    unit = unit
                                )
                            } else {
                                Log.w(
                                    "FirebaseData",
                                    "Skipping item $idString due to invalid fields"
                                )
                            }
                        } catch (e: Exception) {
                            Log.e("RTDBError", "Error parsing item: ${itemSnapshot.key}", e)
                        }
                    }

                    Log.d("FirebaseData", "dataList size after update: ${dataList.size}")
                    adapter.notifyDataSetChanged()
                    binding.emptyStateText.visibility =
                        if (dataList.isEmpty()) View.VISIBLE else View.GONE
                    binding.itemsRecyclerView.visibility =
                        if (dataList.isEmpty()) View.GONE else View.VISIBLE
                }

                override fun onCancelled(error: DatabaseError) {
                    Log.e("RTDBError", "Failed to fetch items", error.toException())
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.itemsRecyclerView.visibility = View.GONE
                    Toast.makeText(
                        context,
                        "Failed to load items: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
        } else {
            Log.w("FirebaseData", "Invalid retailerId or user not authenticated")
            binding.emptyStateText.visibility = View.VISIBLE
            binding.itemsRecyclerView.visibility = View.GONE
        }
    }

    private fun setUpRecyclerView() {
        val id = arguments?.getString("id")
        dataList = mutableListOf()
        recyclerView = binding.itemsRecyclerView
        adapter = CustomerShopItemsAdapter(id, requireContext(), dataList)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}