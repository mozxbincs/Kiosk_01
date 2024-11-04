package com.example.kiosk02.customer.menu

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.kiosk02.R
import com.example.kiosk02.admin.menu.data.MenuModel
import com.example.kiosk02.databinding.FragmentConsumerCartBinding
import com.google.common.reflect.TypeToken
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson

class ConsumerCartFragment: Fragment() {
    private lateinit var binding: FragmentConsumerCartBinding
    private lateinit var cartAdapter: ConsumerCartAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val cartItems = mutableListOf<MenuModel>()
    private val gson = Gson()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConsumerCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadCartData()
        setupRecyclerView()

        binding.backButton.setOnClickListener {
            goToConsumerMenuList()
        }

        binding.toOrderButton.setOnClickListener {
            placeOrder()
        }
    }



    private fun loadCartData() {
        val sharedPreferences = requireContext().getSharedPreferences("cart", Context.MODE_PRIVATE)
        val cartJson = sharedPreferences.getString("cartItems", null)
        if (cartJson != null) {
            val type = object : TypeToken<List<MenuModel>>() {}.type
            val items: List<MenuModel> = gson.fromJson(cartJson, type)
            cartItems.addAll(items)
            updateTotalPrice()
        }
    }

    private fun setupRecyclerView() {
        cartAdapter = ConsumerCartAdapter(cartItems, ::onQuantityChanged, ::onItemDeleted)
        binding.recyclerview.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = cartAdapter
        }
    }

    private fun onQuantityChanged() {
        updateTotalPrice()
    }

    private fun onItemDeleted(menuModel: MenuModel) {
        cartItems.remove(menuModel)
        saveCartData()
        cartAdapter.notifyDataSetChanged()
        updateTotalPrice()
    }

    private fun updateTotalPrice() {
        val total = cartItems.sumOf { (it.price ?: 0) * (it.quantity ?: 1) }
        binding.totalItemPriceTextView.text = "${total}원"
    }

    private fun saveCartData() {
        val sharedPreferences = requireContext().getSharedPreferences("cart", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val cartJson = gson.toJson(cartItems)
        editor.putString("cartItems", cartJson)
        editor.apply()
    }

    private fun placeOrder() {
        val email = "cherrychoi35@gmail.com"
        val consumerEmail = "ccc@ccc.ccc"
        val tableId = "table1"
        val orderData = cartItems.map {
            mapOf(
                "menuName" to it.menuName,
                "quantity" to (it.quantity ?: 1),
                "totalPrice" to (it.price ?: 0) * (it.quantity ?: 1)
            )
        }
        val totalAmount = cartItems.sumOf { (it.price ?: 0) * (it.quantity ?: 1) }

        // 관리자에 데이터 추가
        val adminOrderRef = firestore.collection("admin").document(email)
            .collection("order").document(tableId)

        // 소비자에 데이터 추가
        val consumerOrderRef = firestore.collection("consumer").document(consumerEmail)
            .collection("email").document(tableId)

        // 데이터 맵핑
        val orderMap = mapOf(
            "items" to orderData,
            "totalAmount" to totalAmount
        )

        // 관리자 데이터 저장
        adminOrderRef.set(orderMap)
            .addOnSuccessListener {
                //성공 시, 소비자에 저장
                consumerOrderRef.set(orderMap)
                    .addOnSuccessListener {
                        Toast.makeText(requireContext(), "주문이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                        goToConsumerMenuList()
                        clearCart()
                    }
                    .addOnFailureListener {
                        Toast.makeText(requireContext(), "소비자에 주문 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "관리자에 주문 저장에 실패했습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearCart() {
        cartItems.clear()
        saveCartData()
        cartAdapter.notifyDataSetChanged()
        updateTotalPrice()
    }

    private fun goToConsumerMenuList() {
        findNavController().navigate(R.id.action_to_ConsumerMenuList)
    }
}