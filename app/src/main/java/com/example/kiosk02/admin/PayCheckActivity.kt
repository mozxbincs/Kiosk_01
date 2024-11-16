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
    private lateinit var tableFrame: FrameLayout  // FrameLayout을 그대로 사용
    private lateinit var floorSpinner: Spinner
    private var Aemail: String? = null
    private val floorIds = mutableListOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val rootView = inflater.inflate(R.layout.activity_paycheck, container, false)
        tableFrame = rootView.findViewById(R.id.table_frame)  // FrameLayout을 가져옴
        floorSpinner = rootView.findViewById(R.id.floor_spinner)

        // 현재 로그인된 사용자의 이메일을 가져오기
        Aemail = FirebaseAuth.getInstance().currentUser?.email

        if (Aemail != null) {
            loadFloorsFromFirestore(Aemail!!)
        } else {
            Log.e("PaycheckActivity", "Aemail is null")
        }

        return rootView
    }

    private fun loadFloorsFromFirestore(Aemail: String) {
        Log.d("PaycheckActivity", "Loading floors for Aemail: $Aemail")

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
        Log.d("PaycheckActivity", "Loading tables for floor: $floorId")

        tableFrame.removeAllViews()  // 기존 테이블을 제거

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

                        val tableView = createTableView(tableType, tableId, floorId, tableX, tableY, isSelected)
                        tableFrame.addView(tableView)
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("PaycheckActivity", "Error loading tables for floor $floorId", e)
            }
    }

    private fun createTableView(
        tableType: String,
        tableId: String,
        floorId: String,
        x: Float,
        y: Float,
        isSelected: Boolean
    ): View {
        val tableView = LayoutInflater.from(context).inflate(R.layout.table_item, null)
        val params = FrameLayout.LayoutParams(100, 100)  // 테이블의 크기
        tableView.layoutParams = params
        tableView.x = x
        tableView.y = y

        val tableNameTextView = tableView.findViewById<TextView>(R.id.table_name)
        tableNameTextView.text = tableType  // 테이블의 이름

        // 선택된 테이블은 다른 색상으로 표시
        if (isSelected) {
            tableView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue01))  // 선택된 테이블 색상
        } else {
            tableView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow))  // 기본 테이블 색상
        }

        // 클릭 시 선택 상태 변경
        tableView.setOnClickListener {
            if (!isSelected) {
                firestore.collection("admin/$Aemail/floors/$floorId/tables").document(tableId)
                    .update("isSelected", true)
                    .addOnSuccessListener {
                        tableView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue01))
                        Toast.makeText(context, "Table $tableId selected", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("PaycheckActivity", "Error updating table selection", e)
                    }
            }
        }

        return tableView
    }
}
