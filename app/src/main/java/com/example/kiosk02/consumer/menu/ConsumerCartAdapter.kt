package com.example.kiosk02.consumer.menu

import android.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kiosk02.admin.menu.data.MenuModel
import com.example.kiosk02.databinding.ItemCartMenuBinding

class ConsumerCartAdapter(
    private val items: List<MenuModel>,
    private val onQuantityChanged: () -> Unit,
    private val onItemDeleted: (MenuModel) -> Unit
) : RecyclerView.Adapter<ConsumerCartAdapter.CartViewHolder>() {

    inner class CartViewHolder(private val binding: ItemCartMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: MenuModel) {
            binding.menuNameTextView.text = item.menuName
            binding.priceTextView.text = "${item.price}원"
            updateTotalPrice(item)

            Glide.with(binding.menuImageView.context)
                .load(item.imageUrl)
                .into(binding.menuImageView)

            // 수량 표시
            binding.menuNumberTextView.text = "${item.quantity}개"

            // - 버튼 클릭 시
            binding.removeButton.setOnClickListener {
                if (item.quantity > 1) {
                    item.quantity--
                    binding.menuNumberTextView.text = "${item.quantity}개"
                    updateTotalPrice(item)
                    onQuantityChanged()
                }
            }

            // + 버튼 클릭 시
            binding.addButton.setOnClickListener {
                item.quantity++
                binding.menuNumberTextView.text = "${item.quantity}개"
                updateTotalPrice(item)
                onQuantityChanged()
            }

            // 삭제 버튼 클릭 시
            binding.deleteButton.setOnClickListener {
                onItemDeleted(item)
            }
        }

        private fun updateTotalPrice(item: MenuModel) {
            val totalPrice = (item.price ?: 0) * (item.quantity ?: 1)
            binding.totalPriceTextView.text = "${totalPrice}원"
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val binding =
            ItemCartMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CartViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size
}