package com.example.kiosk02.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.databinding.ActivityAdminSignBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
class AdminSignFragment : Fragment(R.layout.activity_admin_sign) {

    private lateinit var binding: ActivityAdminSignBinding
    private lateinit var businessService: BusinessService
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val apiKey = "q40rN3NyDh2wW96K08F2X3EGCIyLsV8c3tbNBgnZBZ3hFmxCeznuLoJr5+aHtlHRRH0GA+NpGakhiX13jv4pWg=="
    private var businessNumberCk = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = ActivityAdminSignBinding.bind(view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        businessService = RetrofitClient.getInstance().create(BusinessService::class.java)

        // 상호등록 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.registerButton).setOnClickListener {
            val businessNumber = binding.businessNumberEditText.text.toString()
            checkBusinessNumber(businessNumber)
        }

//        // 관리자 회원가입 전 버튼 클릭 리스너 설정
//        view.findViewById<Button>(R.id.registerBackButton).setOnClickListener {
//            findNavController().navigate(R.id.action_to_adminFragment) // 관리자 초기화면으로 이동
//        }
    }

    private fun registerUser(name: String, email: String, password: String, businessname: String, businessnumber: String, address: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveAdminDataToFirestore(name, email, password, businessname, businessnumber, address)
                    findNavController().navigate(R.id.action_to_adminFragment)
                } else {
                    Toast.makeText(context, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveAdminDataToFirestore(name: String, email: String, password: String, businessname: String, businessnumber: String, address: String) {
        val admin = AdminData(
            name = name,
            email = email,
            tradeName = businessname,
            businessnumber = businessnumber,
            address = address
        )

        firestore.collection("admin").document(email)
            .set(admin)
            .addOnSuccessListener {
                Toast.makeText(context, "회원가입 완료", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener {
                Toast.makeText(context, "데이터 저장 실패", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkBusinessNumber(businessNumber: String) {
        val request = BusinessRequest(b_no = listOf(businessNumber))

        businessService.checkBusinessNumber(apiKey, request).enqueue(object : Callback<BusinessResponse> {
            override fun onResponse(
                call: Call<BusinessResponse>,
                response: Response<BusinessResponse>
            ) {
                if (response.isSuccessful) {
                    val businessData = response.body()?.data?.firstOrNull()
                    if (businessData?.b_stt_cd == "01") {
                        Toast.makeText(requireContext(), "등록된 사업자 번호", Toast.LENGTH_SHORT).show()
                        businessNumberCk = true

                        // 사업자 번호가 유효하면 회원가입 프로세스 진행
                        val name = binding.nameEditText.text.toString()
                        val email = binding.emailEditText.text.toString()
                        val password = binding.passwordEditText.text.toString()
                        val confirmPassword = binding.confirmPasswordEditText.text.toString()
                        val businessname = binding.businessNameEditText.text.toString()
                        val businessnumber = binding.businessNumberEditText.text.toString()
                        val address = binding.addressEditText.text.toString()

                        registerUser(name, email, password, businessname, businessnumber, address)
                    } else {
                        Toast.makeText(requireContext(), "유효하지 않은 등록번호", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(requireContext(), "요청실패 ${response.code()}", Toast.LENGTH_SHORT).show()
                    Log.e("API_ERROR", "요청실패 ${response.code()}")
                }
            }

            override fun onFailure(call: Call<BusinessResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
