package com.example.kiosk02.admin.orderStatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.CheckBox
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.kiosk02.R
import com.example.kiosk02.admin.orderStatus.data.Order
import java.text.SimpleDateFormat
import java.util.Locale

class OrderStatusAdapter(private val orders: List<Order>) :
    RecyclerView.Adapter<OrderStatusAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val orderDateTextView: TextView = itemView.findViewById(R.id.orderDateTextView)
        val orderTimeTextView: TextView = itemView.findViewById(R.id.orderTimeTextView)
        val tableIdTextView: TextView = itemView.findViewById(R.id.tableIdTextView)
        val priceTextView: TextView = itemView.findViewById(R.id.priceTextView)
        val paymentTextView: TextView = itemView.findViewById(R.id.paymentTextView)
        val menuStatusRecyclerView: RecyclerView = itemView.findViewById(R.id.menuStatusRecyclerView)
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

        holder.orderDateTextView.text = formattedDate
        holder.orderTimeTextView.text = formattedTime
        holder.tableIdTextView.text = order.tableId

        holder.priceTextView.text = "${order.totalAmount}원"
        holder.paymentTextView.text = order.orderType

        // 메뉴 리스트 RecyclerView 설정
        holder.menuStatusRecyclerView.layoutManager = LinearLayoutManager(holder.itemView.context)
        holder.menuStatusRecyclerView.adapter = MenuItemsAdapter(order.items)


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
}