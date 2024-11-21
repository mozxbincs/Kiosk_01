package com.example.kiosk02.admin.orderStatus

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.core.text.isDigitsOnly
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kiosk02.R
import com.example.kiosk02.admin.orderStatus.data.Order
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale
import com.google.firebase.database.FirebaseDatabase

class OrderStatusAdapter(private val orders: MutableList<Order>) :
    RecyclerView.Adapter<OrderStatusAdapter.OrderViewHolder>() {

    private val completedOrders = mutableSetOf<String>()

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderDateTextView: TextView = itemView.findViewById(R.id.orderDateTextView)
        val orderTimeTextView: TextView = itemView.findViewById(R.id.orderTimeTextView)
        val tableIdTextView: TextView = itemView.findViewById(R.id.tableIdTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        val cancelButton: TextView = itemView.findViewById(R.id.cancelButton)
        val menuStatusRecyclerView: RecyclerView =
            itemView.findViewById(R.id.menuStatusRecyclerView)
        val checkBox: CheckBox = itemView.findViewById(R.id.completeCheckBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_order_status, parent, false)
        return OrderViewHolder(view)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orders[position]

        // 주문 시간 포맷팅
        val (formattedDate, formattedTime) = formatOrderTime(order.orderTime)

        // 가격 포맷팅
        val formattedPrice = NumberFormat.getNumberInstance(Locale.KOREA).format(order.totalAmount)

        // 테이블 번호 포맷팅
        val formattedTableId = formatTableId(order.tableId)


        holder.orderDateTextView.text = formattedDate
        holder.orderTimeTextView.text = formattedTime

        // orderType이 "pickup"인 경우 "픽업" 텍스트 표시
        holder.tableIdTextView.text =
            if (order.orderType.lowercase(Locale.getDefault()) == "pickup") {
                "픽업"
            } else {
                formattedTableId
            }
        // 총 가격 표시
        holder.priceTextView.text = "${formattedPrice}원"

        // 메뉴 리스트 RecyclerView 설정
        holder.menuStatusRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.menuStatusRecyclerView.adapter = MenuItemsAdapter(order.items)

        // 체크박스 상태 설정
        holder.checkBox.setOnCheckedChangeListener(null) // 기존 리스너 제거
        holder.checkBox.isChecked = completedOrders.contains(order.orderId)

        // 체크박스 리스너 설정
        holder.checkBox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                completedOrders.add(order.orderId)
            } else {
                completedOrders.remove(order.orderId)
            }
            reorderList()
        }

        // 체크된 항목 배경색 변경
        if (completedOrders.contains(order.orderId)) {
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.gray01))
            holder.cancelButton.setBackgroundColor(holder.cancelButton.context.getColor(R.color.gray01))
        } else {
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.defaultBackground))
            holder.cancelButton.setBackgroundColor(holder.cancelButton.context.getColor(R.color.defaultBackground))
        }

        holder.cancelButton.setOnClickListener {
            showCancelDialog(holder.itemView.context, order.orderTime, position)
        }
    }

    override fun getItemCount(): Int = orders.size

    // 주문 시간 포맷팅 함수
    private fun formatOrderTime(orderTime: String): Pair<String, String> {
        return try {
            val originalFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
            val date = originalFormat.parse(orderTime)

            val dateFormat = SimpleDateFormat("yyyy.MM.dd", Locale.getDefault())
            val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

            val formattedDate = dateFormat.format(date!!)
            val formattedTime = timeFormat.format(date)

            Pair(formattedDate, formattedTime)
        } catch (e: Exception) {
            Pair(orderTime, orderTime)
        }
    }

    // 테이블 번호 포맷팅 함수
    private fun formatTableId(tableId: String): String {
        return tableId.replace("table_", "").let { id ->
            if (id.isDigitsOnly()) "${id}번 테이블" else tableId
        }
    }

    // 리스트를 재정렬하여 체크된 항목을 아래로 이동
    private fun reorderList() {
        orders.sortWith(
            compareBy<Order> { completedOrders.contains(it.orderId) }
                .thenByDescending { it.orderTime }
        )
        notifyDataSetChanged()
    }

    private fun showCancelDialog(context: Context, orderTime: String, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("주문 취소")
            .setMessage("주문을 취소하겠습니까?")
            .setPositiveButton("예") { _, _ ->
                deleteOrderFromRealtimeDatabase(orderTime, position)
            }
            .setNegativeButton("아니오", null)
            .show()
    }

    private fun deleteOrderFromRealtimeDatabase(orderTime: String, position: Int) {
        val database = FirebaseDatabase.getInstance()
        val adminOrdersRef = database.getReference("admin_orders")

        adminOrdersRef.get()
            .addOnSuccessListener { snapshot ->
                var adminEmail: String? = null

                // admin_orders 하위 경로를 탐색하여 adminEmail을 찾음
                for (childSnapshot in snapshot.children) {
                    if (childSnapshot.hasChild(orderTime)) {
                        adminEmail = childSnapshot.key?.replace("_", ".") // Realtime DB는 '.'를 '_'로 저장
                        break
                    }
                }
                if (adminEmail != null) {
                    // adminEmail과 orderTime으로 삭제 경로 구성
                    val orderRef = adminOrdersRef.child(adminEmail.replace(".", "_")).child(orderTime)
                    orderRef.removeValue()
                        .addOnSuccessListener {
                            orders.removeAt(position)
                            notifyItemRemoved(position)
                            notifyItemRangeChanged(position, orders.size)
                        }
                        .addOnFailureListener { exception ->
                            Log.e("OrderStatusAdapter", "주문 삭제 실패: ${exception.message}")
                        }
                } else {
                    Log.e("OrderStatusAdapter", "adminEmail을 찾을 수 없습니다.")
                }
            }
            .addOnFailureListener { exception ->
                Log.e("OrderStatusAdapter", "데이터 가져오기 실패: ${exception.message}")
            }
    }
}