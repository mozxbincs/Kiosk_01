package com.example.kiosk02.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R

class AdminActivity : Fragment(R.layout.activity_admin) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 관리자 인터페이스 뒤로가기 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.BackAdminActivityButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment) // 관리자 초기화면으로 이동
        }
        //테이블 레이아웃 클릭시 창전환
        view.findViewById<Button>(R.id.tableLayoutButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_table_Edit_Fragment) // 테이블 편집창으로 이동
        }
    }
}