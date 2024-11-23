package com.example.kiosk02

data class Order(
    val consumerEmail: String = "",
    val items: List<MenuItem> = emptyList(),
    val orderTime: String = "",
    val orderType: String = "",
    val tableId: String = "",
    val totalAmount: Int = 0
)

data class  MenuItem(
    val menuName: String = "",
    val quantity: Int = 0,
    val totalPrice: Int = 0
)