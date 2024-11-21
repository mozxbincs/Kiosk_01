package com.example.kiosk02

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class accountAdapter(
    private val orders: List<Order>,
    private val onItemClick: (String) -> Unit
) : RecyclerView.Adapter<accountAdapter.accountViewHolder>() {

    class accountViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textName: TextView = itemView.findViewById(R.id.textName)
        val textTime: TextView = itemView.findViewById(R.id.textTime)
        val textTotal: TextView = itemView.findViewById(R.id.textTotal)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): accountViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
        return accountViewHolder(view)
    }

    override fun getItemCount() = orders.size

    override fun onBindViewHolder(holder: accountViewHolder, position: Int) {
        val order = orders[position]
        holder.textName.text = order.consumerEmail
        holder.textTime.text = formatOrderTime(order.orderTime)
        holder.textTotal.text = "${formatPrice(order.totalAmount)} 원"

        holder.itemView.setOnClickListener { onItemClick(order.tableId) }
    }

    private fun formatOrderTime(orderTime: String): String {
        return orderTime
    }

    private fun formatPrice(price: Int): String {
        return String.format("%,d", price)
    }


}