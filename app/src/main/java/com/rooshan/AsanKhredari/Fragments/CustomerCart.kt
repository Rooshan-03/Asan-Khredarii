package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerCart : Fragment() {
    private var _binding: FragmentCustomerCartBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: CustomerCartAdapter
    private lateinit var dataList: MutableList<CustomerCartDataClass>
    private val db: DatabaseReference = FirebaseDatabase.getInstance().getReference("Customers")
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private var cartListener: ValueEventListener? = null
    private val progressDialog by lazy { createProgressDialog() }

    private fun createProgressDialog(): Dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.progress_dialoug)
        setCancelable(false)
    }

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
        binding.orderBtnWithPrice.setOnClickListener {
            placeOrder()
        }
    }

    private fun placeOrder() {
        val shopId = arguments?.getString("shopId") ?: return
        val userId = auth.currentUser?.uid ?: return

        if (dataList.isEmpty()) {
            if (isAdded) {
                Toast.makeText(requireContext(), "Cart is empty", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (isAdded) {
            progressDialog.show()
        }

        // Create order object
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val totalAmount = dataList.sumOf { it.itemUnitPrice.toDouble() * it.itemCartQuantity.toDouble() }
        val order = mapOf(
            "userId" to userId,
            "timestamp" to timestamp,
            "items" to dataList.map { item ->
                mapOf(
                    "itemName" to item.itemCartName,
                    "unitPrice" to item.itemUnitPrice.toDouble(),
                    "quantity" to item.itemCartQuantity.toLong(),
                    "unit" to item.itemCartUnit
                )
            },
            "totalAmount" to totalAmount
        )

        // Store order under Retailers/shopId/Orders
        val retailerRef = FirebaseDatabase.getInstance().getReference("Retailers").child(shopId).child("Orders")
        val orderId = retailerRef.push().key ?: return
        retailerRef.child(orderId).setValue(order)
            .addOnSuccessListener {
                // Clear the user's cart
                db.child(userId).child(shopId).child("Cart").removeValue()
                    .addOnSuccessListener {
                        if (isAdded) {
                            progressDialog.dismiss()
                            Toast.makeText(requireContext(), "Order placed", Toast.LENGTH_SHORT).show()
                            findNavController().popBackStack()
                        }
                    }
                    .addOnFailureListener {
                        if (isAdded) {
                            progressDialog.dismiss()
                            Toast.makeText(requireContext(), "Failed to clear cart", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
            .addOnFailureListener {
                if (isAdded) {
                    progressDialog.dismiss()
                    Toast.makeText(requireContext(), "Failed to place order", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun goToItemPage() {
        binding.backBtn.setOnClickListener {
            val id = arguments?.getString("shopId")
            if (id.isNullOrEmpty()) return@setOnClickListener

            if (isAdded) {
                progressDialog.show()
            }

            val userRef = FirebaseDatabase.getInstance().getReference("Retailers").child(id)
            userRef.get().addOnSuccessListener { snapshot ->
                if (isAdded) {
                    progressDialog.dismiss()
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
                        Toast.makeText(requireContext(), "Shop not found", Toast.LENGTH_SHORT).show()
                    }
                }
            }.addOnFailureListener {
                if (isAdded) {
                    progressDialog.dismiss()
                    Log.e("RetailerInfo", "Failed to retrieve shop info", it)
                    Toast.makeText(requireContext(), "Failed to load shop info", Toast.LENGTH_SHORT).show()
                }
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
            if (isAdded) {
                progressDialog.show()
            }
            val cartRef = db.child(userId).child(shopId).child("Cart")
            cartListener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (isAdded) {
                        progressDialog.dismiss()
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
                        adapter.notifyDataSetChanged()
                        Log.d("CustomerCart", "DataList size: ${dataList.size}, GrandTotal: $grandTotal")
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    if (isAdded) {
                        progressDialog.dismiss()
                        Log.e("CustomerCart", "Database error: ${error.message}")
                        _binding?.emptyStateView?.visibility = View.VISIBLE
                        Toast.makeText(requireContext(), "Failed to load cart", Toast.LENGTH_SHORT).show()
                    }
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
        progressDialog.dismiss()
        _binding = null
    }
}