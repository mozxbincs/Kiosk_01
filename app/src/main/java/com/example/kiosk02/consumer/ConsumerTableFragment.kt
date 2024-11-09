package com.example.kiosk02.consumer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.kiosk02.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConsumerTableFragment : Fragment(R.layout.activity_consumer_table) {
    private lateinit var tableFrame: FrameLayout
    private lateinit var floorSpinner: Spinner
    private lateinit var orderButton: Button
    private val firestore = FirebaseFirestore.getInstance()
    private var Aemail: String? = null
    private var Uemail: String? = null
    private var selectedTableId: String? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        Aemail = arguments?.getString("Aemail")
        Uemail = arguments?.getString("Uemail")
        Log.d("ConsumerTableFragment", "Aemail retrieved: $Aemail")
        Log.d("ConsumerTableFragment", "Uemail retrieved:$Uemail")
        tableFrame = view.findViewById(R.id.table_frame)
        floorSpinner = view.findViewById(R.id.floor_spinner)
        orderButton = view.findViewById(R.id.update_button)


        if (Aemail != null) {
            loadFloorsFromFirestore(Aemail!!)
        } else {
            Log.e("ConsumerTableFragment", "Aemail is null")
        }

        Log.d("ConsumerTableFragment", "floorSpinner initialized")

        floorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedFloor = parent.getItemAtPosition(position) as String
                Log.d("ConsumerTableFragment", "Selected floor: $selectedFloor")

                if (Aemail != null) {
                    loadTablesFromFirestore(Aemail!!, selectedFloor)
                }
            }

            override fun onNothingSelected(parent: AdapterView<*>) {

                Log.d("ConsumerTableFragment", "No floor selected")
            }
        }


        orderButton.setOnClickListener {
            if (selectedTableId.isNullOrEmpty()) {
                Toast.makeText(requireContext(), "테이블을 선택해주세요.", Toast.LENGTH_SHORT).show()
                Log.d("ConsumerTableFragment", "No table selected for order")
            } else {
                Log.d("ConsumerTableFragment", "Order button clicked. Selected table: $selectedTableId")
                saveOrderToFirestore(selectedTableId!!)
            }
        }
    }

    private fun loadFloorsFromFirestore(Aemail: String) {
        Log.d("ConsumerTableFragment", "Loading floors for Aemail: $Aemail")

        firestore.collection("admin/$Aemail/floors").get()
            .addOnSuccessListener { result ->
                val floors = mutableListOf<String>()
                for (document in result) {
                    floors.add(document.id) // 층 ID를 리스트에 추가
                }
                Log.d("ConsumerTableFragment", "Loaded floors: $floors")

                // 기본값으로 1층
                if (floors.isNotEmpty()) {
                    val adapter =
                        ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, floors)
                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                    floorSpinner.adapter = adapter


                    val defaultFloor = if ("1층" in floors) "1층" else floors.first()
                    val position = floors.indexOf(defaultFloor)
                    floorSpinner.setSelection(position) // 기본값 선택
                    Log.d("ConsumerTableFragment", "Default floor set to: $defaultFloor")
                    loadTablesFromFirestore(Aemail, defaultFloor)
                } else {
                    Log.e("ConsumerTableFragment", "No floors found for Aemail: $Aemail")
                }
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error loading floors", e)
            }
    }

    private fun loadTablesFromFirestore(Aemail: String, selectedFloor: String) {

        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserEmail == null) {
            Log.e("ConsumerTableFragment", "User not logged in or no email found")
            return
        }

        Log.d("ConsumerTableFragment", "Loading tables for floor: $selectedFloor in Aemail: $Aemail")

        tableFrame.removeAllViews() // 이전 테이블 뷰 삭제
        firestore.collection("admin/$Aemail/floors/$selectedFloor/tables")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.d("ConsumerTableFragment", "No tables found for floor: $selectedFloor")
                } else {
                    // 테이블마다 select 상태를 가져와서 처리
                    for (document in result) {
                        val tableId = document.id
                        val tableType = document.getString("tableType") ?: "Unknown"

                        // 테이블의 위치 정보 가져오기
                        val tableX = document.getDouble("x")?.toFloat() ?: 0f
                        val tableY = document.getDouble("y")?.toFloat() ?: 0f

                        // select 상태 가져오기 (사용자의 이메일로 접근)
                        firestore.collection("admin/$Aemail/floors/$selectedFloor/tables/$tableId/select")
                            .document(currentUserEmail) // 사용자의 이메일을 사용
                            .get()
                            .addOnSuccessListener { selectDoc ->
                                val isSelected = selectDoc.getBoolean("select") ?: false
                                Log.d("ConsumerTableFragment", "Table $tableId select state: $isSelected")

                                val tableWidth = 50f * resources.displayMetrics.density
                                val tableHeight = 50f * resources.displayMetrics.density

                                // 선택된 테이블에 대한 뷰 생성
                                val tableView = createTableView(
                                    tableType, tableId, tableX, tableY, tableWidth, tableHeight, isSelected, selectedFloor
                                )

                                tableFrame.addView(tableView)
                            }
                            .addOnFailureListener { e ->
                                Log.e("ConsumerTableFragment", "Error fetching select state for table $tableId", e)
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error loading tables", e)
            }
    }

    // 테이블 뷰를 생성하는 메서드
    private fun createTableView(
        tableType: String,
        tableId: String,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        isSelected: Boolean,
        selectedFloor: String
    ): View {
        Log.d("ConsumerTableFragment", "Creating table view for table: $tableId")

        val tableView = LayoutInflater.from(requireContext()).inflate(R.layout.table_item, null)
        val params = FrameLayout.LayoutParams(width.toInt(), height.toInt())
        tableView.layoutParams = params

        val tableNameTextView = tableView.findViewById<TextView>(R.id.table_name)

        // 테이블 이름 설정
        tableNameTextView.text = tableType

        // 테이블 위치 설정
        tableView.x = x
        tableView.y = y

        // 테이블이 선택되었을 때의 UI 처리
        if (isSelected) {
            tableNameTextView.text = "X" // "X"로 표시
            tableView.alpha = 0.5f // 예약된 테이블 흐리게 표시
        }

        // 테이블 클릭 리스너 추가
        tableView.setOnClickListener {
            if (selectedTableId != null) {
                // 이미 선택된 테이블이 있으면 선택 취소 먼저
                if (selectedTableId != tableId) {
                    // 이미 선택된 테이블이 있으면 클릭할 수 없도록 함
                    Toast.makeText(requireContext(), "이미 테이블이 선택되었습니다. 선택을 취소하고 다른 테이블을 선택하세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
            }

            // 선택되지 않은 테이블을 클릭한 경우, 새로 선택
            if (isSelected) {
                // 이미 선택된 테이블을 클릭한 경우, 선택 취소
                deselectTable(tableId, selectedFloor)
            } else {
                // 새 테이블을 선택
                selectTable(tableId, selectedFloor)
            }
        }

        return tableView
    }


    private fun selectTable(tableId: String, selectedFloor: String) {
        if (selectedTableId != null) {
            // 이미 하나의 테이블이 선택되어 있으면 다른 테이블을 선택할 수 없도록 처리
            Toast.makeText(requireContext(), "이미 선택된 테이블이 있습니다. 기존 테이블을 취소하고 선택하세요.", Toast.LENGTH_SHORT).show()
            return
        }

        // 새 테이블을 선택
        firestore.collection("admin/$Aemail/floors/$selectedFloor/tables/$tableId/select")
            .document(Uemail!!)
            .set(hashMapOf("select" to true))
            .addOnSuccessListener {
                Log.d("ConsumerTableFragment", "Table $tableId selected")
                selectedTableId = tableId
                loadTablesFromFirestore(Aemail!!, selectedFloor)
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error selecting table $tableId", e)
            }
    }

    private fun deselectTable(tableId: String, selectedFloor: String) {
        firestore.collection("admin/$Aemail/floors/$selectedFloor/tables/$tableId/select")
            .document(Uemail!!)
            .set(hashMapOf("select" to false))
            .addOnSuccessListener {
                Log.d("ConsumerTableFragment", "Table $tableId deselected")
                selectedTableId = null // 선택된 테이블 ID 초기화
                loadTablesFromFirestore(Aemail!!, selectedFloor)
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error deselecting table $tableId", e)
            }
    }




    private fun saveOrderToFirestore(tableId: String) {

    }
}
