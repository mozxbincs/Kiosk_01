package com.example.kiosk02.admin.orderStatus.data

data class Order(
    var orderId: String = "",
    val orderTime: String = "00000000_000000", // 예: "20241118_133237"
    val orderType: String = "",
    val tableId: String = "",
    val totalAmount: Int = 0,
    val items: List<OrderItem> = emptyList()
)
