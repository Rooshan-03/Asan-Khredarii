package com.rooshan.AsanKhredari.DataClass

data class CustomerShopItemsDataClass(
    var id: Long? = null,
    var itemName: String? = null,
    var price: Double? = null,
    var deliveryPrice: Double? = null,
    var quantity: Long? = null,
    var totalPrice: Double? = null,
    var unit: String? = null
)