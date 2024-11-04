package com.example.kiosk02.customer.menu

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
            val totalPrice = (item.price ?: 0) * (item.quantity ?: 1)

            binding.totalPriceTextView.text = "${totalPrice}원"

            Glide.with(binding.menuImageView.context)
                .load(item.imageUrl)
                .into(binding.menuImageView)

            val quantityList = (1..10).toList()
            val adapter = ArrayAdapter(
                binding.numberSpinner.context,
                R.layout.simple_spinner_item,
                quantityList
            )
            adapter.setDropDownViewResource(R.layout.simple_spinner_dropdown_item)
            binding.numberSpinner.adapter = adapter
            binding.numberSpinner.setSelection(item.quantity - 1)

            binding.numberSpinner.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        // item.quantity 업데이트
                        item.quantity = quantityList[position]
                        val totalPrice = (item.price ?: 0) * (item.quantity ?: 1)
                        binding.totalPriceTextView.text = "${totalPrice}원"
                        onQuantityChanged()
                    }

                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

            binding.deleteButton.setOnClickListener {
                onItemDeleted(item)
            }
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