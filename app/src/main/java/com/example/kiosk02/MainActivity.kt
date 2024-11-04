package com.example.kiosk02


import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.NavController
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment

import com.example.kiosk02.databinding.ActivityMainBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth

class MainActivity : AppCompatActivity() {
    private lateinit var navController: NavController
    private lateinit var auth: FirebaseAuth

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        auth = FirebaseAuth.getInstance()

        // Navigation Controller 설정
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        navController = navHostFragment.navController

        val fragmentToShow = intent?.getStringExtra("fragmentToShow")
        val Aemail = intent?.getStringExtra("Aemail")
        val Uemail = intent?.getStringExtra("Uemail")

        when (fragmentToShow) {
            "targetFragment" -> {
                val bundle = Bundle().apply {
                    putString("Aemail", Aemail)
                    putString("Uemail", Uemail)
                }
                navController.navigate(R.id.action_to_OderMethod, bundle)
            }
        }

        // 관리자 버튼 클릭 리스너 설정
        findViewById<Button>(R.id.admin_button).setOnClickListener {
            navController.navigate(R.id.adminFragment) // 관리자 화면으로 이동
        }

        findViewById<Button>(R.id.guest_use_button).setOnClickListener {
            navController.navigate(R.id.action_to_ConsumerMenuList)
        }
        /*
        findViewById<Button>(R.id.login_button).setOnClickListener {
            val email = findViewById<EditText>(R.id.email_input).text.toString()
            val password = findViewById<EditText>(R.id.password_input).text.toString()
            LoginToFirebase(email, password)
        }

        findViewById<Button>(R.id.sign_up_button).setOnClickListener {
            navController.navigate(R.id.action_to_fragment_consumer_sig)
        }
        */
    }
/*
    private fun LoginToFirebase(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val intent = Intent(this, SearchStoreActivity::class.java)
                    startActivity(intent)
                } else {
                    Log.e("LoginError", "Login failed", task.exception)
                }
            }

    }*/
}
