package com.example.kiosk02.consumer

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ConsumerTableFragment : Fragment(R.layout.activity_consumer_table) {
    private lateinit var tableFrame: FrameLayout
    private lateinit var floorSpinner: Spinner
    private lateinit var orderButton: LinearLayout
    private val firestore = FirebaseFirestore.getInstance()
    private var Aemail: String? = null
    private var Uemail: String? = null
    private var selectedTableId: String? = null
    private var isOrderConfirmed = false


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
                Log.d(
                    "ConsumerTableFragment",
                    "Order button clicked. Selected table: $selectedTableId"
                )
                saveOrderToFirestore(selectedTableId!!)

                // 주문 완료 상태로 설정
                isOrderConfirmed = true

                val bundle = Bundle().apply {
                    putString("selectedTableId", selectedTableId)
                    putString("Aemail", Aemail)
                    putString("Uemail", Uemail)
                    putString("orderType", arguments?.getString("orderType"))
                }
                findNavController().navigate(R.id.action_to_ConsumerMenuList, bundle)
            }
        }


        view.findViewById<TextView>(R.id.back_activity_consumer).setOnClickListener {
            findNavController().navigate(R.id.action_to_pickupFragment)
        }

    }

    override fun onPause() {
        super.onPause()
        if (!isOrderConfirmed && selectedTableId != null) {
            removeTableSelection(selectedTableId!!)
        }
    }
    private fun removeTableSelection(tableId: String) {
        firestore.collection("admin/$Aemail/floors/${floorSpinner.selectedItem}/tables/$tableId/select")
            .document(Uemail!!)
            .delete() // 선택 데이터 삭제
            .addOnSuccessListener {
                Log.d("ConsumerTableFragment", "Selection for table $tableId removed")
                selectedTableId = null // 선택된 테이블 ID 초기화
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error removing selection for table $tableId", e)
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

        Log.d(
            "ConsumerTableFragment",
            "Loading tables for floor: $selectedFloor in Aemail: $Aemail"
        )

        tableFrame.removeAllViews() // 이전 테이블 뷰 삭제
        firestore.collection("admin/$Aemail/floors/$selectedFloor/tables")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    Log.d("ConsumerTableFragment", "No tables found for floor: $selectedFloor")
                } else {

                    for (document in result) {
                        val tableId = document.id
                        val tableType = document.getString("tableType") ?: "Unknown"

                        val tableX = document.getDouble("x")?.toFloat() ?: 0f
                        val tableY = document.getDouble("y")?.toFloat() ?: 0f


                        firestore.collection("admin/$Aemail/floors/$selectedFloor/tables/$tableId/select")
                            .document(currentUserEmail)
                            .get()
                            .addOnSuccessListener { selectDoc ->
                                val isSelected = selectDoc.getBoolean("select") ?: false
                                Log.d(
                                    "ConsumerTableFragment",
                                    "Table $tableId select state: $isSelected"
                                )

                                val tableWidth = 50f * resources.displayMetrics.density
                                val tableHeight = 50f * resources.displayMetrics.density


                                val tableView = createTableView(
                                    tableType,
                                    tableId,
                                    tableX,
                                    tableY,
                                    tableWidth,
                                    tableHeight,
                                    isSelected,
                                    selectedFloor
                                )

                                tableFrame.addView(tableView)
                            }
                            .addOnFailureListener { e ->
                                Log.e(
                                    "ConsumerTableFragment",
                                    "Error fetching select state for table $tableId",
                                    e
                                )
                            }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error loading tables", e)
            }
    }


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
        tableNameTextView.text = tableType
        tableView.x = x
        tableView.y = y

        if (isSelected) {
            // 테이블이 선택된 상태에서 예약자와 현재 사용자를 확인
            if (selectedTableId == tableId) {
                // 현재 사용자가 선택한 테이블 -> 선택 취소 가능
                tableView.alpha = 0.7f
                tableView.setOnClickListener {
                    deselectTable(tableId)
                }
            } else {
                // 다른 사용자가 예약한 테이블은 선택 불가능
                tableNameTextView.text = "X"
                tableView.alpha = 0.5f
                tableView.isClickable = false
            }
        } else {
            // 테이블이 선택되지 않은 경우 -> 현재 사용자가 선택 가능
            tableView.setOnClickListener {
                if (selectedTableId == null) {
                    selectTable(tableId, selectedFloor) // 새로운 테이블 선택
                } else {
                    Toast.makeText(requireContext(), "이미 다른 테이블이 선택되었습니다.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }

        return tableView
    }


    private fun selectTable(tableId: String, selectedFloor: String) {
        firestore.collection("admin/$Aemail/floors/$selectedFloor/tables/$tableId/select")
            .document(Uemail!!)
            .set(hashMapOf("select" to true))
            .addOnSuccessListener {
                Log.d("ConsumerTableFragment", "Table $tableId selected")
                selectedTableId = tableId
                loadTablesFromFirestore(Aemail!!, selectedFloor) // UI 갱신
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error selecting table $tableId", e)
            }
    }

    private fun deselectTable(tableId: String) {
        firestore.collection("admin/$Aemail/floors/${floorSpinner.selectedItem}/tables/$tableId/select")
            .document(Uemail!!)
            .delete()
            .addOnSuccessListener {
                Log.d("ConsumerTableFragment", "Table $tableId deselected")
                selectedTableId = null
                loadTablesFromFirestore(Aemail!!, floorSpinner.selectedItem.toString()) // UI 갱신
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error deselecting table $tableId", e)
            }
    }


    private fun saveOrderToFirestore(tableId: String) {
        val selectedFloor = floorSpinner.selectedItem.toString()
        val consumerTablePath = "consumer/$Uemail/$tableId"  // 사용자 하위의 tableId 컬렉션 경로

        // 1. 선택된 테이블 정보를 "admin" 컬렉션에 업데이트
        firestore.collection("admin/$Aemail/floors/$selectedFloor/tables/$tableId/select")
            .document(Uemail!!)
            .set(hashMapOf("select" to true))
            .addOnSuccessListener {
                Log.d("ConsumerTableFragment", "Order confirmed for table $tableId")
                Toast.makeText(requireContext(), "자리선택이 완료됐습니다.", Toast.LENGTH_SHORT).show()
                selectedTableId = null // 선택을 고정하여 다시 선택할 수 없도록 함

                // 2. "consumer" 컬렉션의 사용자 하위 tableId 컬렉션에 데이터 추가
                val reservationData = hashMapOf(
                    "Aemail" to Aemail,
                    "floor" to selectedFloor,
                    "tableId" to tableId,
                    "timestamp" to System.currentTimeMillis() // 예약 시간 정보
                )

                // 예약 정보를 consumer 문서 경로 아래 tableId 컬렉션에 저장
                firestore.collection(consumerTablePath)
                    .document("reservation_info") //에약 내역
                    .set(reservationData)
                    .addOnSuccessListener {
                        Log.d(
                            "ConsumerTableFragment",
                            "Reservation data added in $consumerTablePath"
                        )
                        loadTablesFromFirestore(Aemail!!, selectedFloor) // UI 갱신
                    }
                    .addOnFailureListener { e ->
                        Log.e("ConsumerTableFragment", "Error adding reservation data", e)
                        Toast.makeText(
                            requireContext(),
                            "데이터 저장 실패. 다시 시도해주세요.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            }
            .addOnFailureListener { e ->
                Log.e("ConsumerTableFragment", "Error confirming order", e)
                Toast.makeText(requireContext(), "자리 선택 실패. 다시 시도해주세요.", Toast.LENGTH_SHORT).show()
            }
    }


}
