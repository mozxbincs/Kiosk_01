package com.example.kiosk02.admin

import android.os.Bundle

import android.view.View
import android.widget.Button

import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R

class FindPasswordAdminFragment : Fragment(R.layout.activity_find_password_admin) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 비밀번호 재설정 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.AdminResetPasswordButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_reset_password_admin) // 비밀번호 재설정 화면으로 이동
        }
        // 이메일 찾기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.AdminEmailFindTextView).setOnClickListener {
            findNavController().navigate(R.id.action_to_find_email_admin) // 이메일 찾기 화면으로 이동
        }
        // 비밀번호 찾기 뒤로가기 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.BackAdminFindPasswordButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment) // 관리자 초기 화면으로 이동
        }
        // 로그인 화면으로 가기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.AdminLoginTextView2).setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment) //로그인 화면으로 이동
        }

    }

}