package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase // Import for Realtime Database
import com.google.firebase.database.DatabaseError // Import for Realtime Database error
import com.google.firebase.database.DataSnapshot // Import for Realtime Database data snapshot
import com.rooshan.AsanKhredari.Adapter.CustomerHomeAdapter
import com.rooshan.AsanKhredari.DataClass.CustomerHomeDataClass
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentCustomerHomeBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.tasks.await // For await() on RTDB tasks

class CustomerHome : Fragment() {
    private var _binding: FragmentCustomerHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: FirebaseDatabase // Changed to FirebaseDatabase
    private lateinit var dataList: MutableList<CustomerHomeDataClass>
    private lateinit var adapter: CustomerHomeAdapter
    private lateinit var recyclerView: RecyclerView
    // Removed firestoreListener as it's specific to Firestore snapshots.
    // For RTDB, if real-time listeners were used, they would be managed differently.

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

        // Setup Toolbar
        val toolbar = binding.toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        // Init Firebase Auth and Realtime Database
        sharedPreferences = requireContext().getSharedPreferences("Details", 0)
        db = FirebaseDatabase.getInstance() // Initialize Firebase Realtime Database
        auth = FirebaseAuth.getInstance()

        // Set saved username from SharedPreferences
        val username = sharedPreferences.getString("UserName", "")
        if (!username.isNullOrEmpty()) {
            binding.name.text = username
        } else {
            // If username is not in SharedPreferences, try to fetch from Realtime Database
            fetchUserNameFromDatabase()
        }

        // Change Name Button
        setupChangeNameDialog()

        // Toolbar Menu
        setupMenu()

        // Recycler Setup
        setupRecyclerView()
        loadShopsData()
    }

    // Function to fetch username from Realtime Database if not in SharedPreferences
    private fun fetchUserNameFromDatabase() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            db.getReference("Roles").child(uid).child("UserName").get()
                .addOnSuccessListener { dataSnapshot ->
                    val username = dataSnapshot.getValue(String::class.java)
                    if (!username.isNullOrEmpty()) {
                        binding.name.text = username
                        sharedPreferences.edit {
                            putString("UserName", username)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CustomerHome", "Failed to fetch username from DB: ${e.message}", e)
                }
        }
    }

    private fun setupChangeNameDialog() {
        binding.editName.setOnClickListener {
            if (auth.currentUser == null) {
                Toast.makeText(requireContext(), "User not authenticated", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.update_name, null)
            val newNameInput = dialogView.findViewById<TextInputEditText>(R.id.editName)
            val cancelBtn = dialogView.findViewById<MaterialButton>(R.id.cancel)
            val saveBtn = dialogView.findViewById<MaterialButton>(R.id.editNameBtn)

            val dialog = AlertDialog.Builder(requireContext())
                .setView(dialogView)
                .setCancelable(false)
                .create()

            cancelBtn.setOnClickListener { dialog.dismiss() }

            saveBtn.setOnClickListener {
                val newName = newNameInput.text.toString().trim()
                if (newName.isEmpty()) {
                    newNameInput.error = "Enter Name"
                } else {
                    val uid = auth.currentUser?.uid ?: run {
                        Toast.makeText(requireContext(), "User not logged in.", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    // Update in Realtime Database
                    db.getReference("Roles").child(uid).child("UserName").setValue(newName)
                        .addOnSuccessListener {
                            binding.name.text = newName
                            sharedPreferences.edit {
                                putString("UserName", newName)
                            }
                            Toast.makeText(requireContext(), "Name updated", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "Update failed: ${it.message}", Toast.LENGTH_SHORT).show()
                        }
                }
            }

            dialog.show()
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
        if (auth.currentUser == null) {
            binding.emptyStateView.visibility = View.VISIBLE
            binding.recyclerView.visibility = View.GONE
            Snackbar.make(binding.root, "Please sign in to view shops", Snackbar.LENGTH_LONG).show()
            return
        }

        // Fetch data from Realtime Database
        db.getReference("Retailers").get()
            .addOnSuccessListener { dataSnapshot ->
                dataList.clear()
                if (!dataSnapshot.exists() || dataSnapshot.childrenCount == 0L) {
                    binding.emptyStateView.visibility = View.VISIBLE
                    binding.recyclerView.visibility = View.GONE
                } else {
                    binding.emptyStateView.visibility = View.GONE
                    binding.recyclerView.visibility = View.VISIBLE
                    for (shopSnapshot in dataSnapshot.children) {
                        val id = shopSnapshot.key // Get the key as the ID
                        val name = shopSnapshot.child("ShopName").getValue(String::class.java)
                        val type = shopSnapshot.child("ShopType").getValue(String::class.java)
                        val location = shopSnapshot.child("Location").getValue(String::class.java)

                        if (id != null && name != null && type != null && location != null) {
                            dataList.add(CustomerHomeDataClass(id, name, type, location))
                        } else {
                            Log.w("CustomerHome", "Skipping shop with missing data: ${shopSnapshot.key}")
                        }
                    }
                    adapter.notifyDataSetChanged()
                }
            }
            .addOnFailureListener {
                Log.e("CustomerHome", "Failed to load shops from RTDB", it)
                binding.emptyStateView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                Snackbar.make(binding.root, "Failed to load shops: ${it.message}", Snackbar.LENGTH_LONG).show()
            }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.option_menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.signOut -> {
                        auth.signOut()
                        sharedPreferences.edit { clear() }
                        Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()
                        true
                    }
                    R.id.deleteAccount -> {
                        auth.currentUser?.delete()
                            ?.addOnSuccessListener {
                                sharedPreferences.edit { clear() }
                                Toast.makeText(requireContext(), "Account deleted", Toast.LENGTH_SHORT).show()
                            }
                            ?.addOnFailureListener { e ->
                                Toast.makeText(requireContext(), "Account deletion failed: ${e.message}", Toast.LENGTH_LONG).show()
                                Log.e("CustomerHome", "Account deletion failed", e)
                            }
                        true
                    }
                    else -> false
                }
            }
        }, viewLifecycleOwner)
    }

    override fun onDestroyView() {
        // No firestoreListener to remove for RTDB one-time fetches.
        // If real-time listeners were added, their removal would go here.
        super.onDestroyView()
        _binding = null
    }
}
