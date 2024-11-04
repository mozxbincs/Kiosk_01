package com.example.kiosk02

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.databinding.ActivityAdminSignBinding
import com.example.kiosk02.databinding.FragmentOrderMethodBinding

class OrderMethodFragment:Fragment(R.layout.fragment_order_method) {
    private lateinit var binding: FragmentOrderMethodBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentOrderMethodBinding.bind(view)

        val Uemail = arguments?.getString("Uemail")
        val Aemail = arguments?.getString("Aemail")
        Log.d("AddInformActivity", "Received email: $Uemail")
        Log.d("AddInformActivity", "Received email: $Aemail")
        binding.togoButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("Aemail", Aemail)
                putString("Uemail", Uemail)
            }
            //findNavController().navigate()
        }

        binding.forhearButton.setOnClickListener {
            val bundle = Bundle().apply {
                putString("Aemail", Aemail)
                putString("Uemail", Uemail)
            }
            //findNavController().navigate()
        }

    }
}