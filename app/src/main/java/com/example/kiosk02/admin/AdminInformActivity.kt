package com.example.kiosk02.admin

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.example.kiosk02.databinding.ActivityAddInformBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class AdminInformActivity : Fragment(R.layout.fragment_admin_inform) {

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var addInformFinishButton: Button

    private var action = R.id.action_to_admin_sign_fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Spinner 항목 데이터 (첫 번째 값은 설명)
        val services = arrayOf("서비스유형", "음식점", "카페", "제과점", "바")
        val pickUps = arrayOf("픽업", "to go", "for here")
        val floors = arrayOf("층수") + (1..5).map { String.format("%d", it) }.toTypedArray()
        //val tables = arrayOf("테이블 갯수") + (1..100).map { String.format("%d개", it) }.toTypedArray()
        // val floors = arrayOf("층수", "1층", "2층", "3층","4층")
        // "테이블 갯수"를 포함하고, 1~100까지 숫자를 배열로 생성

        // val tables = arrayOf("테이블 갯수", "1개", "2개", "3개")


        // 어댑터 생성 및 Spinner에 연결
        val serviceAdapter = createAdapter(services)
        val pickUpAdapter = createAdapter(pickUps)
        val floorAdapter = createAdapter(floors)
        //val tableAdapter = createAdapter(tables)

        // 각 Spinner에 어댑터 설정
        val serviceSpinner = view.findViewById<Spinner>(R.id.AddInformService)
        val pickUpSpinner = view.findViewById<Spinner>(R.id.AddInformPickUp)
        val floorSpinner = view.findViewById<Spinner>(R.id.AddInformFloor)
        val addressEditText = view.findViewById<EditText>(R.id.addressEditText)
        //val tableSpinner = view.findViewById<Spinner>(R.id.AddInformTable)

        val user = auth.currentUser
        val email = user?.email

        if(email != null) {
            findUser(email, serviceSpinner, pickUpSpinner, floorSpinner, services, pickUps, floors)
        }

        //AddInformFinish Button 초기화
        addInformFinishButton = view.findViewById(R.id.AddInformFinish)
        addInformFinishButton.isEnabled = false

        serviceSpinner.adapter = serviceAdapter
        pickUpSpinner.adapter = pickUpAdapter
        floorSpinner.adapter = floorAdapter
        //tableSpinner.adapter = tableAdapter

        //spinner 동작감지 호출
        checkAllSpinnersSelected(serviceSpinner, pickUpSpinner, floorSpinner)

        // 회원가입 완료 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.AddInformFinish).setOnClickListener {
            val user = auth.currentUser
            val email = user?.email

            val serviceType = serviceSpinner.selectedItem.toString()
            val pickUpType = pickUpSpinner.selectedItem.toString()
            val floorCount = floorSpinner.selectedItem.toString()
            //val addressString = addressEditText.text.toString()
            //val tableCount = tableSpinner.selectedItem.toString()

            if(email != null) {
                updateFirestore(email, serviceType, pickUpType, floorCount)
            }

        }

        view.findViewById<ImageButton>(R.id.AddInformBack).setOnClickListener {
            findNavController().navigate(action) // 추가등록 전으로 이동
        }



    }

    private fun findUser(email: String,
                         serviceSpinner: Spinner,
                         pickUpSpinner: Spinner,
                         floorSpinner: Spinner,
                         services: Array<String>,
                         pickUps: Array<String>,
                         floors: Array<String>, ) {
            firestore.collection("admin")
                .document(email)
                .get()
                .addOnSuccessListener { document ->

                    if (document != null && document.exists()) {
                        val serviceType = document.getString("serviceType")
                        val pickUpType = document.getString("pickUpType")

                        val floorCount = when (val count = document.get("totalFloorCount")) {
                            is Number -> count.toInt()  // 숫자 타입일 경우 Int로 변환
                            is String -> count.toIntOrNull() ?: 0  // 문자열일 경우 Int로 변환 시도
                            else -> 0  // 다른 타입이거나 null일 경우 0으로 설정
                        }

                        // totalFloorCount를 Long으로 받아온 후, null 체크 및 변환
                        //val floorCount = document.getLong("totalFloorCount")?.toInt() ?: 0

                        var hasPreSelectedValues = false
                        serviceType?.let {
                            val position = services.indexOf(it)
                            if (position != -1) {
                                serviceSpinner.setSelection(position)
                                hasPreSelectedValues = true
                            }
                        }
                        pickUpType?.let {
                            val position = pickUps.indexOf(it)
                            if (position != -1) {
                                pickUpSpinner.setSelection(position)
                                hasPreSelectedValues = true
                            }
                        }

                        if (floorCount > 0) {
                            val position = floors.indexOf(floorCount.toString())
                            if (position > 0) { // 0번은 설명용 항목이므로 1부터 시작
                                floorSpinner.setSelection(position)
                                hasPreSelectedValues = true
                            }
                        }

                        action = if (hasPreSelectedValues) R.id.action_to_admin_activity else R.id.action_to_admin_sign_fragment
                    }

                    /*
                    if (document != null && document.exists()) {
                        val serviceType = document.getString("serviceType")
                        val pickUpType = document.getString("pickUpType")
                        val floorCountString = document.getString("totalFloorCount")
                        val floorCount = floorCountString?.toIntOrNull() ?: 0

                        var hasPreSelectedValues = false
                        serviceType?.let {
                            val position = services.indexOf(it)
                            if (position != -1) {
                                serviceSpinner.setSelection(position)
                                hasPreSelectedValues = true
                            }
                        }
                        pickUpType?.let {
                            val position = pickUps.indexOf(it)
                            if (position != -1) {
                                pickUpSpinner.setSelection(position)
                                hasPreSelectedValues = true
                            }
                        }

                        if (floorCount > 0) {
                            val position = floors.indexOf(floorCount.toString())
                            if (position > 0) { // 0번은 설명용 항목이므로 1부터 시작
                                floorSpinner.setSelection(position)
                                hasPreSelectedValues = true
                            }
                        }

                        action = if (hasPreSelectedValues) R.id.action_to_admin_activity else R.id.action_to_admin_sign_fragment

                    } */

                }.addOnFailureListener {
                    Snackbar.make(requireView(), "사용자 정보 불러오기 실패", Snackbar.LENGTH_SHORT).show()
                }
    }

    private fun updateFirestore(email: String, serviceType: String, pickUpType: String, floorCount: String) {
        val storeInform = hashMapOf<String, Any>(
            "serviceType" to serviceType,
            "pickUpType" to pickUpType,
            "totalFloorCount" to floorCount,
        )

        firestore.collection("admin")
            .document(email)
            .update(storeInform)
            .addOnSuccessListener {
                Snackbar.make(requireView(), "가게 정보 저장 완료", Snackbar.LENGTH_SHORT).show()
                findNavController().navigate(R.id.adminActivity)
            }.addOnFailureListener { exception ->
                Snackbar.make(requireView(), "정보 저장 실패 ${exception.message}", Snackbar.LENGTH_SHORT).show()
            }
    }

    //동작 감지후 모두 선택되었을때 버튼 활성화
    private fun checkAllSpinnersSelected(service: Spinner, pickUp: Spinner, floor: Spinner) {
        val spinnerListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p0: AdapterView<*>?, p1: View?, p2: Int, p3: Long) {
                val isAllSelected = service.selectedItemPosition != 0 &&
                        pickUp.selectedItemPosition != 0 &&
                        floor.selectedItemPosition != 0

                addInformFinishButton.isEnabled = isAllSelected
            }

            override fun onNothingSelected(p0: AdapterView<*>?) {}
        }

        service.onItemSelectedListener = spinnerListener
        pickUp.onItemSelectedListener = spinnerListener
        floor.onItemSelectedListener = spinnerListener
    }


    // 어댑터 생성 (첫 번째 항목을 비활성화)
    private fun createAdapter(items: Array<String>): ArrayAdapter<String> {
        return object :
            ArrayAdapter<String>(requireContext(), android.R.layout.simple_spinner_item, items) {
            override fun isEnabled(position: Int): Boolean {
                // 첫 번째 항목(설명)은 선택 불가능하게 설정
                return position != 0
            }

            override fun getDropDownView(
                position: Int,
                convertView: View?,
                parent: ViewGroup
            ): View {
                val view = super.getDropDownView(position, convertView, parent)

                // 첫 번째 항목(설명)은 비활성화된 상태로 표시 (회색 처리)
                if (position == 0) {
                    val textView = view as TextView
                    textView.setTextColor(context.getColor(android.R.color.darker_gray))
                } else {
                    val textView = view as TextView
                    textView.setTextColor(context.getColor(android.R.color.black))
                }
                return view
            }
        }.also {
            it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }
}