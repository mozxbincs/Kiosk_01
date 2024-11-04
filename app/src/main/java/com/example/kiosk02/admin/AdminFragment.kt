package com.example.kiosk02.admin

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.util.Patterns

import android.view.View
import android.widget.Button

import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.databinding.FragmentAdminBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class AdminFragment : Fragment(R.layout.fragment_admin) {

    private lateinit var binding: FragmentAdminBinding

    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var ediotr: SharedPreferences.Editor

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentAdminBinding.bind(view)

        binding.adminLoginButton.isEnabled = false

        binding.adminEmailInput.addTextChangedListener(textWatcher)
        binding.adminPasswordInput.addTextChangedListener(textWatcher)

        //sharedPreferences 초기화
        sharedPreferences = requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
        ediotr = sharedPreferences.edit()


        // 로그인 상태 유지 선택 시, 자동 로그인
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)
        if(isLoggedIn){
            val savedID = sharedPreferences.getString("user_email",null).toString()
            val savedPW = sharedPreferences.getString("user_password", null).toString()

            Firebase.auth.signInWithEmailAndPassword(savedID,savedPW)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        findNavController().navigate(R.id.action_to_admin_activity)
                    }else{
                        // 로그인 실패 시, 알림
                        Snackbar.make(binding.root,"로그인에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                    }
                }
        }

        //로그인 버튼
        binding.adminLoginButton.setOnClickListener {

            val email = binding.adminEmailInput.text.toString()
            val password = binding.adminPasswordInput.text.toString()

            Firebase.auth.signInWithEmailAndPassword(email,password)
                .addOnCompleteListener { task ->
                    if(task.isSuccessful){
                        findNavController().navigate(R.id.action_to_admin_activity)

                        //로그인 상태 유지 체크박스가 선택 되어 있을 시,
                        if(binding.adminRememberMeCheckbox.isChecked){
                            //로그인 성공 시, SharedPreferences에 로그인 상태 저장
                            val sharedPreferences = requireContext().getSharedPreferences("LoginPrefs", Context.MODE_PRIVATE)
                            val editor = sharedPreferences.edit()
                            editor.putBoolean("isLoggedIn", true)
                            editor.putString("user_email", email)
                            editor.putString("user_password", password)
                            editor.apply()
                        }
                        Log.e("stayLoggedIn", "$email")
                        Log.e("stayLoggedIn", "$password")
                    }else{
                        // 로그인 실패 시, 알림
                        Snackbar.make(binding.root,"로그인에 실패했습니다.", Snackbar.LENGTH_SHORT).show()
                    }

                }
        }



        // 이메일 찾기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.admin_forgot_email_text).setOnClickListener {
            findNavController().navigate(R.id.action_to_find_email_admin) // 이메일 찾기 화면으로 이동
        }

        // 비밀번호 찾기 버튼 클릭 리스너 설정
        view.findViewById<TextView>(R.id.admin_forgot_password_text).setOnClickListener {
            findNavController().navigate(R.id.action_to_find_password_admin) // 비밀번호 찾기 화면으로 이동
        }

        // 회원가입 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.admin_sign_up_button).setOnClickListener {
            findNavController().navigate(R.id.action_to_admin_sign_fragment) // 회원가입 화면으로 이동
        }


        // GoBack 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.guest_use_button).setOnClickListener {
            findNavController().navigate(R.id.action_to_mainFragment) // 초기화면으로 이동
        }
///

    }



        private fun isEmailValid(email:String):Boolean{
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private val textWatcher = object: TextWatcher{
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {

        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            val email = binding.adminEmailInput.text.toString()
            val password = binding.adminPasswordInput.text.toString()

            binding.adminLoginButton.isEnabled = isEmailValid(email) && password.isNotEmpty()
        }

        override fun afterTextChanged(p0: Editable?) {

        }
    }

}
