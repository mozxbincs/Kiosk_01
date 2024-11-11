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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentConsumerMenuListBinding.bind(view)

        val bundle = arguments
        val Aemail = bundle?.getString("Aemail")

        if(Aemail.isNullOrEmpty()){
            Log.e("ConsumerMenuListFragment", "Aemail is missing or empty")
            return
        }

        loadCategoriesToTabs(Aemail)

        binding.cartImageButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_ConsumerCartFragment, bundle)
        }

//        binding.backButton.setOnClickListener {
//
//        }

    }


    private fun loadCategoriesToTabs(Aemail:String) {
        getAdminDocument(Aemail).collection("category")
            .get()
            .addOnSuccessListener { documents ->
                val categories = documents.map { it.getString("name") ?: "" }
                setupTabLayoutWithViewPager(categories,arguments)
            }.addOnFailureListener {
                Log.e("TabLayout", "탭 레이아웃 설정 실패")
            }
    }

    private fun setupTabLayoutWithViewPager(categories: List<String>, bundle: Bundle?) {
        val viewPagerAdapter = ConsumerMenuPagerAdapter(this, bundle)
        viewPagerAdapter.setCategories(categories)
        binding.viewPager.adapter = viewPagerAdapter

        TabLayoutMediator(binding.tabLayout, binding.viewPager) { tab, position ->
            run {
                tab.text = viewPagerAdapter.getCategoryTitle(position)
            }

        }.attach()
    }

    // 경로 지정 함수
    private fun getAdminDocument(Aemail: String): DocumentReference {
        return firestore.collection("admin").document(Aemail)
    }
}