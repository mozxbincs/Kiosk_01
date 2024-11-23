package com.example.kiosk02.admin.orderStatus

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kiosk02.R
import com.example.kiosk02.admin.orderStatus.data.Order
import com.example.kiosk02.databinding.FragmentAdminOrderStatusBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class AdminOrderStatusFragment : Fragment(R.layout.fragment_admin_order_status) {
    private var _binding: FragmentAdminOrderStatusBinding? = null
    private val binding get() = _binding!!

    private lateinit var ordersAdapter: OrderStatusAdapter
    private val ordersList = mutableListOf<Order>()

    private lateinit var database: FirebaseDatabase
    private lateinit var ordersReference: DatabaseReference
    private lateinit var auth: FirebaseAuth

    private lateinit var adminEmail: String
    private lateinit var safeAdminEmail: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding = FragmentAdminOrderStatusBinding.bind(view)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val currentUser = auth.currentUser
        if (currentUser == null) {
            Toast.makeText(requireContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show()
            return
        }

        adminEmail = currentUser.email ?: ""
        if (adminEmail.isEmpty()) {
            Toast.makeText(requireContext(), "관리자 이메일을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        safeAdminEmail = adminEmail.replace(".", "_") // Realtime Database에서 '.' 사용 불가

        // 주문 데이터 경로 설정
        ordersReference = database.getReference("admin_orders/$safeAdminEmail")

        // RecyclerView 설정
        ordersAdapter = OrderStatusAdapter(ordersList, requireContext())
        binding.orderStatusRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.orderStatusRecyclerView.adapter = ordersAdapter
        binding.orderStatusRecyclerView.setHasFixedSize(true)

        // 데이터 로드
        loadOrderStatus()

        // 백 버튼 클릭 리스너 설정
        binding.backButton.setOnClickListener {
            findNavController().navigate(R.id.adminActivity)
        }
    }

    override fun onResume() {
        super.onResume()
        // 화면 방향을 가로로 고정
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
    }

    override fun onPause() {
        super.onPause()
        // 화면 방향 고정 해제
        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun loadOrderStatus() {
        // 테이블 ID를 순회하며 데이터를 실시간으로 가져옴
        ordersReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ordersList.clear() // 기존 주문 리스트 초기화

                for (tableSnapshot in snapshot.children) {
                    val tableId = tableSnapshot.key ?: continue
                    for (orderSnapshot in tableSnapshot.children) {
                        val order = orderSnapshot.getValue(Order::class.java)
                        order?.let {
                            it.orderId = orderSnapshot.key ?: ""
                            it.tableId = tableId
                            it.adminEmail = adminEmail // 설정된 adminEmail 사용
                            ordersList.add(it)
                        }
                    }
                }

                // 정렬 및 UI 갱신
                ordersAdapter.reorderList()
            }

            override fun onCancelled(error: DatabaseError) {
                // 오류 처리
                Toast.makeText(requireContext(), "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}