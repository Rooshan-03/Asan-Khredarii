package com.rooshan.AsanKhredari.DataClass

data class CustomerShopItemsDataClass(
    var key: String = "",
    var itemName: String? = null,
    var price: Double? = null,
    var deliveryPrice: Double? = null,
    var quantity: Long? = null,
    var totalPrice: Double? = null,
    var unit: String? = null
)