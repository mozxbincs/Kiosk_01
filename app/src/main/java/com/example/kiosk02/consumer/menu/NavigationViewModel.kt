package com.example.kiosk02.consumer.menu

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class NavigationViewModel : ViewModel() {
    private val _isNavigated = MutableLiveData(false)
    val isNavigated: LiveData<Boolean> get() = _isNavigated

    fun setNavigated(value: Boolean) {
        _isNavigated.value = value
    }

    private val _isOrderPlaced = MutableLiveData(false)
    val isOrderPlaced: LiveData<Boolean> get() = _isOrderPlaced

    fun setOrderPlaced(value: Boolean) {
        _isOrderPlaced.value = value
    }
}