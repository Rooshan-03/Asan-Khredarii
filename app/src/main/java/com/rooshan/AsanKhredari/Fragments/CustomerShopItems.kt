package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.rooshan.AsanKhredari.Adapter.CustomerShopItemsAdapter
import com.rooshan.AsanKhredari.DataClass.CustomerShopItemsDataClass
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentCustomerShopItemsBinding
import kotlinx.coroutines.launch

class CustomerShopItems : Fragment() {
    private var _binding: FragmentCustomerShopItemsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var adapter: CustomerShopItemsAdapter
    private lateinit var dataList: MutableList<CustomerShopItemsDataClass>
    private lateinit var recyclerView: RecyclerView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentCustomerShopItemsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.apply {
            auth = FirebaseAuth.getInstance()
            db = FirebaseFirestore.getInstance()
            if (auth.currentUser != null) {
                setUpDashboard()
                setUpRecyclerView()
                loadShopItem()
                val currentUid = auth.uid.toString()
                Log.d("Home", "Authenticated user UID: $currentUid")
            }
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
        val id = arguments?.getString("id")
        if (auth.currentUser != null) {
            db.collection("Retailer").document(id.toString()).collection("items").get()
                .addOnSuccessListener {
                    dataList.clear()
                    if (it.isEmpty) {
                        binding.emptyStateText.visibility = View.VISIBLE
                        binding.itemsRecyclerView.visibility = View.GONE
                    }
                    for (document in it) {
                        val id = document.id
                        val itemName = document.getString("itemName")
                        val itemPrice = document.getDouble("itemPrice")
                        val itemDeliveryPrice = document.getDouble("deliveryPrice")
                        val itemImage = document.getString("imageBase64")
                        val quantity = document.getString("quantity")
                        val unit = document.getString("unit")
                        val total = document.getDouble("totalPrice")
                        if (itemName != null && itemPrice != null && itemDeliveryPrice != null && itemImage != null && quantity != null && unit != null && total != null) {
                            dataList += CustomerShopItemsDataClass(
                                id = id,
                                itemName = itemName,
                                price = itemPrice,
                                deliveryPrice = itemDeliveryPrice,
                                quantity = quantity,
                                totalPrice = total,
                                unit = unit,
                                imageBase64 = itemImage
                            )
                        }
                        adapter.notifyDataSetChanged()
                    }
                    binding.emptyStateText.visibility = View.GONE
                    binding.itemsRecyclerView.visibility = View.VISIBLE
                }
                .addOnFailureListener {
                    e -> Log.e("FirestoreError", "Failed to fetch", e)
                    binding.emptyStateText.visibility = View.VISIBLE
                    binding.itemsRecyclerView.visibility = View.GONE
                }

        }
    }

    private fun setUpRecyclerView() {
        dataList = mutableListOf()
        recyclerView = binding.itemsRecyclerView
        adapter = CustomerShopItemsAdapter(requireContext(), dataList.toMutableList())
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }
}
