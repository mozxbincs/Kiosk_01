package com.example.kiosk02.admin.Menu.data

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.kiosk02.databinding.ItemCategoryBinding

class CategoriesAdapter(
    private val onItemClick: (String) -> Unit // 클릭 이벤트 핸들러
) : ListAdapter<String, CategoriesAdapter.CategoryViewHolder>(DiffCallback()) {

    // ViewHolder 정의 (ViewBinding 사용)
    inner class CategoryViewHolder(val binding: ItemCategoryBinding) : RecyclerView.ViewHolder(binding.root) {

        // 카테고리 데이터를 바인딩하는 메서드
        fun bind(categoryName: String) {
            binding.categoryNameTextView.text = categoryName
            binding.root.setOnClickListener {
                onItemClick(categoryName)
            }
        }
    }

    // 새로운 ViewHolder 생성
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val binding = ItemCategoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CategoryViewHolder(binding)
    }

    // ViewHolder에 데이터를 바인딩
    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = getItem(position)
        holder.bind(category)
    }

    // DiffUtil 클래스 정의
    class DiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    }
}