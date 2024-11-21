package com.example.kiosk02

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.kiosk02.databinding.ItemMenuBinding
import com.example.kiosk02.databinding.MenuItemBinding

class MenuAdapter(
    private val menuItems: List<MenuItem>
): RecyclerView.Adapter<MenuAdapter.MenuViewHolder>() {


    inner class MenuViewHolder(private val binding: MenuItemBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: MenuItem) {
            binding.textMenuName.text = item.menuName
            binding.textQuantity.text = "수량: ${item.quantity}"
            binding.textPrice.text = "${formatPrice(item.totalPrice)} 원"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = MenuItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun getItemCount() = menuItems.size

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(menuItems[position])
    }

    private fun formatPrice(price: Int): String {
        return String.format("%,d", price)
    }
}