package com.example.kiosk02.consumer.menu

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
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

    private var Aemail: String? = null
    private var Uemail: String? = null
    private var selectedTableId: String? = null
    private var selectedFloor: String? = null

    private lateinit var navigationViewModel: NavigationViewModel

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

        // Shared ViewModel 초기화
        navigationViewModel = ViewModelProvider(requireActivity()).get(NavigationViewModel::class.java)

        loadCartData()
        setupRecyclerView()

        binding.backButton.setOnClickListener {
            goToConsumerMenuList()
            navigationViewModel.setNavigated(true)
        }

        binding.toOrderButton.setOnClickListener {
            val orderType = arguments?.getString("orderType") ?: "unknown"

            if (orderType == "for_here") {
                // Firestore에서 해당 테이블의 select 컬렉션 존재 여부 확인
                val selectDocRef = firestore.collection("admin")
                    .document(Aemail!!)
                    .collection("floors")
                    .document(selectedFloor!!)
                    .collection("tables")
                    .document(selectedTableId!!)
                    .collection("select")
                    .document(Uemail!!)

                selectDocRef.get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // select 컬렉션이 존재하면 주문 진행
                            placeOrder()
                            navigationViewModel.setNavigated(true)
                        } else {
                            // select 컬렉션이 없으면 ConsumerTableFragment로 이동
                            val updatedArguments = arguments?.let {
                                val newBundle = Bundle(it)
                                newBundle.remove("selectedTableId") // selectedTableId 제거
                                newBundle
                            }

                            findNavController().navigate(R.id.action_to_table_Select_Fragment, updatedArguments)
                        }
                    }
                    .addOnFailureListener { exception ->
                        Log.e("ConsumerCartFragment", "Firestore 조회 중 오류 발생", exception)
                    }
            }else{
                placeOrder()
                navigationViewModel.setNavigated(true)
            }
        }
    }

//    override fun onResume() {
//        super.onResume()
//        // Fragment로 돌아올 때 select 생성
//        if (!Aemail.isNullOrEmpty() && !selectedTableId.isNullOrEmpty() && !selectedFloor.isNullOrEmpty()) {
//            createSelectCollection(Aemail!!, selectedFloor!!, selectedTableId!!)
//                .addOnSuccessListener {
//                    Log.d("ConsumerCartFragment", "select 재생성 완료")
//                }
//                .addOnFailureListener { e ->
//                    Log.e("ConsumerCartFragment", "select 재생성 실패", e)
//                }
//        }
//    }

    override fun onPause() {
        super.onPause()

        // 주문 완료 상태에서는 onPause 동작 방지
        if (navigationViewModel.isOrderPlaced.value == true) {
            Log.d("ConsumerOrderFragment", "Order placed - Skipping onPause actions")
            return
        }

        if (!navigationViewModel.isNavigated.value!! &&
            !navigationViewModel.isOrderPlaced.value!!
        ) {
            findNavController().navigate(R.id.action_to_table_Select_Fragment, arguments)
            deleteSelectCollection()
        }
        // onPause 후 isNavigated 플래그 초기화
        navigationViewModel.setNavigated(false)
    }

//    private fun createSelectCollection(Aemail: String, floor: String, tableId: String): Task<Void> {
//        val selectDocRef = firestore.collection("admin")
//            .document(Aemail)
//            .collection("floors")
//            .document(floor)
//            .collection("tables")
//            .document(tableId)
//            .collection("select")
//            .document(Uemail!!)
//
//        return selectDocRef.set(mapOf("select" to true))
//    }

    private fun deleteSelectCollection() {
        val Aemail = arguments?.getString("Aemail")
        val floor = arguments?.getString("selectedFloor")
        val tableId = arguments?.getString("selectedTableId")
        val Uemail = arguments?.getString("Uemail")

        if (!Aemail.isNullOrEmpty() && !floor.isNullOrEmpty() && !tableId.isNullOrEmpty() && !Uemail.isNullOrEmpty()) {
            val selectDocRef = firestore.collection("admin")
                .document(Aemail)
                .collection("floors")
                .document(floor)
                .collection("tables")
                .document(tableId)
                .collection("select")
                .document(Uemail)

            selectDocRef.delete()
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

        // 주문 완료 상태 업데이트
        navigationViewModel.setOrderPlaced(true)
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
                    arguments = arguments?.apply {
                        putBoolean("isOrderd", true)
                    }
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
        val orderRef = database.getReference("admin_orders/$safeAdminEmail/$tableId/$formattedTime")

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