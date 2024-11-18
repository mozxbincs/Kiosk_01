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

        val adminEmail = currentUser.email
        if (adminEmail.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "관리자 이메일을 가져올 수 없습니다.", Toast.LENGTH_SHORT).show()
            return
        }

        val safeAdminEmail = adminEmail.replace(".", "_") // Realtime Database에서는 '.' 사용 불가

        // 주문 데이터 경로 설정
        ordersReference = database.getReference("admin_orders/$safeAdminEmail")

        // RecyclerView 설정
        ordersAdapter = OrderStatusAdapter(ordersList)
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
        // 실시간 데이터 리스너 설정
        ordersReference.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                ordersList.clear()
                for (orderSnapshot in snapshot.children) {
                    val order = orderSnapshot.getValue(Order::class.java)
                    order?.orderId = orderSnapshot.key ?: ""
                    if (order != null) {
                        ordersList.add(order)
                    }
                }
                // 날짜 및 시간 기준으로 정렬 (최신 주문이 상단)
                ordersList.sortWith(compareByDescending { it.orderTime })
                ordersAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // 오류 처리 (예: Toast, Snackbar 등)
                Toast.makeText(requireContext(), "데이터 로드 실패: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }
}