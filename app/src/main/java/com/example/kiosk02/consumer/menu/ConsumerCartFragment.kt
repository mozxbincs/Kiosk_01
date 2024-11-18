package com.example.kiosk02.consumer.menu

import android.content.Context
import android.os.Bundle
import android.util.Log
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
import com.google.android.gms.tasks.Task
import com.google.common.reflect.TypeToken
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ConsumerCartFragment : Fragment() {
    private lateinit var binding: FragmentConsumerCartBinding
    private lateinit var cartAdapter: ConsumerCartAdapter
    private val firestore = FirebaseFirestore.getInstance()
    private val database = FirebaseDatabase.getInstance()
    private val cartItems = mutableListOf<MenuModel>()
    private val gson = Gson()

    private var isOrderPlaced = false // 주문 여부를 나타내는 플래그 추가
    private var Aemail: String? = null
    private var Uemail: String? = null
    private var selectedTableId: String? = null
    private var selectedFloor: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentConsumerCartBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Aemail = arguments?.getString("Aemail")
        Uemail = arguments?.getString("Uemail")
        selectedTableId = arguments?.getString("selectedTableId")
        selectedFloor = arguments?.getString("selectedFloor")

        loadCartData()
        setupRecyclerView()

        binding.backButton.setOnClickListener {
            goToConsumerMenuList()
        }

        binding.toOrderButton.setOnClickListener {
            placeOrder()
        }
    }

    override fun onResume() {
        super.onResume()
        // Fragment로 돌아올 때 select 생성
        if (!Aemail.isNullOrEmpty() && !selectedTableId.isNullOrEmpty() && !selectedFloor.isNullOrEmpty()) {
            createSelectCollection(Aemail!!, selectedFloor!!, selectedTableId!!)
                .addOnSuccessListener {
                    Log.d("ConsumerCartFragment", "select 재생성 완료")
                }
                .addOnFailureListener { e ->
                    Log.e("ConsumerCartFragment", "select 재생성 실패", e)
                }
        }
    }

    override fun onPause() {
        super.onPause()
        // 화면을 벗어날 때 select 삭제 (단, 주문하기 버튼으로 이동한 경우 제외)
        if (!isOrderPlaced &&
            !Aemail.isNullOrEmpty() &&
            !selectedTableId.isNullOrEmpty() &&
            !selectedFloor.isNullOrEmpty()
        ) {
            deleteSelectCollection(Aemail!!, selectedFloor!!, selectedTableId!!)
                .addOnSuccessListener {
                    Log.d("ConsumerCartFragment", "select 삭제 완료")
                }
                .addOnFailureListener { e ->
                    Log.e("ConsumerCartFragment", "select 삭제 실패", e)
                }
        }
    }

    private fun createSelectCollection(Aemail: String, floor: String, tableId: String): Task<Void> {
        val selectDocRef = firestore.collection("admin")
            .document(Aemail)
            .collection("floors")
            .document(floor)
            .collection("tables")
            .document(tableId)
            .collection("select")
            .document(Uemail!!)

        return selectDocRef.set(mapOf("select" to true))
    }

    private fun deleteSelectCollection(Aemail: String, floor: String, tableId: String): Task<Void> {
        val selectDocRef = firestore.collection("admin")
            .document(Aemail)
            .collection("floors")
            .document(floor)
            .collection("tables")
            .document(tableId)
            .collection("select")
            .document(Uemail!!)

        return selectDocRef.delete()
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
        if (cartItems.isEmpty()) {
            Toast.makeText(requireContext(), "장바구니가 비어 있습니다.", Toast.LENGTH_SHORT).show()
            return
        }
        val adminEmail = arguments?.getString("Aemail") ?: ""
        val consumerEmail = arguments?.getString("Uemail") ?: ""
        val tableId = arguments?.getString("selectedTableId") ?: ""
        val orderType = arguments?.getString("orderType") ?: "unknown"
        val timestamp = System.currentTimeMillis()
        val formattedTime =
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.KOREA).format(Date(timestamp))

        val orderData = cartItems.map {
            mapOf<String, Any>(
                "menuName" to (it.menuName ?: ""),
                "quantity" to (it.quantity ?: 1),
                "totalPrice" to (it.price ?: 0) * (it.quantity ?: 1)
            )
        }
        val totalAmount = cartItems.sumOf { (it.price ?: 0) * (it.quantity ?: 1) }

        // Firestore에 소비자 주문 데이터 저장
        saveOrderToFirestore(
            consumerEmail,
            adminEmail,
            tableId,
            orderData,
            totalAmount,
            orderType,
            formattedTime
        )

        // Realtime Database에 관리자 주문 데이터 저장
        saveOrderToRealtimeDatabase(
            adminEmail,
            consumerEmail,
            tableId,
            orderData,
            totalAmount,
            orderType,
            formattedTime
        )
    }

    private fun saveOrderToFirestore(
        consumerEmail: String,
        adminEmail: String,
        tableId: String,
        orderData: List<Map<String, Any>>,
        totalAmount: Int,
        orderType: String,
        formattedTime: String
    ) {
        val consumerOrderRef = if (orderType == "pickup") {
            firestore.collection("consumer").document(consumerEmail)
                .collection(adminEmail).document("pickupOrders")
        } else {
            firestore.collection("consumer").document(consumerEmail)
                .collection(tableId).document("tableOrders")
        }

        consumerOrderRef.get().addOnSuccessListener { document ->
            var finalTotalAmount = totalAmount
            val updatedItems = mutableListOf<Map<String, Any>>()

            // 기존 데이터가 있을 경우 totalAmount를 누적
            val existingOrderData = document.data
            if (existingOrderData != null) {
                val existingTotalAmount = (existingOrderData["totalAmount"] as? Long)?.toInt() ?: 0
                finalTotalAmount += existingTotalAmount

                // 기존 items 가져오기
                val existingItems = existingOrderData["items"] as? List<Map<String, Any>> ?: emptyList()
                updatedItems.addAll(existingItems)
            }

            // 기존 items에 새로운 orderData 병합
            orderData.forEach { newItem ->
                val existingItemIndex = updatedItems.indexOfFirst { it["menuName"] == newItem["menuName"] }
                if (existingItemIndex != -1) {
                    // 이미 있는 항목의 수량과 총 가격을 증가
                    val existingItem = updatedItems[existingItemIndex]
                    val existingQuantity = (existingItem["quantity"] as? Long)?.toInt() ?: 0
                    val newQuantity = (newItem["quantity"] as? Int) ?: 1
                    val updatedQuantity = existingQuantity + newQuantity

                    val existingTotalPrice = (existingItem["totalPrice"] as? Long)?.toInt() ?: 0
                    val newTotalPrice = (newItem["totalPrice"] as? Int) ?: 0
                    val updatedTotalPrice = existingTotalPrice + newTotalPrice

                    updatedItems[existingItemIndex] = existingItem.toMutableMap().apply {
                        this["quantity"] = updatedQuantity
                        this["totalPrice"] = updatedTotalPrice
                    }
                } else {
                    // 새로운 항목 추가
                    updatedItems.add(newItem)
                }
            }

            // 업데이트할 주문 맵 구성
            val updatedOrderMap = mapOf(
                "items" to updatedItems,
                "totalAmount" to finalTotalAmount,
                "orderType" to orderType,
                "orderTime" to formattedTime
            )

            // Firestore에 주문 데이터 저장
            consumerOrderRef.set(updatedOrderMap)
                .addOnSuccessListener {
                    Toast.makeText(requireContext(), "주문이 완료되었습니다.", Toast.LENGTH_SHORT).show()
                    goToConsumerMenuList()
                    clearCart()
                }
                .addOnFailureListener {
                    Toast.makeText(requireContext(), "주문에 실패하였습니다.", Toast.LENGTH_SHORT).show()
                }
        }.addOnFailureListener {
            Toast.makeText(requireContext(), "주문을 확인하는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveOrderToRealtimeDatabase(
        adminEmail: String,
        consumerEmail: String,
        tableId:String,
        orderData: List<Map<String, Any>>,
        totalAmount: Int,
        orderType: String,
        formattedTime: String
    ) {
        val safeAdminEmail = adminEmail.replace(".", "_") //realtime datebase에서는 . 사용 불가
        val orderRef = database.getReference("admin_orders/$safeAdminEmail/$formattedTime")

        val orderMap = mapOf(
            "consumerEmail" to consumerEmail,
            "tableId" to tableId,
            "items" to orderData,
            "totalAmount" to totalAmount,
            "orderType" to orderType,
            "orderTime" to formattedTime
        )

        orderRef.setValue(orderMap)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "주문이 완료 되었습니다.", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearCart() {
        cartItems.clear()
        saveCartData()
        cartAdapter.notifyDataSetChanged()
        updateTotalPrice()
    }

    private fun goToConsumerMenuList() {
        findNavController().navigate(R.id.action_to_ConsumerMenuList, arguments)
    }
}