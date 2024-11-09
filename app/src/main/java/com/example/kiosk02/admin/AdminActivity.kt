package com.example.kiosk02.admin

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

class AdminActivity : Fragment(R.layout.activity_admin) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        LogOutButton(view)

        view.findViewById<Button>(R.id.menuCustomizationButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_admin_menu_list_fragment)
        }
        //테이블 레이아웃 클릭시 창전환
        view.findViewById<Button>(R.id.tableLayoutButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_table_Edit_Fragment) // 테이블 편집창으로 이동
        }
    }


    private fun LogOutButton(view: View) {
        view.findViewById<Button>(R.id.logoutButton).setOnClickListener {

            //SharedPreferences 초기화
            val sharedPreferences =
                requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // 자동 로그인 상태 해제 및 저장된 로그인 정보 삭제
            editor.putBoolean("isLoggedIn", false)
            editor.remove("user_email")
            editor.remove("user_password")
            editor.apply()

            //Firebase 로그아웃
            Firebase.auth.signOut()

            findNavController().navigate(R.id.action_to_adminFragment) // 관리자 초기화면으로 이동
        }

        view.findViewById<Button>(R.id.additionalRegistrationButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_admin_inform)
        }
    }
}





