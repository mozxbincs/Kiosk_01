package com.example.kiosk02.admin.menu.data

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kiosk02.databinding.FragmentMenuListBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class MenuListFragment : Fragment() {

    private lateinit var binding: FragmentMenuListBinding
    private val firestore = FirebaseFirestore.getInstance()
    private var category: String? = null
    private val user = Firebase.auth.currentUser

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String): MenuListFragment {
            return MenuListFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category)
                }
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentMenuListBinding.inflate(inflater, container, false)
        category = arguments?.getString(ARG_CATEGORY)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()

        loadMenuItemsForCategory(category)
    }

    private fun setupRecyclerView() {
        val adapter = MenuListAdapter()
        val gridLayoutManager = GridLayoutManager(requireContext(),2)
        binding.menuListRecyclerView.layoutManager = gridLayoutManager
        binding.menuListRecyclerView.adapter = adapter

        Log.d("MenuListFragment", "Adapter set for RecyclerView")
    }

    private fun loadMenuItemsForCategory(category: String?) {
        category ?: return

        binding.progressBarLayout.visibility = View.VISIBLE
        binding.menuListRecyclerView.visibility = View.GONE

        getAdminDocument().collection("menu")
            .whereEqualTo("category", category)
            .get()
            .addOnSuccessListener { documents ->
                val menuItems = documents.toObjects(MenuModel::class.java)
                Log.d("MenuListFragment", "Loaded ${menuItems.size} items")
                menuItems.forEach { menuItem ->
                    Log.d("MenuListFragment", "Menu Item: $menuItem")

                    //ProgressBar 숨기기
                    binding.progressBarLayout.visibility = View.GONE
                    binding.menuListRecyclerView.visibility = View.VISIBLE
                }
                (binding.menuListRecyclerView.adapter as MenuListAdapter).submitList(menuItems)
                Log.d("MenuListFragment", "Items submitted to adapter: $menuItems")
            }
            .addOnFailureListener {

                Snackbar.make(binding.root, "표시할 메뉴가 없습니다.", Snackbar.LENGTH_SHORT).show()
                //ProgressBar 숨기기
                binding.progressBarLayout.visibility = View.GONE
                binding.menuListRecyclerView.visibility = View.VISIBLE
            }
    }

    private fun getAdminDocument(): DocumentReference {
        val email = getUserEmail()
        return firestore.collection("admin").document(email)
    }

    private fun getUserEmail(): String {
        return user?.email.toString()
    }
}