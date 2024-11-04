package com.example.kiosk02.consumer.menu

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.databinding.FragmentConsumerMenuListBinding
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore


class ConsumerMenuListFragment : Fragment(R.layout.fragment_consumer_menu_list) {
    private lateinit var binding: FragmentConsumerMenuListBinding
    private val firestore = FirebaseFirestore.getInstance()

    // 소비자가 매장을 선택했을 때 해당 매장의 document명을 받아와야 함
//    private val user = "cherrychoi35@gmail.com"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentConsumerMenuListBinding.bind(view)

        loadCategoriesToTabs()

        binding.cartImageButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_ConsumerCartFragment)
        }

//        binding.backButton.setOnClickListener {
//
//        }

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
        val viewPagerAdapter = ConsumerMenuPagerAdapter(this)
        viewPagerAdapter.setCategories(categories)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            run {
                tab.text = viewPagerAdapter.getCategoryTitle(position)
            }

        }.attach()
    }

    // 경로 지정 함수
    private fun getAdminDocument(): DocumentReference {
//        val email = getUserEmail()
        val email = "cherrychoi35@gmail.com"
        return firestore.collection("admin").document(email)
    }


// 해당 매장의 document명 처리 함수
//    private fun getUserEmail(): String {
//        return user?.email.toString()
//    }
}