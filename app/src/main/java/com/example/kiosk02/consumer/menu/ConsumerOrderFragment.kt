package com.example.kiosk02.consumer.menu

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.kiosk02.R
import com.example.kiosk02.admin.menu.data.MenuModel
import com.example.kiosk02.databinding.FragmentConsumerOrderFragmentBinding
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.text.NumberFormat
import java.util.Locale

class ConsumerOrderFragment : Fragment(R.layout.fragment_consumer_order_fragment) {
    private lateinit var binding: FragmentConsumerOrderFragmentBinding
    private var menuId: String? = null
    private var menuModel: MenuModel? = null
    private var bundle: Bundle? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        menuId = arguments?.getString("menuId")
        bundle = arguments
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding = FragmentConsumerOrderFragmentBinding.bind(view)

        menuModel = arguments?.getParcelable("menuModel")



        loadMenuData()

        setupSpinner()

        binding.cartImageButton.setOnClickListener {
            val newBundle = Bundle(bundle).apply {
                remove("menuModel") // 전달받은 Bundle값 중 menuModel 삭제
            }
            findNavController().navigate(R.id.action_to_ConsumerCartFragment, newBundle)
        }

        binding.toCartButton.setOnClickListener {
            setupCartButton()
        }

        binding.backButton.setOnClickListener {
            goToConsumerMenuList()
        }
    }


    private fun loadMenuData() {
        menuModel?.let { menu ->
            Glide.with(binding.menuImageView.context)
                .load(menu.imageUrl)
                .into(binding.menuImageView)

            binding.menuNameTextView.setText(menu.menuName)

            val formattedPrice =
                NumberFormat.getNumberInstance(Locale.KOREA).format(menu.price ?: 0)
            binding.priceTextView.setText("${formattedPrice}원")

            binding.compositionTextView.setText(menu.composition)
            binding.menuDetailTextView.setText(menu.detail)
        }

    }

    private fun setupSpinner() {
        val quantityList = (1..10).toList()
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            quantityList
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.numberSpinner.adapter = adapter
    }

    private fun setupCartButton() {
        binding.toCartButton.setOnClickListener {
            val selectedQuantity = binding.numberSpinner.selectedItem as Int
            addToCart(menuId, selectedQuantity)
        }
    }

    private fun addToCart(menuId: String?, quantity: Int) {
        menuModel?.let { menu ->
            // 선택한 수량을 menuModel에 업데이트
            menu.quantity = quantity

            // SharedPreferences에 장바구니 데이터 저장
            val sharedPreferences =
                requireContext().getSharedPreferences("cart", Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // 현재 저장된 장바구니 데이터를 가져와서 기존 항목 유지 및 업데이트
            val gson = Gson()
            val cartJson = sharedPreferences.getString("cartItems", null)
            val type = object : TypeToken<MutableList<MenuModel>>() {}.type
            val cartItems: MutableList<MenuModel> = gson.fromJson(cartJson, type) ?: mutableListOf()

            // 장바구니에 동일한 메뉴가 이미 있는지 확인 후 업데이트, 아니면 추가
            val existingItemIndex = cartItems.indexOfFirst { it.menuId == menu.menuId }
            if (existingItemIndex != -1) {
                cartItems[existingItemIndex].quantity += quantity
            } else {
                cartItems.add(menu)
            }

            // 수정된 장바구니 데이터를 JSON으로 저장
            editor.putString("cartItems", gson.toJson(cartItems))
            editor.apply()

            Toast.makeText(requireContext(), "장바구니에 담겼습니다.", Toast.LENGTH_SHORT).show()
            goToConsumerMenuList()
        }
    }

    private fun goToConsumerMenuList() {
        findNavController().navigate(R.id.action_to_ConsumerMenuList)
    }

}