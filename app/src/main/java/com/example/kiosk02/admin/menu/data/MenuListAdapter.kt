package com.example.kiosk02.admin.menu.data

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kiosk02.databinding.ItemMenuBinding
import java.text.NumberFormat
import java.util.Locale

class MenuListAdapter : ListAdapter<MenuModel, MenuListAdapter.MenuViewHolder>(MenuDiffCallback()) {

    inner class MenuViewHolder(private val binding: ItemMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(menuModel: MenuModel) {
            // 메뉴명 설정
            binding.menuNameTextView.text = menuModel.menuName ?: "이름 없음"
            // 메뉴 가격 설정
            val formattedPrice = NumberFormat.getNumberInstance(Locale.KOREA).format(menuModel.price ?: 0)
            binding.priceTextView.text = "${formattedPrice}원"
            // 메뉴 이미지 설정
            Glide.with(binding.menuImageView.context)
                .load(menuModel.imageUrl)
                .into(binding.menuImageView)
        }
    }

    //item_menu 레이아웃 inflate
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding = ItemMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    // 각 메뉴 데이터를 View에 바인딩
    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class MenuDiffCallback : DiffUtil.ItemCallback<MenuModel>() {
    override fun areItemsTheSame(oldItem: MenuModel, newItem: MenuModel): Boolean {
        return oldItem.menuId == newItem.menuId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: MenuModel, newItem: MenuModel): Boolean {
        return oldItem == newItem
    }
}