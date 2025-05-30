package com.rooshan.AsanKhredari.Fragments

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.database.FirebaseDatabase
import com.rooshan.AsanKhredari.Adapter.CustomerHomeAdapter
import com.rooshan.AsanKhredari.DataClass.CustomerHomeDataClass
import com.rooshan.AsanKhredari.R
import com.rooshan.AsanKhredari.databinding.FragmentCustomerHomeBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class CustomerHome : Fragment() {
    private var _binding: FragmentCustomerHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var db: FirebaseDatabase
    private lateinit var dataList: MutableList<CustomerHomeDataClass>
    private lateinit var adapter: CustomerHomeAdapter
    private lateinit var recyclerView: RecyclerView
    private val progressDialog by lazy { createProgressDialog() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCustomerHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    private fun createProgressDialog(): Dialog = Dialog(requireContext()).apply {
        setContentView(R.layout.progress_dialoug)
        setCancelable(false)
    }

    @SuppressLint("MissingInflatedId")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Setup Toolbar
        val toolbar = binding.toolbar
        (requireActivity() as AppCompatActivity).setSupportActionBar(toolbar)

        // Init Firebase Auth and Realtime Database
        sharedPreferences = requireContext().getSharedPreferences("Details", 0)
        db = FirebaseDatabase.getInstance()
        auth = FirebaseAuth.getInstance()

        // Set saved username from SharedPreferences
        val username = sharedPreferences.getString("UserName", "")
        if (!username.isNullOrEmpty()) {
            binding.name.text = username
        } else {
            // Fetch from Realtime Database if not in SharedPreferences
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

    private fun fetchUserNameFromDatabase() {
        val uid = auth.currentUser?.uid
        if (uid != null && isAdded) {
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    progressDialog.show()
                    val dataSnapshot = db.getReference("Roles").child(uid).child("UserName").get().await()
                    val username = dataSnapshot.getValue(String::class.java)
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            if (!username.isNullOrEmpty()) {
                                binding.name.text = username
                                sharedPreferences.edit {
                                    putString("UserName", username)
                                }
                            }
                            progressDialog.dismiss()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            Log.e("CustomerHome", "Failed to fetch username from DB: ${e.message}", e)
                            progressDialog.dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun setupChangeNameDialog() {
        binding.editName.setOnClickListener {
            if (auth.currentUser == null) {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
                }
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
                        if (isAdded) {
                            Toast.makeText(requireContext(), "Not signed in", Toast.LENGTH_SHORT).show()
                        }
                        return@setOnClickListener
                    }

                    if (isAdded) {
                        progressDialog.show()
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                db.getReference("Roles").child(uid).child("UserName").setValue(newName).await()
                                withContext(Dispatchers.Main) {
                                    if (isAdded) {
                                        binding.name.text = newName
                                        sharedPreferences.edit {
                                            putString("UserName", newName)
                                        }
                                        Toast.makeText(requireContext(), "Name updated", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                        progressDialog.dismiss()
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    if (isAdded) {
                                        Toast.makeText(requireContext(), "Failed to update name", Toast.LENGTH_SHORT).show()
                                        progressDialog.dismiss()
                                    }
                                }
                            }
                        }
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
            if (isAdded) {
                binding.emptyStateView.visibility = View.VISIBLE
                binding.recyclerView.visibility = View.GONE
                Toast.makeText(requireContext(), "Please sign in", Toast.LENGTH_SHORT).show()
            }
            return
        }

        if (isAdded) {
            progressDialog.show()
            CoroutineScope(Dispatchers.Main).launch {
                try {
                    val dataSnapshot = db.getReference("Retailers").get().await()
                    dataList.clear()
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            if (!dataSnapshot.exists() || dataSnapshot.childrenCount == 0L) {
                                binding.emptyStateView.visibility = View.VISIBLE
                                binding.recyclerView.visibility = View.GONE
                            } else {
                                binding.emptyStateView.visibility = View.GONE
                                binding.recyclerView.visibility = View.VISIBLE
                                for (shopSnapshot in dataSnapshot.children) {
                                    val id = shopSnapshot.key
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
                            progressDialog.dismiss()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        if (isAdded) {
                            Log.e("CustomerHome", "Failed to load shops from RTDB", e)
                            binding.emptyStateView.visibility = View.VISIBLE
                            binding.recyclerView.visibility = View.GONE
                            Toast.makeText(requireContext(), "Failed to load shops", Toast.LENGTH_SHORT).show()
                            progressDialog.dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.customer_option_menu, menu)
            }

            override fun onMenuItemSelected(item: MenuItem): Boolean {
                return when (item.itemId) {
                    R.id.signOut -> {
                        implementSignOut()
                        true
                    }
                    R.id.deleteAccount -> {
                        implementDeleteAccount()
                        true
                    }
                    R.id.pendingOrders -> {
                        findNavController().navigate(R.id.action_customerHome_to_customersPendingOrders)
                        true
                    }
                    else -> false
                }
            }

            private fun implementDeleteAccount() {
                val currentUser = auth.currentUser
                if (currentUser == null) {
                    if (isAdded) {
                        Toast.makeText(context, "Not signed in", Toast.LENGTH_SHORT).show()
                    }
                    return
                }

                val builder = AlertDialog.Builder(context)
                builder.setTitle("Delete Account")
                builder.setMessage("This will permanently delete your account and all associated data. Are you sure?")
                val view = LayoutInflater.from(context).inflate(R.layout.delete_account_layout, null)
                val pass = view.findViewById<TextInputEditText>(R.id.pass)
                val email = view.findViewById<TextInputEditText>(R.id.email)
                val deleteAccount = view.findViewById<MaterialButton>(R.id.deleteAccount)
                val cancel = view.findViewById<MaterialButton>(R.id.cancel)
                builder.setView(view)
                val dialog = builder.create()
                dialog.show()

                cancel.setOnClickListener {
                    dialog.dismiss()
                }

                deleteAccount.setOnClickListener {
                    val emailInput = email.text.toString().trim()
                    val password = pass.text.toString().trim()

                    if (emailInput.isEmpty()) {
                        if (isAdded) {
                            Toast.makeText(context, "Enter your email", Toast.LENGTH_SHORT).show()
                        }
                        return@setOnClickListener
                    }
                    if (password.isEmpty()) {
                        if (isAdded) {
                            Toast.makeText(context, "Enter your password", Toast.LENGTH_SHORT).show()
                        }
                        return@setOnClickListener
                    }
                    if (emailInput != currentUser.email) {
                        if (isAdded) {
                            Toast.makeText(context, "Email does not match", Toast.LENGTH_SHORT).show()
                        }
                        return@setOnClickListener
                    }
                    if (isAdded) {
                        progressDialog.show()
                        CoroutineScope(Dispatchers.Main).launch {
                            try {
                                val credential = EmailAuthProvider.getCredential(emailInput, password)
                                currentUser.reauthenticate(credential).await()
                                db.reference.child("Customers").child(auth.uid.toString()).removeValue().await()
                                currentUser.delete().await()
                                sharedPreferences.edit { clear() }
                                withContext(Dispatchers.Main) {
                                    if (isAdded) {
                                        progressDialog.dismiss()
                                        Toast.makeText(context, "Account deleted", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                        findNavController().navigate(R.id.action_customerHome_to_logIn)
                                    }
                                }
                            } catch (e: Exception) {
                                withContext(Dispatchers.Main) {
                                    if (isAdded) {
                                        progressDialog.dismiss()
                                        val message = when {
                                            e.message?.contains("credential is incorrect") == true -> "Incorrect password"
                                            e.message?.contains("no user record") == true -> "No user found"
                                            else -> "Failed to delete account"
                                        }
                                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        }
                    }
                }
            }

            private fun implementSignOut() {
                val builder = AlertDialog.Builder(requireContext())
                builder.setTitle("Sign Out")
                builder.setMessage("Are you sure you want to sign out?")
                builder.setNegativeButton("Cancel") { dialog, _ -> dialog.dismiss() }
                builder.setCancelable(true)
                builder.setPositiveButton("Sign Out") { dialog, _ ->
                    if (isAdded) {
                        progressDialog.show()
                        try {
                            auth.signOut()
                            sharedPreferences.edit { clear() }
                            progressDialog.dismiss()
                            Toast.makeText(requireContext(), "Signed out", Toast.LENGTH_SHORT).show()
                            findNavController().navigate(R.id.action_customerHome_to_logIn)
                            dialog.dismiss()
                        } catch (e: Exception) {
                            progressDialog.dismiss()
                            Toast.makeText(requireContext(), "Failed to sign out", Toast.LENGTH_SHORT).show()
                            dialog.dismiss()
                        }
                    }
                }
                val dialog = builder.create()
                dialog.show()
            }
        }, viewLifecycleOwner)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (progressDialog.isShowing) {
            progressDialog.dismiss()
        }
        _binding = null
    }
}