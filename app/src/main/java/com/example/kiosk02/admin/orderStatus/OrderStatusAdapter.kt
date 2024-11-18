package com.example.kiosk02.admin.orderStatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kiosk02.R
import com.example.kiosk02.admin.orderStatus.data.Order
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

class OrderStatusAdapter(private val orders: MutableList<Order>) :
    RecyclerView.Adapter<OrderStatusAdapter.OrderViewHolder>() {

    private val completedOrders = mutableSetOf<String>()

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderDateTextView: TextView = itemView.findViewById(R.id.orderDateTextView)
        val orderTimeTextView: TextView = itemView.findViewById(R.id.orderTimeTextView)
        val tableIdTextView: TextView = itemView.findViewById(R.id.tableIdTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        val paymentTextView: TextView = itemView.findViewById(R.id.paymentTextView)
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

        holder.orderDateTextView.text = formattedDate
        holder.orderTimeTextView.text = formattedTime

        // orderType이 "pickup"인 경우 "픽업" 텍스트 표시
        holder.tableIdTextView.text =
            if (order.orderType.lowercase(Locale.getDefault()) == "pickup") {
                "픽업"
            } else {
                order.tableId
            }
        // 총 가격 표시
        holder.priceTextView.text = "${formattedPrice}원"

        // 결제 여부 표시
        holder.paymentTextView.text = "x" //주문 경로 설정 필요

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
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.gray01)) // 원하는 색상으로 변경
        } else {
            holder.itemView.setBackgroundColor(holder.itemView.context.getColor(R.color.defaultBackground))
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

    // 리스트를 재정렬하여 체크된 항목을 아래로 이동
    private fun reorderList() {
        orders.sortWith(
            compareBy<Order> { completedOrders.contains(it.orderId) }
                .thenByDescending { it.orderTime }
        )
        notifyDataSetChanged()
    }
}