package com.example.kiosk02.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.kiosk02.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
class PayCheckActivity : Fragment(R.layout.activity_paycheck) {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var tableFrame: FrameLayout
    private lateinit var floorSpinner: Spinner
    private lateinit var consumerTextView: TextView  // 소비자 정보를 표시할 TextView
    private var Aemail: String? = null
    private val floorIds = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.activity_paycheck, container, false)
        tableFrame = rootView.findViewById(R.id.table_frame)
        floorSpinner = rootView.findViewById(R.id.floor_spinner)
        consumerTextView = rootView.findViewById(R.id.consumer_info)  // 소비자 정보 TextView 가져오기

        Aemail = FirebaseAuth.getInstance().currentUser?.email

        if (Aemail != null) {
            loadFloorsFromFirestore(Aemail!!)
        } else {
            Log.e("PaycheckActivity", "Aemail is null")
        }

        return rootView
    }

    private fun loadFloorsFromFirestore(Aemail: String) {
        firestore.collection("admin/$Aemail/floors")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val floorId = document.id
                    floorIds.add(floorId)
                }

                val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_item, floorIds)
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                floorSpinner.adapter = adapter

                floorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                        val selectedFloorId = floorIds[position]
                        loadTablesForFloor(Aemail, selectedFloorId)
                    }

                    override fun onNothingSelected(parent: AdapterView<*>) {}
                }
            }
            .addOnFailureListener { e ->
                Log.e("PaycheckActivity", "Error loading floors", e)
            }
    }

    private fun loadTablesForFloor(Aemail: String, floorId: String) {
        tableFrame.removeAllViews()

        firestore.collection("admin/$Aemail/floors/$floorId/tables")
            .get()
            .addOnSuccessListener { tables ->
                if (!tables.isEmpty) {
                    for (table in tables) {
                        val tableId = table.id
                        val tableType = table.getString("tableType") ?: "Unknown"
                        val tableX = table.getDouble("x")?.toFloat() ?: 0f
                        val tableY = table.getDouble("y")?.toFloat() ?: 0f
                        val isSelected = table.getBoolean("isSelected") ?: false

                        // check if table is selected
                        checkIfTableIsSelected(Aemail, floorId, tableId) { isTableSelected, consumerEmail ->
                            val tableView = createTableView(tableType, tableId, floorId, tableX, tableY, isTableSelected, consumerEmail)
                            tableFrame.addView(tableView)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PaycheckActivity", "Error loading tables for floor $floorId", e)
            }
    }

    private fun checkIfTableIsSelected(Aemail: String, floorId: String, tableId: String, callback: (Boolean, String?) -> Unit) {
        firestore.collection("admin/$Aemail/floors/$floorId/tables/$tableId/select")
            .get()
            .addOnSuccessListener { result ->
                if (!result.isEmpty) {
                    // 예약된 테이블은 select 컬렉션에 문서가 존재
                    val consumerEmail = result.documents.firstOrNull()?.id
                    callback(true, consumerEmail)  // 예약된 테이블과 소비자 이메일 반환
                } else {
                    callback(false, null)  // 예약되지 않은 테이블
                }
            }
            .addOnFailureListener { e ->
                Log.e("PaycheckActivity", "Error checking table selection for $tableId", e)
                callback(false, null)  // 오류 발생 시 예약되지 않은 상태로 처리
            }
    }

    private fun createTableView(
        tableType: String,
        tableId: String,
        floorId: String,
        x: Float,
        y: Float,
        isSelected: Boolean,
        consumerEmail: String?
    ): View {
        val tableView = LayoutInflater.from(context).inflate(R.layout.table_item, null)
        val params = FrameLayout.LayoutParams(100, 100)
        tableView.layoutParams = params
        tableView.x = x
        tableView.y = y

        val tableNameTextView = tableView.findViewById<TextView>(R.id.table_name)
        tableNameTextView.text = tableType

        if (isSelected) {
            tableView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue01))  // 예약된 테이블 색상
            consumerEmail?.let {
                // 예약된 테이블의 소비자 이메일 표시
                consumerTextView.text = "Reserved by: $it"
            }
        } else {
            tableView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow))  // 예약되지 않은 테이블 색상
            consumerTextView.text = ""  // 소비자 정보 초기화
        }

        tableView.setOnClickListener {
            if (!isSelected) {
                firestore.collection("admin/$Aemail/floors/$floorId/tables/$tableId/select")
                    .document(Aemail!!)  // 선택한 소비자의 이메일을 추가
                    .set(mapOf("select" to true))  // 테이블 예약
                    .addOnSuccessListener {
                        tableView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue01))
                        Toast.makeText(context, "Table $tableId selected", Toast.LENGTH_SHORT).show()
                        consumerTextView.text = "Reserved by: $Aemail"  // 선택된 테이블의 소비자 정보 표시
                    }
                    .addOnFailureListener { e ->
                        Log.e("PaycheckActivity", "Error updating table selection", e)
                    }
            }
        }

        return tableView
    }
}

