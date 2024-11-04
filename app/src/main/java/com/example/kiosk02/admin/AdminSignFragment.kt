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
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.databinding.ActivityAdminSignBinding
import com.google.android.material.snackbar.Snackbar
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

    private var NameValid = false
    private var Emailvalid = false
    private var Phonnumbalid = false
    private var passwordValid = false
    private var ConfirmpasswordValid = false
    private var BusinessnameValid = false
    private var businessnumbervalid = false
    private var Checkredundancy = false
    private var addressValid = false



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = ActivityAdminSignBinding.bind(view)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        businessService = RetrofitClient.getInstance().create(BusinessService::class.java)

        binding.registerButton.isEnabled = false

        binding.nameEditText.addTextChangedListener(textWatcher)
        binding.emailEditText.addTextChangedListener(textWatcher)
        binding.phonNumberEditText.addTextChangedListener(textWatcher)
        binding.passwordEditText.addTextChangedListener(textWatcher)
        binding.confirmPasswordEditText.addTextChangedListener(textWatcher)
        binding.businessNameEditText.addTextChangedListener(textWatcher)
        binding.businessNumberEditText.addTextChangedListener(textWatcher)
        binding.addressEditText.addTextChangedListener(textWatcher)

        // 상호등록 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.registerButton).setOnClickListener {
            val name = binding.nameEditText.text.toString()
            val email = binding.emailEditText.text.toString()
            val password = binding.passwordEditText.text.toString()
            val phonnumber = binding.phonNumberEditText.text.toString()
            val businessname = binding.businessNameEditText.text.toString()
            val businessnumber = binding.businessNumberEditText.text.toString()
            val address = binding.addressEditText.text.toString()

            registerUser(name, email, password, phonnumber, businessname, businessnumber, address)

        }

        // 관리자 회원가입 전 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.registerBackButton).setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment) // 관리자 초기화면으로 이동
        }

        view.findViewById<Button>(R.id.businessNumberButton).setOnClickListener {
            val businessNumber = binding.businessNumberEditText.text.toString()
            checkBusinessNumber(businessNumber)
        }
    }

    private val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\$\$".toRegex()
    private val passwordPattern = "^.*(?=^.{8,15}\$)(?=.*\\d)(?=.*[a-zA-Z])(?=.*[!@#\$%^&+=]).*\$".toRegex()
    private val phonNumPatten = "^^01[0-9]{8,9}\$".toRegex()
    private val hangulPatten = "^[가-힣]{2,}\$".toRegex()
    private val businessnamePatten = "^[가-힣a-zA-Z0-9\\s\\-]+\$".toRegex()
    private val businessnumberPatten = "^\\d{10}\$".toRegex()

    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {}

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
            val editText = view?.findFocus() as? EditText
            editText?.let { validateInputs(it) }
        }

        override fun afterTextChanged(p0: Editable?) {}
    }

    private fun validateInputs(editText: EditText) {
        when(editText.id) {
            R.id.nameEditText -> {
                val name = binding.nameEditText.text.toString().trim()
                NameValid = name.matches(hangulPatten)
                if (NameValid != true) {
                    binding.warningTextView.text = "한글로 입력해주세요"
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }

            R.id.emailEditText -> {
                val email = binding.emailEditText.text.toString().trim()
                Emailvalid = email.matches(emailPattern)
                if (Emailvalid != true) {
                    binding.warningTextView.text = "이메일 형식에 맞게 입력해주세요"
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }

            R.id.phonNumberEditText -> {
                val phonnumber = binding.phonNumberEditText.text.toString().trim()
                Phonnumbalid = phonnumber.matches(phonNumPatten)
                if (Phonnumbalid != true) {
                    binding.warningTextView.text = "-없이 전화번호를 정확히 입력해주세요"
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }

            R.id.passwordEditText -> {
                val password = binding.passwordEditText.text.toString()
                passwordValid = password.matches(passwordPattern)
                if (passwordValid != true) {
                    binding.warningTextView.text = "숫자, 문자, 특수문자를 포함하여 6자리 이상으로 입력해주세요"
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }

            R.id.confirmPasswordEditText -> {
                val confirmpassword = binding.confirmPasswordEditText.text.toString()
                ConfirmpasswordValid = confirmpassword.matches(passwordPattern)
                if (ConfirmpasswordValid != true) {
                    binding.warningTextView.text = "숫자, 문자, 특수문자를 포함하여 6자리 이상으로 입력해주세요"
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    if (binding.passwordEditText.text.toString() == confirmpassword) {
                        binding.warningTextView.visibility = View.GONE
                    } else {
                        binding.warningTextView.text = "비밀번호가 일치하지 않습니다."
                        ConfirmpasswordValid = false
                    }
                }
            }

            R.id.businessNameEditText -> {
                val businessname = binding.businessNameEditText.text.toString()
                BusinessnameValid = businessname.matches(businessnamePatten)
                if (BusinessnameValid != true) {
                    binding.warningTextView.text = "상호명을 정확히 입력하세요"
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }
            }

            R.id.businessNumberEditText -> {
                val businessnumber = binding.businessNumberEditText.text.toString()
                businessnumbervalid = businessnumber.matches(businessnumberPatten)
                if (businessnumbervalid != true) {
                    binding.warningTextView.text = "10자리 숫자로 입력해주세요"
                    binding.warningTextView.visibility = View.VISIBLE
                    binding.businessNumberButton.isEnabled = false
                } else {
                    binding.warningTextView.visibility = View.GONE
                    binding.businessNumberButton.isEnabled = true
                }
            }

            R.id.addressEditText -> {
                val address = binding.addressEditText.text.toString()
                addressValid = address.matches(hangulPatten)
                if (addressValid != true) {
                    binding.warningTextView.text = "아직 안정해져서 그냥 2자리 쳐"
                    binding.warningTextView.visibility = View.VISIBLE
                } else {
                    binding.warningTextView.visibility = View.GONE
                }

            }
        }
        isAllInputsCheckValid()
    }

    private fun isAllInputsCheckValid() {
        binding.registerButton.isEnabled = NameValid && Emailvalid && Phonnumbalid && passwordValid && ConfirmpasswordValid
                && businessnumbervalid && businessnumbervalid && addressValid && Checkredundancy && businessNumberCk
    }

    private fun findBusinessNumber(businessNumber: String) {
        firestore.collection("admin")
            .whereEqualTo("businessnumber", businessNumber)
            .get()
            .addOnSuccessListener { document ->
                if (!document.isEmpty) {
                    Snackbar.make(binding.root, "이미 등록된 사업자번호 입니다.", Snackbar.LENGTH_SHORT).show()
                    Checkredundancy = false
                } else {
                    Snackbar.make(binding.root, "등록 가능한 사업자번호 입니다.", Snackbar.LENGTH_SHORT).show()
                    Checkredundancy = true
                    binding.businessNumberEditText.isEnabled = false
                }
            }
    }

    private fun registerUser(name: String, email: String, password: String, phonnumber: String ,businessname: String, businessnumber: String, address: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    saveAdminDataToFirestore(name, email, phonnumber, businessname, businessnumber, address)
                    findNavController().navigate(R.id.action_to_admin_sign_fragment)
                } else {
                    Toast.makeText(context, "회원가입 실패: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveAdminDataToFirestore(name: String, email: String, phonnumber: String, businessname: String, businessnumber: String, address: String) {
        val admin = AdminData(
            name = name,
            email = email,
            phonnumber = phonnumber,
            tradeName = businessname,
            businessnumber = businessnumber,
            address = address
        )

        firestore.collection("admin").document(email)
            .set(admin)
            .addOnSuccessListener {
                Toast.makeText(context, "회원가입 완료", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.addInformActivity)
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
                        Toast.makeText(requireContext(), "사용 가능한 사업자 번호", Toast.LENGTH_SHORT).show()
                        businessNumberCk = true
                        findBusinessNumber(businessNumber)
                        isAllInputsCheckValid()

                    } else {
                        Toast.makeText(requireContext(), "유효하지 않은 등록번호", Toast.LENGTH_SHORT).show()
                        binding.businessNumberEditText.isEnabled = true
                        isAllInputsCheckValid()
                    }
                } else {
                    Toast.makeText(requireContext(), "요청실패 ${response.code()}", Toast.LENGTH_SHORT).show()
                    Log.e("API_ERROR", "요청실패 ${response.code()}")
                    isAllInputsCheckValid()
                }
            }

            override fun onFailure(call: Call<BusinessResponse>, t: Throwable) {
                Toast.makeText(requireContext(), "네트워크 오류", Toast.LENGTH_SHORT).show()
                isAllInputsCheckValid()
            }
        })
    }
}
