package com.example.kiosk02


import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment

import com.example.kiosk02.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Navigation Controller 설정
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        // 관리자 버튼 클릭 리스너 설정
        findViewById<Button>(R.id.admin_button).setOnClickListener {
            navController.navigate(R.id.adminFragment) // 관리자 화면으로 이동
        }

        findViewById<Button>(R.id.login_button).setOnClickListener {

        }

    }
}
