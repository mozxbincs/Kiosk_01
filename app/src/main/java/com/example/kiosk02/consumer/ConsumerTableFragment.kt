package com.example.kiosk02.consumer

import android.net.Uri
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
    private var Aemail: String? = null // Aemail을 저장할 변수

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bundle에서 Aemail 값을 가져오기
        Aemail = arguments?.getString("Aemail")

        tableFrame = view.findViewById(R.id.table_frame)
        floorSpinner = view.findViewById(R.id.floor_spinner)

        // Aemail이 전달되었으면 층 데이터를 불러오는 메서드 호출
        if (Aemail != null) {
            loadFloorsFromFirestore(Aemail!!)  // Aemail을 인자로 전달하여 층 정보 로드
        } else {
            Log.e("ConsumerTableFragment", "Aemail is null")
        }

        Log.d("ConsumerTableFragment", "floorSpinner initialized")

        floorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                val selectedFloor = parent.getItemAtPosition(position) as String
                Log.d("ConsumerTableFragment", "Selected floor: $selectedFloor")

                if (Aemail != null) {
                    loadTablesFromFirestore(Aemail!!, selectedFloor) // 선택된 층에 맞는 테이블 불러오기
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                // 선택된 층이 없을 때 처리
                Log.d("ConsumerTableFragment", "No floor selected")
            }
        }
    }

    private fun loadFloorsFromFirestore(Aemail: String) {
        firestore.collection("admin/$Aemail/floors").get()
            .addOnSuccessListener { result ->
                val floors = mutableListOf<String>()
                for (document in result) {
                    floors.add(document.id) // 층 ID를 리스트에 추가
                }
                Log.d("ConsumerTableFragment", "Loaded floors: $floors")

                // 기본값으로 1층 선택
                if (floors.isNotEmpty()) {
                    val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, floors)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    floorSpinner.adapter = adapter // 스피너에 층 데이터 설정

                    // 기본값을 1층으로 설정
                    val defaultFloor = if ("1층" in floors) "1층" else floors.first()
                    val position = floors.indexOf(defaultFloor)
                    floorSpinner.setSelection(position) // 기본값 선택
                    loadTablesFromFirestore(Aemail, defaultFloor) // 기본값에 맞는 테이블 로드
                } else {
                    Log.e("ConsumerTableFragment", "No floors found for Aemail: $Aemail")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error loading floors", e)
            }
    }

    private fun loadTablesFromFirestore(Aemail: String, selectedFloor: String) {
        tableFrame.removeAllViews() // 이전 테이블 뷰 삭제
        firestore.collection("admin/$Aemail/floors/$selectedFloor/tables").get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.d("ConsumerTableFragment", "No tables found for floor: $selectedFloor")
                } else {
                    for (document in result) {
                        val tableType = document.getString("tableType") ?: "Unknown" // 테이블 타입 가져오기
                        val tableId = document.id

                        // 테이블의 위치 가져오기
                        val tableX = document.getDouble("x")?.toFloat() ?: 0f // x 위치
                        val tableY = document.getDouble("y")?.toFloat() ?: 0f // y 위치

                        // 테이블 크기는 고정된 50f
                        val tableWidth = 50f * resources.displayMetrics.density
                        val tableHeight = 50f * resources.displayMetrics.density

                        // 테이블 뷰 생성 (매개변수 추가)
                        val tableView = createTableView(tableType, tableId, tableX, tableY, tableWidth, tableHeight)



                        // 테이블 뷰를 테이블 프레임에 추가
                        tableFrame.addView(tableView)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error loading tables", e)
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
