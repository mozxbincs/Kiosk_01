package com.example.kiosk02.admin

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R

class AddInformActivity : Fragment(R.layout.activity_add_inform) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Spinner 항목 데이터 (첫 번째 값은 설명)
        val services = arrayOf("서비스유형", "음식점", "카페", "제과점", "바")
        val pickUps = arrayOf("픽업", "to go", "for here")
        val floors = arrayOf("층수") + (1..5).map { String.format("%d개", it) }.toTypedArray()
        val tables = arrayOf("테이블 갯수") + (1..100).map { String.format("%d개", it) }.toTypedArray()
        // val floors = arrayOf("층수", "1층", "2층", "3층","4층")
        // "테이블 갯수"를 포함하고, 1~100까지 숫자를 배열로 생성

        // val tables = arrayOf("테이블 갯수", "1개", "2개", "3개")

        // 어댑터 생성 및 Spinner에 연결
        val serviceAdapter = createAdapter(services)
        val pickUpAdapter = createAdapter(pickUps)
        val floorAdapter = createAdapter(floors)
        val tableAdapter = createAdapter(tables)

        // 각 Spinner에 어댑터 설정
        val serviceSpinner = view.findViewById<Spinner>(R.id.AddInformService)
        val pickUpSpinner = view.findViewById<Spinner>(R.id.AddInformPickUp)
        val floorSpinner = view.findViewById<Spinner>(R.id.AddInformFloor)
        val tableSpinner = view.findViewById<Spinner>(R.id.AddInformTable)

        serviceSpinner.adapter = serviceAdapter
        pickUpSpinner.adapter = pickUpAdapter
        floorSpinner.adapter = floorAdapter
        tableSpinner.adapter = tableAdapter

        // 회원가입 완료 버튼 클릭 리스너 설정
        view.findViewById<Button>(R.id.AddInformFinish).setOnClickListener {
            findNavController().navigate(R.id.action_to_adminFragment) // 관리자 메인 화면으로 이동
        }
        view.findViewById<Button>(R.id.AddInformBack).setOnClickListener {
            findNavController().navigate(R.id.action_to_admin_sign_fragment) // 추가등록 전으로 이동
        }
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
