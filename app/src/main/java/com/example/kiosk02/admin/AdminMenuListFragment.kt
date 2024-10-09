package com.example.kiosk02.admin

import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kiosk02.R
import com.example.kiosk02.admin.data.MenuModel
import com.example.kiosk02.databinding.FragmentAdminMenuListBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.toObject

class AdminMenuListFragment : Fragment(R.layout.fragment_admin_menu_list) {
    private lateinit var binding: FragmentAdminMenuListBinding
    private lateinit var auth: FirebaseAuth

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAdminMenuListBinding.bind(view)

        val db = Firebase.firestore
        val user = Firebase.auth.currentUser

        if (user == null) {
            findNavController().navigate(R.id.adminFragment)
            return
        }
        val email = user.email.toString()

        val firestore = FirebaseFirestore.getInstance()
        // admin 컬렉션에서 로그인한 유저 email 문서 참조
        val adminDoc = firestore.collection("admin").document(email)
        // 하위 컬렉션 menu에서 메뉴 문서 참조
        val menuDoc = adminDoc.collection("menu").document("순대국밥")

        menuDoc.get().addOnSuccessListener { document ->
            val menu = document.toObject<MenuModel>()
        }.addOnFailureListener {
            it.printStackTrace()
        }

        setupEditMenuButton(view)


        //item_menu 2줄 나열
        val recyclerView = binding.menuListRecyclerView
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        recyclerView.layoutManager = gridLayoutManager

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
}