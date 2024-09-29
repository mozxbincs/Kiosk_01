package com.example.kiosk02.admin

import android.os.Bundle

import android.view.View
import android.widget.Button

import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.databinding.ActivityFindPasswordAdminBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FindPasswordAdminFragment : Fragment(R.layout.activity_find_password_admin) {

    private lateinit var binding: ActivityFindPasswordAdminBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = ActivityFindPasswordAdminBinding.bind(view)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        binding.AdminSendResetMailButton.setOnClickListener {
            //trim() -> 공백 제거
            val inputEmail = binding.AdminEmailEditText.text.toString().trim()
            val inputPhoneNum = binding.AdminPhoneEditText2.text.toString().trim()

            if (inputEmail.isNotEmpty() && inputPhoneNum.isNotEmpty()) {
                sendResetMail(inputEmail, inputPhoneNum)
            }else{
                Snackbar.make(binding.root,"이메일과 전화번호를 확인해 주세요.", Snackbar.LENGTH_SHORT).show()
            }
        }


//        // 비밀번호 재설정 버튼 클릭 리스너 설정
//        view.findViewById<Button>(R.id.AdminSendResetMailButton).setOnClickListener {
//            findNavController().navigate(R.id.action_to_reset_password_admin) // 비밀번호 재설정 화면으로 이동
//        }
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

    private fun sendResetMail(inputEmail: String, inputPhoneNum: String) {
        db.collection("admin")
            .whereEqualTo("email", inputEmail)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (!task.result.isEmpty) {
                        for (document in task.result) {
                            val firestorePhone = document.getString("businessnumber")
                            if (firestorePhone == inputPhoneNum) {
                                // 전화번호가 일치하면 비밀번호 재설정 이메일 전송
                                auth.sendPasswordResetEmail(inputEmail)
                                    .addOnCompleteListener { resetTask ->
                                        if (resetTask.isSuccessful) {
                                            Snackbar.make(binding.root,"재설정 메일 전송 완료", Snackbar.LENGTH_SHORT).show()
                                        } else {
                                            Snackbar.make(binding.root,"이메일 전송 실패: ${resetTask.exception?.message}", Snackbar.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Snackbar.make(binding.root,"전화번호를 확인해 주세요.", Snackbar.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Snackbar.make(binding.root,"이메일을 확인해 주세요.", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    Snackbar.make(binding.root,"데이터베이스 오류: ${task.exception?.message}", Snackbar.LENGTH_SHORT).show()
                }
            }
    }
    }
