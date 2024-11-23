package com.example.kiosk02.admin.orderStatus.data

data class Order(
    var orderId: String = "",
    var adminEmail: String = "",
    var tableId: String = "",
    var orderTime: String = "",
    var orderType: String = "",
    var totalAmount: Int = 0,
    var items: List<MenuItem> = listOf()
)

data class MenuItem(
    var menuName: String = "",
    var quantity: Int = 1,
    var totalPrice: Int = 0
)