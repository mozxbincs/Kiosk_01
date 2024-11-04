package com.example.kiosk02

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController

class MainFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_main, container, false)
        val adminButton = view.findViewById<Button>(R.id.admin_button)

        adminButton.setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment)
        }

        view.findViewById<Button>(R.id.test_01).setOnClickListener {
            findNavController().navigate(R.id.action_to_table_Select_Fragment) // 초기화면으로 이동
        }

        return view
    }
}
