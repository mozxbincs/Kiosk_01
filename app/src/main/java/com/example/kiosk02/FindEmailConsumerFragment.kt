package com.example.kiosk02

import android.os.Bundle
import android.provider.ContactsContract.CommonDataKinds.Phone
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.transition.Visibility
import com.example.kiosk02.R
import com.example.kiosk02.databinding.ActivityFindEmailAdminBinding
import com.example.kiosk02.databinding.FragmentFindEmailConsumerBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FirebaseFirestore

class FindEmailConsumerFragment : Fragment(R.layout.fragment_find_email_consumer) {
    private lateinit var binding: FragmentFindEmailConsumerBinding
    private lateinit var db: FirebaseFirestore
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        db = FirebaseFirestore.getInstance()

        binding = FragmentFindEmailConsumerBinding.bind(view)

        //예외처리 완료
        binding.AdminFindEmailButton.isEnabled = false
        binding.AdminBusinessNumEditText.addTextChangedListener(textWatcher)
        binding.AdminPhoneEditText.addTextChangedListener(textWatcher)

        binding.AdminFindEmailButton.setOnClickListener {
            val businessNum = binding.AdminBusinessNumEditText.text.toString()
            val phoneNum = binding.AdminPhoneEditText.text.toString()

            if (businessNum.isNotEmpty() && phoneNum.isNotEmpty()) {
                searchAdminId(businessNum, phoneNum)
            } else {
                Snackbar.make(binding.root, " 이름와 전화번호를 다시 확인해주세요.", Snackbar.LENGTH_SHORT)
                    .show()
            }
        }


        // 이메일 찾기 뒤로가기 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.BackAdminFindEmailButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment) // 관리자 초기 화면으로 이동
        }
        // 비밀번호 찾기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.AdminPasswordFindTextView).setOnClickListener {
            //findNavController().navigate(R.id.action_to_find_password_admin) // 비밀번호 찾기 화면으로 이동
        }
        // 로그인 화면으로 가기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.AdminLoginTextView).setOnClickListener {
            findNavController().navigate(R.id.action_to_mainFragment) //로그인 화면으로 이동
        }


    }

    private val businessnumberPatten = "^[가-힣]{2,}\$".toRegex()// 변수명 변경 안함 이름 한글로 받음
    private val phonnumberPatten = "^01[0-9]{8,9}\$".toRegex()

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            Log.e("작동", "작동")
            val inputBusinessNumber = binding.AdminBusinessNumEditText.text.toString()
            val inputPhonNumber = binding.AdminPhoneEditText.text.toString()
            val validBusinessNum = inputBusinessNumber.matches(businessnumberPatten)
            val validPhonNum = inputPhonNumber.matches(phonnumberPatten)

            if (!validBusinessNum) {
                unValidBusinessNum()
                binding.AdminFindEmailButton.isEnabled = false
            } else if (!validPhonNum) {
                unValidPhonNum()
                binding.AdminFindEmailButton.isEnabled = false
            }
            else {
                binding.warningTextView.visibility = View.GONE
            }

            binding.AdminFindEmailButton.isEnabled = validBusinessNum && validPhonNum
        }

        override fun afterTextChanged(p0: Editable?) {}

    }

    private fun unValidBusinessNum() {
        binding.warningTextView.visibility = View.VISIBLE
        binding.warningTextView.text = "이름을 정확히 한글로 입력하세요"

    }

    private fun unValidPhonNum() {
        binding.warningTextView.visibility = View.VISIBLE
        binding.warningTextView.text = "전화번호를 정확히 입력하세요"
    }

    //입력한 사업자 등록번호와 전화번호를 기반으로 firestore 탐색
    private fun searchAdminId(businessNumber: String, phoneNumber: String) {
        db.collection("consumer")
            .whereEqualTo("name", businessNumber)
            .whereEqualTo("phoneNumber", phoneNumber)
            .get()
            .addOnSuccessListener { documnets ->
                if (!documnets.isEmpty) {
                    for (document in documnets) {
                        val adminId = document.getString("email")

                        //다음 fragment로 이메일 전달
                        val bundle = Bundle().apply {
                            putString("adminEmail", adminId)
                            putString("goto", "consumer")
                        }
                        findNavController().navigate(R.id.action_to_show_email_admin, bundle)
                    }
                } else {
                    Snackbar.make(binding.root, "일치하는 아이디를 찾을 수 없습니다.", Snackbar.LENGTH_SHORT)
                        .show()
                }
            }
            .addOnFailureListener { exception ->
                Snackbar.make(binding.root, "일치하는 관리자를 찾을 수 없습니다.", Snackbar.LENGTH_SHORT).show()
            }
    }


}
