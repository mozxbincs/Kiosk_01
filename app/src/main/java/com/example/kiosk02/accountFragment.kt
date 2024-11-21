package com.example.kiosk02

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.icu.util.Calendar
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kiosk02.databinding.FragmentAccountBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson

class accountFragment: Fragment(R.layout.fragment_account) {

    private var _binding: FragmentAccountBinding? = null
    private val binding get() = _binding!!

    private val orderList = mutableListOf<Order>()
    private lateinit var accountAdapter: accountAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentAccountBinding.bind(view)

        accountAdapter = accountAdapter(orderList) { tableId ->
            showOrderDetails(tableId)
        }
        binding.recyclerViewOrders.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = accountAdapter
        }

        binding.buttonPickDate.setOnClickListener {
            showDatePicker()
        }

    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedDate = "$year-${month + 1}-$day"
                binding.textViewSelectedDate.text = selectedDate
                fetchAccountData(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fetchAccountData (selectedDate: String) {
        val firestore = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val ordersMap = mutableMapOf<String, MutableList<Order>>() // tableId를 키로 하는 Map
        val adminEmail = auth.currentUser?.email ?: ""

        if (adminEmail.isEmpty()) {
            Toast.makeText(requireContext(), "관리자 이메일이 설정되지 않았습니다.", Toast.LENGTH_SHORT).show()
            Log.e("AccountFragment", "Admin email is empty.")
            return
        }

        val checksalesCollection = firestore.collection("admin").document(adminEmail).collection("checksales")
        val ordersCollection = checksalesCollection.document(selectedDate).collection("orders")

        ordersCollection.get()
            .addOnSuccessListener { querySnapshot ->
                if (querySnapshot.isEmpty) {
                    Toast.makeText(requireContext(), "선택한 날짜에 주문이 없습니다.", Toast.LENGTH_SHORT).show()
                    orderList.clear()
                    accountAdapter.notifyDataSetChanged()
                    binding.textViewTotalAmount.text = "0 원"
                    Log.d("AccountFragment", "No orders found for the selected date.")
                    return@addOnSuccessListener
                }

                val gson = Gson()
                var totalAmount = 0

                for (document in querySnapshot.documents) {
                    val dataMap = document.data
                    if (dataMap != null) {
                        for ((key, value) in dataMap) {
                            if (value is Map<*, *>) {
                                val orderJson = gson.toJson(value)
                                val order = gson.fromJson(orderJson, Order::class.java)
                                if (order != null) {
                                    ordersMap.getOrPut(order.tableId) { mutableListOf() }.add(order)
                                    totalAmount += order.totalAmount
                                    Log.d("AccountFragment", "Order fetched: $order")
                                } else {
                                    Log.w("AccountFragment", "Order is null for key: $key")
                                }
                            }
                        }
                    } else {
                        Log.w("AccountFragment", "Document data is null for document: ${document.id}")
                    }
                }

                orderList.clear()
                ordersMap.forEach { (tableId, orders) ->
                    val totalAmount = orders.sumOf { it.totalAmount }
                    val combinedOrder = orders.first().copy(
                        totalAmount = totalAmount,
                        items = orders.flatMap { it.items } // 아이템 리스트 병합
                    )
                    orderList.add(combinedOrder)
                }

                binding.textViewTotalAmount.text = "${String.format("%,d", totalAmount)} 원"

                accountAdapter.notifyDataSetChanged()
                Log.d("AccountFragment", "Orders successfully loaded and grouped by tableId.")
            }
            .addOnFailureListener { e ->
                Log.e("FirestoreError", "Failed to fetch orders: ", e)
                Toast.makeText(requireContext(), "주문 데이터를 불러오지 못했습니다.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun showOrderDetails(tableId: String) {
        val filteredOrders = orderList.filter { it.tableId == tableId }

        if (filteredOrders.isEmpty()) {
            Toast.makeText(requireContext(), "해당 테이블에 대한 주문이 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val menuMap = mutableMapOf<String, MenuItem>()
        for (order in filteredOrders) {
            for (menuItem in order.items) {
                val existingItem = menuMap[menuItem.menuName]
                if (existingItem != null) {
                    // 이미 있는 메뉴의 수량 및 총 금액 업데이트
                    menuMap[menuItem.menuName] = existingItem.copy(
                        quantity = existingItem.quantity + menuItem.quantity,
                        totalPrice = existingItem.totalPrice + menuItem.totalPrice
                    )
                } else {
                    menuMap[menuItem.menuName] = menuItem
                }
            }
        }

        // 통합된 메뉴 아이템 리스트
        val consolidatedMenuItems = menuMap.values.toList()

        // 상세 정보를 다이얼로그로 표시
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_order_details, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recyclerViewMenu)
        val textViewOrderDetailsTitle = dialogView.findViewById<TextView>(R.id.textViewOrderDetailsTitle)

        recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = MenuAdapter(consolidatedMenuItems)
        }

        textViewOrderDetailsTitle.text = "테이블 $tableId 주문 상세"

        AlertDialog.Builder(requireContext())
            .setView(dialogView)
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}