package com.example.kiosk02.admin.menu.data

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kiosk02.R
import com.example.kiosk02.databinding.FragmentAdminMenuListBinding
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore

class AdminMenuListFragment : Fragment(R.layout.fragment_admin_menu_list) {
    private lateinit var binding: FragmentAdminMenuListBinding
    private val firestore = FirebaseFirestore.getInstance()
    private val user = Firebase.auth.currentUser

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAdminMenuListBinding.bind(view)

        if (user == null) {
            findNavController().navigate(R.id.adminFragment)
            return
        }

        loadCategoriesToTabs()

        setupEditMenuButton(view)

        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_admin_activity)
        }

    }

    private fun setupEditMenuButton(view: View) {
        binding.addMenuButton.setOnClickListener {
            if (Firebase.auth.currentUser != null) {
                findNavController().navigate(R.id.action_to_edit_menu_fragment)
            } else {
                Snackbar.make(view, "로그인이 되어 있지 않습니다.", Snackbar.LENGTH_SHORT).show()
            }

        }
    }

    private fun loadCategoriesToTabs() {
        getAdminDocument().collection("category")
            .get()
            .addOnSuccessListener { documents ->
                val categories = documents.map { it.getString("name") ?: "" }
                setupTabLayoutWithViewPager(categories)
            }.addOnFailureListener {
                Log.e("TabLayout", "탭 레이아웃 설정 실패")
            }
    }

    private fun setupTabLayoutWithViewPager(categories: List<String>) {
        val viewPagerAdapter = MenuPagerAdapter(this)
        viewPagerAdapter.setCategories(categories)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            run{
                tab.text = viewPagerAdapter.getCategoryTitle(position)
            }

        }.attach()
    }

    // 경로 지정 함수
    private fun getAdminDocument(): DocumentReference {
        val email = getUserEmail()
        return firestore.collection("admin").document(email)
    }

    private fun getUserEmail(): String {
        return user?.email.toString()
    }
}