package com.example.kiosk02.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.databinding.ActivityAdminSignBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.regex.Pattern

class AdminSignFragment : Fragment(R.layout.activity_admin_sign) {

    private lateinit var binding: ActivityAdminSignBinding
    private lateinit var businessService: BusinessService
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private val apiKey = "q40rN3NyDh2wW96K08F2X3EGCIyLsV8c3tbNBgnZBZ3hFmxCeznuLoJr5+aHtlHRRH0GA+NpGakhiX13jv4pWg=="
    private var businessNumberCk = false

    private var isEmailUnique = true
    private var isBusinessNumberUnique = true

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

        binding.nameEditText.addTextChangedListener(textWatcher)
        binding.emailEditText.addTextChangedListener(textWatcher)
        binding.passwordEditText.addTextChangedListener(textWatcher)
        binding.confirmPasswordEditText.addTextChangedListener(textWatcher)
        binding.businessNameEditText.addTextChangedListener(textWatcher)
        binding.businessNumberEditText.addTextChangedListener(textWatcher)
        binding.addressEditText.addTextChangedListener(textWatcher)
        binding.startEditText.addTextChangedListener(textWatcher)

    }

    // 정규식 지정
    private val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$\$".toRegex()
    private val passwordPattern = "^.*(?=^.{8,15}\$)(?=.*\\d)(?=.*[a-zA-Z])(?=.*[!@#\$%^&+=]).*\$".toRegex()
    private val hangulPatten = "^[가-힣]{2,}\$".toRegex()
    private val businessnumberPatten = "^\\d{10}\$".toRegex()
    //private val adressPatten = "^[가-힣a-zA-Z0-9\\\\s,.-]{3,}\$".toRegex()
    private val startDayPatten = "^\\d{8}\$".toRegex()

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            val editText = view?.findFocus() as? EditText
            editText?.let { validateInputs(it) }
        }

        override fun afterTextChanged(p0: Editable?) {}

    }
    // 검사 기능
    private fun validateInputs(editText: EditText) {
        when(editText.id) {
            R.id.nameEditText -> {
                val name = binding.nameEditText.text.toString()
                val NameValid = name.matches(hangulPatten)
                if (NameValid != true) {
                    binding.warningTextView.text = getString(R.string.warning_hangul)
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }

            R.id.emailEditText -> {
                val email = binding.emailEditText.text.toString()
                val Emailvalid = email.matches(emailPattern)
                if(Emailvalid != true) {
                    binding.warningTextView.text = getString(R.string.warning_email)
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                    //checkEmailInFirebase(email)
                }
            }

            R.id.passwordEditText -> {
                val password = binding.passwordEditText.text.toString()
                val passwordValid = password.matches(passwordPattern)
                if (passwordValid != true) {
                    binding.warningTextView.text = getString(R.string.warning_password)
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }

            R.id.confirmPasswordEditText -> {
                val confirmpassword = binding.confirmPasswordEditText.text.toString()
                if (confirmpassword != binding.passwordEditText.text.toString()) {
                    binding.warningTextView.text = "비밀번호가 일치하지 않습니다."
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }

            R.id.businessNameEditText -> {
                val businessname = binding.businessNameEditText.text.toString()
                val NameValid = businessname.matches(hangulPatten)
                if (NameValid != true) {
                    binding.warningTextView.text = getString(R.string.warning_hangul)
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }

            R.id.businessNumberEditText -> {
                val businessnumber = binding.businessNumberEditText.text.toString()
                val businessnumbervalid = businessnumber.matches(businessnumberPatten)
                if(businessnumbervalid != true) {
                    binding.warningTextView.text = getString(R.string.warning_businessnumber)
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                    //checkBusinessNumberInFirebase(businessnumber)
                }
            }

            R.id.addressEditText -> {
                val address = binding.addressEditText.text.toString()
                val NameValid = address.matches(hangulPatten)
                if (NameValid != true) {
                    binding.warningTextView.text = getString(R.string.warning_hangul)
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }

            R.id.startEditText -> {
                val start = binding.startEditText.text.toString()
                val NameValid = start.matches(startDayPatten)
                if (NameValid != true) {
                    binding.warningTextView.text = getString(R.string.warning_startday)
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }
        }
        isAllInputsValid()
    }

    private fun isAllInputsValid(): Boolean {
        val WarningHidden = binding.warningTextView.visibility == View.GONE

        return WarningHidden
    }
    /*
    private fun checkEmailInFirebase(email: String) {
        firestore.collection("admin")
            .whereEqualTo("email", email)
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    isEmailUnique = false }
                validateInputs(binding.emailEditText)
            }.addOnFailureListener { documents ->
                isEmailUnique = false
                validateInputs(binding.emailEditText)
            }
    }

    private fun checkBusinessNumberInFirebase(businessnumber: String) {
        firestore.collection("admin")
            .whereEqualTo("businessnumber", businessnumber)
            .get()
            .addOnSuccessListener { documents ->
                isBusinessNumberUnique = documents.isEmpty
                //validateInputs(binding.businessNumberEditText)
            }.addOnFailureListener {
                isBusinessNumberUnique = false
                //validateInputs(binding.businessNumberEditText)
            }
    }*/

    private fun registerUser(name: String, email: String, password: String, businessname: String, businessnumber: String, address: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveAdminDataToFirestore(name, email, password, businessname, businessnumber, address)
                    findNavController().navigate(R.id.action_to_admin_sign_fragment)
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
                        val businessname = binding.businessNumberEditText.text.toString()
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
