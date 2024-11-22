package com.example.kiosk02.admin.tables

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.kiosk02.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class PayCheckActivity : Fragment(R.layout.activity_paycheck) {
    private val firestore = FirebaseFirestore.getInstance()
    private lateinit var tableFrame: FrameLayout
    private lateinit var floorSpinner: Spinner
    private lateinit var consumerTextView: TextView  // 소비자 정보를 표시할 TextView
    private var Aemail: String? = null
    private val floorIds = mutableListOf<String>()
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // "back_activity_admins" TextView에 클릭 리스너 설정
        view.findViewById<TextView>(R.id.back_activity_admins).setOnClickListener {
            findNavController().navigate(R.id.action_to_admin_activity) // 관리자 초기 화면으로 이동
        }
    }
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
                        val isSelected =  table.getBoolean("select") ?: false

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


        tableView.setOnClickListener {
            if (isSelected) {
                // 예약된 테이블의 소비자 이메일을 표시
                consumerEmail?.let {
                    // 예: 테이블 클릭 시에 Aemail, floorId, tableId를 모두 넘겨주기
                    consumerTextView.text = "Table ID: $tableId\n Number of People: $tableType\n Reserved by: $it"
                    Aemail?.let { email ->
                        loadOrderDataForTable(email, tableId, floorId)  // 세 번째 인자 floorId 추가
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


    data class MenuItem(
        val menuName: String = "",
        val quantity: Int = 0,
        val totalPrice: Int = 0
    )


    data class OrderItem(
        val consumerEmail: String = "",
        val items: List<MenuItem> = listOf(),
        val orderTime: String = "",
        val orderType: String = "",
        val tableId: String = "",
        val totalAmount: Int = 0
    )

    private fun loadOrderDataForTable(Aemail: String, tableId: String, floorId: String) {
        // Firebase에서 이메일에 대한 특수 문자 처리
        val sanitizedEmail = Aemail.replace(".", "_").replace("#", "_")
            .replace("$", "_").replace("[", "_").replace("]", "_")

        val database = FirebaseDatabase.getInstance().reference
        val tableRef = database.child("admin_orders").child(sanitizedEmail).child(tableId)

        tableRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    val orderList = mutableListOf<OrderItem>()
                    for (orderSnapshot in snapshot.children) {
                        val order = orderSnapshot.getValue(OrderItem::class.java)
                        if (order != null) {
                            orderList.add(order)
                        } else {
                            // 수동으로 값 매핑
                            val consumerEmail =
                                orderSnapshot.child("consumerEmail").value?.toString() ?: ""
                            val items = orderSnapshot.child("items").children.map { itemSnapshot ->
                                MenuItem(
                                    menuName = itemSnapshot.child("menuName")
                                        .getValue(String::class.java) ?: "",
                                    quantity = itemSnapshot.child("quantity")
                                        .getValue(Int::class.java) ?: 0,
                                    totalPrice = itemSnapshot.child("totalPrice")
                                        .getValue(Int::class.java) ?: 0
                                )
                            }
                            val orderTime =
                                orderSnapshot.child("orderTime").value?.toString() ?: ""
                            val orderType =
                                orderSnapshot.child("orderType").value?.toString() ?: ""
                            val totalAmount =
                                orderSnapshot.child("totalAmount").value?.toString()?.toIntOrNull()
                                    ?: 0
                            orderList.add(
                                OrderItem(
                                    consumerEmail,
                                    items,
                                    orderTime,
                                    orderType,
                                    tableId,
                                    totalAmount
                                )
                            )
                        }
                    }
                    showOrderReceiptDialog(
                        orderList,
                        Aemail,
                        floorId,
                        tableId,
                        orderList.firstOrNull()?.consumerEmail
                    )
                } else {
                    Toast.makeText(context, "이 테이블에 대한 주문 내역이 없습니다.", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("PayCheckActivity", "Error loading order data", error.toException())
            }
        })
    }


    private fun showOrderReceiptDialog(orderItems: List<OrderItem>, Aemail: String, floorId: String, tableId: String, consumerEmail: String?) {
        val builder = AlertDialog.Builder(requireContext())

        // 커스텀 레이아웃을 사용하는 다이얼로그 설정
        val view = layoutInflater.inflate(R.layout.dialog_receipt, null)

        val statusButton = view.findViewById<Button>(R.id.status_button)  // 미착석/착석 버튼
        val statusTextView = view.findViewById<TextView>(R.id.status_text) // 상태 표시 TextView
        var isSeated = false  // 착석 여부 초기값
        var status = "미착석"  // 초기 상태: 미착석

        statusTextView.text = status  // 상태 표시 초기값

        // Firestore에서 기존 상태 불러오기
        consumerEmail?.let {
            checkIfTableIsSelected(Aemail, floorId, tableId) { isTableSelected, _ ->
                if (isTableSelected) {
                    status = "착석"
                    isSeated = true
                    statusTextView.text = status
                } else {
                    status = "미착석"
                    isSeated = false
                    statusTextView.text = status
                }
            }

            // Firestore에서 확인 여부 상태 불러오기
            checkIfConfirmed(Aemail, floorId, tableId, it) { isConfirmed ->
                if (isConfirmed) {
                    // 이미 확인을 눌렀으면 버튼 비활성화
                    statusButton.isEnabled = false
                }
            }
        }

        // 상태 변경 버튼 클릭 시
        statusButton.setOnClickListener {
            if (status == "미착석") {
                status = "착석"  // 상태 변경
                isSeated = true
            } else {
                status = "미착석"  // 상태 변경
                isSeated = false
            }
            statusTextView.text = status  // 상태 텍스트 변경
        }

        builder.setView(view)
        builder.setTitle("영수증")

        // 주문 내역 처리
        val stringBuilder = StringBuilder()
        var totalAmountSum = 0
        for (order in orderItems) {
            stringBuilder.append("주문 시간: ${order.orderTime}\n")
            stringBuilder.append("주문 유형: ${order.orderType}\n")
            stringBuilder.append("항목:\n")
            for (item in order.items) {
                stringBuilder.append("- ${item.menuName} x ${item.quantity} = ${item.totalPrice} 원\n")
            }
            stringBuilder.append("총 금액: ${order.totalAmount}원\n\n")
            totalAmountSum += order.totalAmount
        }
        stringBuilder.append("\n전체 총 금액: $totalAmountSum 원")

        builder.setMessage(stringBuilder.toString())

        builder.setPositiveButton("확인") { dialog, _ ->
            // 상태 변경 시
            consumerEmail?.let { email ->
                if (isSeated) {
                    // 착석 상태일 때 Firestore에 상태 저장
                    updateTableSelection(Aemail, floorId, tableId, email, isSeated)
                } else {
                    // 미착석 상태일 때 예약 삭제(이메일 삭제)
                    //비동기적으로 작동하므로 서로 충돌 발생-> 순서를 real->database로
//                    removeOrderData(Aemail, tableId)
//            removeTableSelection(Aemail, floorId, tableId, email)

// 미착석 상태에서 확인 버튼 클릭 시 호출
                    removeDataSequentially(Aemail, floorId, tableId, email)


                }

                // 확인 후 상태 변경 버튼 비활성화
                updateConfirmationStatus(Aemail, floorId, tableId, email, true)  // 확인 상태를 true로 업데이트
                statusButton.isEnabled = false  // 상태 변경 버튼 비활성화
            }
            dialog.dismiss() // 다이얼로그 종료
        }

        builder.setNegativeButton("취소") { dialog, _ ->
            dialog.dismiss() // 취소 버튼 클릭 시 다이얼로그 종료
        }

        builder.setNeutralButton("결제하기") { dialog, _ ->
            // 결제하기 버튼 클릭 시 처리
            if(!statusButton.isEnabled){
                saveTableDataToFirestore(Aemail, tableId, consumerEmail!!,floorId)

                dialog.dismiss() // 결제 후 다이얼로그 종료
            }
        else{
                Toast.makeText(requireContext(), "착석 상태를 확인해야 결제 가능합니다.", Toast.LENGTH_SHORT).show()
            }
        }
//  removeDataSequentially(Aemail, floorId, tableId, email)
        // 다이얼로그 생성 후 버튼 텍스트 변경
        val dialog = builder.create()
        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            val neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL)

            // 상태에 따라 버튼 텍스트 변경
            if (status == "착석") {
                positiveButton.text = "결제하기"
                neutralButton.visibility = View.GONE  // "결제하기" 버튼만 보이도록 설정
                positiveButton.setOnClickListener {


                    dialog.dismiss()
                }
            }
        }

        dialog.show()
    }








    // 소비자 이메일을 삭제하는 함수
    private fun removeTableSelection(Aemail: String, floorId: String, tableId: String, consumerEmail: String) {
        val tableRef = firestore.collection("admin/$Aemail/floors/$floorId/tables/$tableId/select")

        // 미착석 상태에서 테이블 예약을 취소
        tableRef.document(consumerEmail)  // 소비자 이메일을 문서 ID로 사용
            .delete()  // 예약 취소 -> 해당 이메일 문서 삭제
            .addOnSuccessListener {
                Log.d("PayCheckActivity", "Selection for table $tableId removed successfully for $consumerEmail")
            }
            .addOnFailureListener { e ->
                Log.e("PayCheckActivity", "Error removing selection for table $tableId", e)
            }
    }


    private fun updateTableSelection(Aemail: String, floorId: String, tableId: String, consumerEmail: String, isSeated: Boolean) {
        val tableRef = firestore.collection("admin/$Aemail/floors/$floorId/tables/$tableId/select")

        // 착석 상태에 따른 처리
        if (isSeated) {
            // 테이블이 착석 상태로 변경되면 select 값을 true로 설정하고, 소비자 이메일을 문서 ID로 추가
            tableRef.document(consumerEmail)  // 소비자 이메일을 문서 ID로 사용
                .update(mapOf(
//                    "selected" to true, -> update로 바꿔줌 새로 갱신되니까 자리 선택이 가능하게 되어버림
                    "seat" to "착석"  // 착석 상태로 저장
                ))
                .addOnSuccessListener {
                    Log.d("PayCheckActivity", "Table selection updated successfully for $consumerEmail")
                }
                .addOnFailureListener { e ->
                    Log.e("PayCheckActivity", "Error updating selection for table $tableId", e)
                }
        } else {
            // 테이블이 미착석 상태로 변경되면 select 컬렉션에서 해당 이메일 삭제하고, seat 필드도 제거
            tableRef.document(consumerEmail)  // 소비자 이메일을 문서 ID로 사용
                .delete()  // 소비자 이메일 문서 삭제
                .addOnSuccessListener {
                    Log.d("PayCheckActivity", "Selection for table $tableId removed successfully for $consumerEmail")
                }
                .addOnFailureListener { e ->
                    Log.e("PayCheckActivity", "Error removing selection for table $tableId", e)
                }

            // Firestore의 seat 값도 "미착석"으로 변경
            tableRef.document(consumerEmail)  // 소비자 이메일을 문서 ID로 사용
                .set(hashMapOf(
                    "selected" to false,
                    "seat" to "미착석"  // 미착석 상태로 저장
                ))
                .addOnSuccessListener {
                    Log.d("PayCheckActivity", "Table selection updated to '미착석' for $consumerEmail")
                }
                .addOnFailureListener { e ->
                    Log.e("PayCheckActivity", "Error updating selection to '미착석' for table $tableId", e)
                }
        }
    }

    private fun updateConfirmationStatus(Aemail: String, floorId: String, tableId: String, consumerEmail: String, isConfirmed: Boolean) {
        val tableRef = firestore.collection("admin")
            .document(Aemail)
            .collection("floors")
            .document(floorId)
            .collection("tables")
            .document(tableId)
            .collection("select")
            .document(consumerEmail)

        // Firestore에 isConfirmed 필드 값 업데이트
        tableRef.update("isConfirmed", isConfirmed)
            .addOnSuccessListener {
                Log.d("Dialog", "Confirmation status updated successfully.")
            }
            .addOnFailureListener { e ->
                Log.e("Dialog", "Error updating confirmation status.", e)
            }
    }



    private fun checkIfConfirmed(Aemail: String, floorId: String, tableId: String, consumerEmail: String, callback: (Boolean) -> Unit) {
        val tableRef = firestore.collection("admin")
            .document(Aemail)
            .collection("floors")
            .document(floorId)
            .collection("tables")
            .document(tableId)
            .collection("select")
            .document(consumerEmail)

        tableRef.get().addOnSuccessListener { document ->
            val isConfirmed = document.getBoolean("isConfirmed") ?: false
            callback(isConfirmed)
        }.addOnFailureListener { e ->
            Log.e("Dialog", "Error fetching confirmation status.", e)
            callback(false)
        }
    }

    fun sanitizeEmail(email: String): String {
        return email.replace(".", "_")  // 점을 언더스코어로 변경 .이 안됌
    }


    private fun removeOrderData(Aemail: String, tableId: String, onComplete: () -> Unit) {
        val sanitizedEmail = sanitizeEmail(Aemail)  // 이메일을 안전하게 변환
        val dbRef = FirebaseDatabase.getInstance().getReference("admin_orders")

        // 테이블 데이터를 삭제
        dbRef.child(sanitizedEmail) // 이메일 경로
            .child(tableId) //
            .removeValue()  // 해당 테이블과 관련된 모든 데이터 삭제
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("Table Removal", "Table data removed successfully.")
                    onComplete()  // 삭제 완료 후 콜백 호출
                } else {
                    Log.d("Table Removal", "Failed to remove table data.")
                }
            }
    }


    private fun removeDataSequentially(Aemail: String, floorId: String, tableId: String, consumerEmail: String) {
        // 1. 리얼타임 데이터 삭제 (removeOrderData)
        removeOrderData(Aemail, tableId) {
            // 2. 리얼타임 데이터 삭제가 완료되면 Firestore 데이터 삭제
            removeTableSelection(Aemail, floorId, tableId, consumerEmail)
            //비동기적 삭제 처리 문제 해결했음
        }

    }
//
fun saveTableDataToFirestore(
    Aemail: String, // 현재 로그인한 관리자 이메일
    tableId: String,
    consumerEmail: String,
    floorId: String // 추가: removeDataSequentially에서 필요한 매개변수
) {
    val database = FirebaseDatabase.getInstance().getReference("admin_orders")
    val firestore = FirebaseFirestore.getInstance()


    val sanitizedEmail = sanitizeEmail(Aemail) // '.' -> '_' 변환

    val originalEmail = Aemail

    val currentDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date()) // 오늘 날짜
    val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date()) // 현재 시간

    database.child(sanitizedEmail).child(tableId).get().addOnSuccessListener { snapshot ->
        if (snapshot.exists()) {
            val tableData = snapshot.value


            firestore.collection("admin")
                .document(originalEmail)
                .collection("checksales")
                .document(currentDate)
                .collection("orders")
                .document(currentTime)
                .set(tableData!!)
                .addOnSuccessListener {
                    Log.d("CheckSales", "Table data saved to Firestore successfully.")


                    removeDataSequentially(Aemail, floorId, tableId, consumerEmail)
                }
                .addOnFailureListener { e ->
                    Log.e("CheckSales", "Error saving table data to Firestore.", e)
                }
        } else {
            Log.d("CheckSales", "No data found for table $tableId in Realtime Database.")
        }
    }.addOnFailureListener { e ->
        Log.e("CheckSales", "Error fetching table data from Realtime Database.", e)
    }
}









}


