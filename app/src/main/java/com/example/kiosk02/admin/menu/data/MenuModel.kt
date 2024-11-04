package com.example.kiosk02.admin.menu.data
import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class MenuModel (
    val menuId: String = "",
    val menuName: String? = null,
    val imageUrl: String? = null,
    val composition: String? = null,
    val detail: String? = null,
    val price: Int? = null,
    val category: String? = null,
    var quantity: Int = 1
) : Parcelable