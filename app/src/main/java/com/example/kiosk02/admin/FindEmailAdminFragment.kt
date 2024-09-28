package com.example.kiosk02.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.databinding.ActivityFindEmailAdminBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class FindEmailAdminFragment : Fragment(R.layout.activity_find_email_admin) {
    private lateinit var binding: ActivityFindEmailAdminBinding
    private lateinit var db: FirebaseFirestore
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        binding = ActivityFindEmailAdminBinding.bind(view)

        binding.AdminFindEmailButton.setOnClickListener {
            val businessNum = binding.AdminBusinessNumEditText.text.toString()
            val phoneNum = binding.AdminPhoneEditText.text.toString()

            if (businessNum.isNotEmpty() && phoneNum.isNotEmpty()) {
                searchAdminId(businessNum, phoneNum)
            } else {
                Snackbar.make(binding.root, "사업자 등록번호와 전화번호를 다시 확인해주세요.", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }

        // 이메일 찾기 뒤로가기 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.BackAdminFindEmailButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment) // 관리자 초기 화면으로 이동
        }
        // 비밀번호 찾기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.AdminPasswordFindTextView).setOnClickListener {
            findNavController().navigate(R.id.action_to_find_password_admin) // 비밀번호 찾기 화면으로 이동
        }
        // 로그인 화면으로 가기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.AdminLoginTextView).setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment) //로그인 화면으로 이동
        }


    }

    //입력한 사업자 등록번호와 전화번호를 기반으로 firestore 탐색
    private fun searchAdminId(businessNumber: String, tradeName: String) {
        db.collection("admin")
            .whereEqualTo("businessnumber", businessNumber)
            .whereEqualTo("tradeName", tradeName)
            .get()
            .addOnSuccessListener { documnets ->
                if (!documnets.isEmpty) {
                    for (document in documnets) {
                        val adminId = document.getString("email")

                        //다음 fragment로 이메일 전달
                        val bundle = Bundle().apply{
                            putString("adminEmail", adminId)
                        }
                        findNavController().navigate(R.id.action_to_show_email_admin,bundle)
                    }
                } else {
                    Snackbar.make(binding.root, "일치하는 아이디를 찾을 수 없습니다.", Snackbar.LENGTH_SHORT)
                        .show();
                }
            }
            .addOnFailureListener { exception ->
                Snackbar.make(binding.root, "일치하는 관리자를 찾을 수 없습니다.", Snackbar.LENGTH_SHORT).show();
            }
    }


}
