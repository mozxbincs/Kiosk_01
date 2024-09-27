package com.example.kiosk02.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R

class ResetPasswordAdminFragment :Fragment(R.layout.activity_reset_password_admin) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 로그인 화면으로 가기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.backToLoginTextView).setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment) //로그인 화면으로 이동
        }
    }
}

