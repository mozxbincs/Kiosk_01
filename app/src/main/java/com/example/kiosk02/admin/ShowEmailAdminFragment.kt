package com.example.kiosk02.admin

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.databinding.FragmentShowEmailAdminBinding

class ShowEmailAdminFragment : Fragment(R.layout.fragment_show_email_admin){
    private lateinit var binding: FragmentShowEmailAdminBinding
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentShowEmailAdminBinding.bind(view)

        // 전달된 관리자 이메일 받기
        val adminEmail = arguments?.getString("adminEmail")

        binding.AdminEmailTextView.text = getString(R.string.showAdminEmailAddress, adminEmail)

        binding.GoBackToLoginButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment)
        }

    }
}