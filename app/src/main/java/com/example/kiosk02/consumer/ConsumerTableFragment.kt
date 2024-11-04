package com.example.kiosk02.consumer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.kiosk02.R
import com.google.firebase.firestore.FirebaseFirestore

class ConsumerTableFragment : Fragment(R.layout.activity_consumer_table) {
    private lateinit var tableFrame: FrameLayout
    private lateinit var floorSpinner: Spinner
    private val firestore = FirebaseFirestore.getInstance()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        tableFrame = view.findViewById(R.id.table_frame)
        floorSpinner = view.findViewById(R.id.floor_spinner)

        // 층 데이터를 불러오는 메서드 호출
        loadFloorsFromFirestore()

        floorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedFloor = parent.getItemAtPosition(position) as String
                loadTablesFromFirestore(selectedFloor) // 선택된 층에 따라 테이블 불러오기
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 선택된 층이 없을 때 처리
            }
        }
    }

    private fun loadFloorsFromFirestore() {
        firestore.collection("admin/aaa@aaa.aaa/floors").get()
            .addOnSuccessListener { result ->
                val floors = mutableListOf<String>()
                for (document in result) {
                    floors.add(document.id) // 층 ID를 리스트에 추가
                }
                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, floors)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                floorSpinner.adapter = adapter // 스피너에 층 데이터 설정
            }
    }

    private fun loadTablesFromFirestore(selectedFloor: String) {
        tableFrame.removeAllViews() // 이전 테이블 뷰 삭제
        firestore.collection("admin/aaa@aaa.aaa/floors/$selectedFloor/tables").get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val tableType = document.getString("tableType") ?: "" // 테이블 타입 가져오기
                    val tableId = document.id

                    // 테이블의 위치와 크기 가져오기
                    val tableX = document.getDouble("x")?.toFloat() ?: 0f // x 위치
                    val tableY = document.getDouble("y")?.toFloat() ?: 0f // y 위치
                    val tableWidth = (document.getDouble("width")?.toFloat() ?: 50f) * resources.displayMetrics.density
                    val tableHeight = (document.getDouble("height")?.toFloat() ?: 50f) * resources.displayMetrics.density

                    // 테이블 뷰 생성 (매개변수 추가)
                    val tableView = createTableView(tableType, tableId, tableX, tableY, tableWidth, tableHeight)

                    // 테이블 뷰를 테이블 프레임에 추가
                    tableFrame.addView(tableView)
                }
            }
    }


    private fun createTableView(tableType: String, tableId: String, x: Float, y: Float, width: Float, height: Float): View {
        // 테이블 뷰 레이아웃을 인플레이트
        val tableView = LayoutInflater.from(requireContext()).inflate(R.layout.table_item, null)

        // 테이블 크기 및 위치 설정
        val params = FrameLayout.LayoutParams(width.toInt(), height.toInt())
        tableView.layoutParams = params // 레이아웃 매개변수 설정

        // 테이블 타입을 표시할 TextView 찾기
        val tableNameTextView = tableView.findViewById<TextView>(R.id.table_name)
        tableNameTextView.text = tableType // 테이블 타입을 설정

        // 테이블의 위치를 설정
        tableView.x = x // x 위치 설정
        tableView.y = y // y 위치 설정

        return tableView
    }

}
