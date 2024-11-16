package com.example.kiosk02.admin

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.kiosk02.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
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
        // 테이블 ID에서 "table_"을 제거하고 숫자만 표시
        val tableNumber = tableId.replace("table_", "")
        tableNameTextView.text = tableNumber

        // 테이블 색상 설정
        if (isSelected) {
            tableView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.blue01))  // 예약된 테이블 색상
        } else {
            tableView.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.yellow))  // 예약되지 않은 테이블 색상
        }

        // 테이블 클릭 시 소비자 정보 표시
        // 테이블 클릭 시 소비자 정보 표시
        // 테이블 클릭 시 소비자 정보 표시
        tableView.setOnClickListener {
            if (isSelected) {
                // 예약된 테이블의 소비자 이메일을 표시
                consumerEmail?.let {
                    consumerTextView.text = "Table ID: $tableId\n Number of People: $tableType\n Reserved by: $it"
                    // Aemail이 null이 아니면 loadOrderDataForTable 호출
                    Aemail?.let { email ->
                        loadOrderDataForTable(email, tableId)
                    } ?: run {
                        Log.e("PaycheckActivity", "Aemail is null")  // Aemail이 null일 때 로그 출력
                    }
                }
            } else {
                // 예약되지 않은 테이블일 경우
                consumerTextView.text = ""  // 비워두기
            }
        }



        return tableView
    }
    //
    // MenuItem 클래스 정의
    data class MenuItem(
        val menuName: String = "",
        val quantity: Int = 0,
        val totalPrice: Int = 0
    )

    // OrderItem 클래스 정의 (items를 List<MenuItem>으로 변경)
    data class OrderItem(
        val consumerEmail: String = "",
        val items: List<MenuItem> = listOf(),  // List<MenuItem>로 변경
        val orderTime: String = "",
        val orderType: String = "",
        val tableId: String = "",
        val totalAmount: Int = 0
    )

    private fun loadOrderDataForTable(Aemail: String, tableId: String) {
        // Firebase에서 이메일에 대한 특수 문자 처리
        val sanitizedEmail = Aemail.replace(".", "_").replace("#", "_")
            .replace("$", "_").replace("[", "_").replace("]", "_")

        val database = FirebaseDatabase.getInstance().reference
        val ordersRef = database.child("admin_orders").child(sanitizedEmail)

        val query = ordersRef.orderByChild("tableId").equalTo(tableId)

        query.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val orderList = mutableListOf<OrderItem>()
                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(OrderItem::class.java)
                        if (order != null) {
                            orderList.add(order)  // 정상적으로 데이터가 매핑되면 추가
                        } else {
                            // 수동으로 값 매핑
                            val consumerEmail = orderSnapshot.child("consumerEmail").value?.toString() ?: ""
                            val items = orderSnapshot.child("items").children.map { itemSnapshot ->
                                MenuItem(
                                    menuName = itemSnapshot.child("menuName").getValue(String::class.java) ?: "",
                                    quantity = itemSnapshot.child("quantity").getValue(Int::class.java) ?: 0,
                                    totalPrice = itemSnapshot.child("totalPrice").getValue(Int::class.java) ?: 0
                                )
                            }

                            val orderTime = orderSnapshot.child("orderTime").value?.toString() ?: ""
                            val orderType = orderSnapshot.child("orderType").value?.toString() ?: ""
                            val totalAmount = orderSnapshot.child("totalAmount").value?.toString()?.toIntOrNull() ?: 0
                            orderList.add(OrderItem(consumerEmail, items, orderTime, orderType, tableId, totalAmount))
                        }
                    }
                    showOrderReceiptDialog(orderList)
                } else {
                    Toast.makeText(context, "이 테이블에 대한 주문 내역이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PayCheckActivity", "Error loading order data", error.toException())
            }
        })
    }

    private fun showOrderReceiptDialog(orderItems: List<OrderItem>) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("영수증")

        val stringBuilder = StringBuilder()
        var totalAmountSum = 0

        // 각 주문에 대해 처리
        for (order in orderItems) {
            stringBuilder.append("주문 시간: ${order.orderTime}\n")
            stringBuilder.append("주문 유형: ${order.orderType}\n")
            stringBuilder.append("항목:\n")

            // 각 항목을 MenuItem 객체로 처리
            for (item in order.items) {
                val menuName = item.menuName  // MenuItem 객체에서 값 가져오기
                val quantity = item.quantity  // MenuItem 객체에서 값 가져오기
                val totalPrice = item.totalPrice  // MenuItem 객체에서 값 가져오기
                stringBuilder.append("- $menuName x $quantity = $totalPrice 원\n")
            }

            stringBuilder.append("총 금액: ${order.totalAmount}원\n\n")
            totalAmountSum += order.totalAmount
        }

        stringBuilder.append("\n전체 총 금액: $totalAmountSum 원")

        builder.setMessage(stringBuilder.toString())

        builder.setPositiveButton("확인") { dialog, _ ->
            dialog.dismiss() // 다이얼로그 닫기
        }

        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss() // 다이얼로그 닫기
        }

        builder.show() // 다이얼로그 표시
    }


}


