package com.example.kiosk02

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.databinding.ActivityAdminSignBinding
import com.example.kiosk02.databinding.FragmentOrderMethodBinding
import com.example.kiosk02.map.SearchStoreActivity

class OrderMethodFragment:Fragment(R.layout.fragment_order_method) {
    private lateinit var binding: FragmentOrderMethodBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOrderMethodBinding.bind(view)

        val Uemail = arguments?.getString("Uemail")
        val Aemail = arguments?.getString("Aemail")
        Log.d("AddInformActivity", "Received email: $Uemail")
        Log.d("AddInformActivity", "Received email: $Aemail")

        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val intent = Intent(requireContext(), SearchStoreActivity::class.java)
                startActivity(intent)
            }
        })

        binding.togoButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("Aemail", Aemail)
                putString("Uemail", Uemail)
                putString("orderType","pickup")
            }
            findNavController().navigate(R.id.action_to_ConsumerMenuList, bundle) // 메뉴판으로 이동

        }

        binding.forhereButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("Aemail", Aemail)
                putString("Uemail", Uemail)
                putString("orderType","for_here")
            }
            findNavController().navigate(R.id.action_to_table_Select_Fragment,bundle) // 초기화면으로 이동
        }

        binding.backButton.setOnClickListener {
            val intent = Intent(requireContext(), SearchStoreActivity::class.java)
            startActivity(intent)
        }

    }
}