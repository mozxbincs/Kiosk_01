package com.example.kiosk02.admin.Menu.data

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class MenuPagerAdapter(fragment: Fragment) : FragmentStateAdapter(fragment) {

    private val categories = mutableListOf<String>()

    override fun getItemCount(): Int = categories.size

    fun setCategories(categoryList: List<String>) {
        categories.clear()
        categories.addAll(categoryList)
        notifyDataSetChanged() // 카테고리가 업데이트되면 알림
    }

    override fun createFragment(position: Int): Fragment {
        val category = categories[position]
        return MenuListFragment.newInstance(category) // 카테고리별로 Fragment 생성
    }

    fun getCategoryTitle(position: Int): String = categories[position]
}