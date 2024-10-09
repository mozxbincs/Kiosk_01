package com.example.kiosk02.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kiosk02.databinding.FragmentAdminMenuListBinding

class AdminMenuListFragment: Fragment() {
    private lateinit var binding: FragmentAdminMenuListBinding

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAdminMenuListBinding.bind(view)

        //item_menu 2줄 나열
        val recyclerView = binding.menuListRecyclerView
        val gridLayoutManager = GridLayoutManager(requireContext(),2)
        recyclerView.layoutManager = gridLayoutManager
    }
}