package com.example.kiosk02

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Patterns

import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.databinding.FragmentFindPasswordConsumerBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class FindPasswordConsumerFragment : Fragment(R.layout.fragment_find_password_consumer) {

    private lateinit var binding: FragmentFindPasswordConsumerBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentFindPasswordConsumerBinding.bind(view)
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 버튼 비활성화
        binding.AdminSendResetMailButton.isEnabled = false

        binding.AdminEmailEditText.addTextChangedListener(textWatcher)
        binding.AdminPhoneEditText2.addTextChangedListener(textWatcher)

        binding.AdminSendResetMailButton.setOnClickListener {
            //trim() -> 공백 제거
            val inputEmail = binding.AdminEmailEditText.text.toString().trim()
            val inputPhoneNum = binding.AdminPhoneEditText2.text.toString().trim()

            if (inputEmail.isNotEmpty() && inputPhoneNum.isNotEmpty()) {
                sendResetMail(inputEmail, inputPhoneNum)
            } else {
                Snackbar.make(binding.root, "이메일과 전화번호를 확인해 주세요.", Snackbar.LENGTH_SHORT).show()
            }
        }


        // 이메일 찾기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.AdminEmailFindTextView).setOnClickListener {
            findNavController().navigate(R.id.action_to_findPasswordFragment) // 이메일 찾기 화면으로 이동
        }
        // 비밀번호 찾기 뒤로가기 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.BackAdminFindPasswordButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_mainFragment) // 관리자 초기 화면으로 이동
        }
        // 로그인 화면으로 가기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.AdminLoginTextView2).setOnClickListener {
            findNavController().navigate(R.id.action_to_mainFragment) //로그인 화면으로 이동
        }

    }

    // 실시간으로 이메일 및 전화번호 EditText 확인
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

        override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
            val inputEmail = binding.AdminEmailEditText.text.toString().trim()
            val inputPhoneNum = binding.AdminPhoneEditText2.text.toString().trim()

            binding.AdminSendResetMailButton.isEnabled =
                isValidEmail(inputEmail) && isValidPhone(inputPhoneNum)

        }

        override fun afterTextChanged(s: Editable?) {}
    }

    // 이메일 형식 확인
    private fun isValidEmail(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    //전화번호 형식 확인 -> 숫자만 입력 및 길이 확인
    private fun isValidPhone(phone: String): Boolean {
        return phone.length in 9..11 && phone.all { it.isDigit() }
    }

    private fun sendResetMail(inputEmail: String, inputPhoneNum: String) {
        db.collection("consumer")
            .whereEqualTo("email", inputEmail)
            .get()
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (!task.result.isEmpty) {
                        for (document in task.result) {
                            val firestorePhone = document.getString("phoneNumber")
                            if (firestorePhone == inputPhoneNum) {
                                // 전화번호가 일치하면 비밀번호 재설정 이메일 전송
                                auth.sendPasswordResetEmail(inputEmail)
                                    .addOnCompleteListener { resetTask ->
                                        if (resetTask.isSuccessful) {
                                            Snackbar.make(
                                                binding.root,
                                                "재설정 메일 전송 완료",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                            binding.AdminEmailEditText.text = null
                                            binding.AdminPhoneEditText2.text = null
                                        } else {
                                            Snackbar.make(
                                                binding.root,
                                                "이메일 전송 실패: ${resetTask.exception?.message}",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                            } else {
                                Snackbar.make(binding.root, "전화번호를 확인해 주세요.", Snackbar.LENGTH_SHORT)
                                    .show()
                            }
                        }
                    } else {
                        Snackbar.make(binding.root, "이메일을 확인해 주세요.", Snackbar.LENGTH_SHORT).show()
                    }
                } else {
                    Snackbar.make(binding.root, "이메일 또는 전화번호를 확인해 주세요.", Snackbar.LENGTH_SHORT).show()
                }
            }
    }
}
