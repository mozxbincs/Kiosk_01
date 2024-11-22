package com.example.kiosk02.admin.orderStatus

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kiosk02.R
import com.example.kiosk02.admin.orderStatus.data.MenuItem

class MenuItemsAdapter(private val menuItems: List<MenuItem>) :
    RecyclerView.Adapter<MenuItemsAdapter.MenuItemViewHolder>() {

    inner class MenuItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val menuNameTextView: TextView = itemView.findViewById(R.id.menuNameTextView)
        val menuNumberTextView: TextView = itemView.findViewById(R.id.menuNumberTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_menu_status, parent, false)
        return MenuItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: MenuItemViewHolder, position: Int) {
        val menuItem = menuItems[position]
        holder.menuNameTextView.text = menuItem.menuName
        holder.menuNumberTextView.text = "${menuItem.quantity}개"
    }

    override fun getItemCount(): Int = menuItems.size
}