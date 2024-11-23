package com.example.kiosk02.consumer.menu

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.kiosk02.admin.menu.data.MenuModel
import com.example.kiosk02.databinding.ItemConsumerMenuBinding
import java.text.NumberFormat
import java.util.Locale

class ConsumerMenuListAdapter(
    private val onItemClick: (MenuModel) -> Unit // 메뉴 ID를 전달하는 클릭 이벤트 핸들러
) : ListAdapter<MenuModel, ConsumerMenuListAdapter.MenuViewHolder>(MenuDiffCallback1()) {

    inner class MenuViewHolder(private val binding: ItemConsumerMenuBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(menuModel: MenuModel) {
            //메뉴명 설정
            binding.menuNameTextView.text = menuModel.menuName
            //메뉴 가격 설정
            val formattedPrice =
                NumberFormat.getNumberInstance(Locale.KOREA).format(menuModel.price ?: 0)
            binding.priceTextView.text = "${formattedPrice}원"
            // 메뉴 구성 설명
            binding.compositionTextView.text = menuModel.composition
            // 메뉴 이미지 설정
            Glide.with(binding.menuImageView.context).load(menuModel.imageUrl)
                .into(binding.menuImageView)

            binding.root.setOnClickListener {
                onItemClick(menuModel)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MenuViewHolder {
        val binding =
            ItemConsumerMenuBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MenuViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MenuViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}

class MenuDiffCallback1 : DiffUtil.ItemCallback<MenuModel>() {
    override fun areItemsTheSame(oldItem: MenuModel, newItem: MenuModel): Boolean {
        return oldItem.menuId == newItem.menuId
    }

    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: MenuModel, newItem: MenuModel): Boolean {
        return oldItem == newItem
    }
}