package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.MenuProvider
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ItemDecoration
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.rooshan.AsanKhredari.Adapter.CustomerHomeAdapter
import com.rooshan.AsanKhredari.DataClass.CustomerHomeDataClass
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentCustomerHomeBinding

class CustomerHome : Fragment() {
    private var _binding: FragmentCustomerHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: FirebaseFirestore
    private lateinit var dataList: MutableList<CustomerHomeDataClass>
    private lateinit var adapter: CustomerHomeAdapter
    private lateinit var recyclerView: RecyclerView
    private var firestoreListener: ListenerRegistration? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    @SuppressLint("MissingInflatedId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val toolbar = binding.toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)
        sharedPreferences = requireContext().getSharedPreferences("MyPrefs", 0)
        var username = sharedPreferences.getString("username", "")
        username?.isEmpty()?.let {
            if (!it)
                binding.name.text = username
        }
        ChangeName()
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.option_menu, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.signOut -> {
                        auth.signOut()
                        sharedPreferences.edit { clear() }
                        true
                    }

                    R.id.deleteAccount -> {
                        auth.currentUser?.delete()
                        sharedPreferences.edit { clear() }
                        true
                    }

                    else -> {
                        false
                    }
                }
            }
        }, viewLifecycleOwner)
        auth = FirebaseAuth.getInstance()
        setupRecyclerView()
        loadShopsData()
    }

    private fun ChangeName() {
        binding.editName.setOnClickListener {
            if (!isAdded || auth.currentUser == null) {
                Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT)
                    .show()
                return@setOnClickListener
            }
            val builder = AlertDialog.Builder(requireContext())
            builder.setTitle("Edit Name")
            val view = LayoutInflater.from(requireContext()).inflate(R.layout.update_name, null)
            val newName =
                view.findViewById<com.google.android.material.textfield.TextInputEditText>(R.id.editName)
            val cancel =
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.cancel)
            val editNameBtn =
                view.findViewById<com.google.android.material.button.MaterialButton>(R.id.editNameBtn)
            builder.setView(view)
            val dialog = builder.create()
            dialog.show()

            cancel.setOnClickListener {
                dialog.dismiss()
            }

            editNameBtn.setOnClickListener {

            }
        }
    }

    private fun setupRecyclerView() {
        dataList = mutableListOf()
        recyclerView = binding.recyclerView
        recyclerView.addItemDecoration(com.rooshan.AsanKhredari.ItemDecoration(20))
        adapter = CustomerHomeAdapter(requireContext(), dataList)
        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.adapter = adapter
    }

    @SuppressLint("NotifyDataSetChanged")

    private fun loadShopsData() {
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        if (auth.currentUser == null) {
            binding.emptyStateView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            Snackbar.make(binding.root, "Please sign in to view shops", Snackbar.LENGTH_LONG).show()
            return
        }

        db.collection("Retailer").get().addOnSuccessListener {
            dataList.clear()
            if (it.isEmpty) {
                binding.emptyStateView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
            } else {
                binding.emptyStateView.visibility = View.GONE
                binding.recyclerView.visibility = View.VISIBLE
                for (document in it) {
                    val id = document.id
                    val ShopType = document.getString("ShopType")
                    val ShopName = document.getString("ShopName")
                    val ShopAddress = document.getString("Location")
                    if (ShopType != null && ShopName != null && ShopAddress != null) {
                        dataList += CustomerHomeDataClass(id, ShopName, ShopType, ShopAddress)
                    }
                }
                adapter.notifyDataSetChanged()
            }
        }.addOnFailureListener { e ->
            Log.e("HomeFragment", "Error loading shops", e)
            binding.emptyStateView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            Snackbar.make(binding.root, "Failed to load shops: ${e.message}", Snackbar.LENGTH_LONG).show()
        }
    }
    override fun onDestroyView() {
        firestoreListener?.remove()
        super.onDestroyView()
        _binding = null
    }
}
