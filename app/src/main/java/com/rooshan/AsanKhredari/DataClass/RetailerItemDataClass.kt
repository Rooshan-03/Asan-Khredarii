package com.rooshan.AsanKhredari.DataClass

data class RetailerItemDataClass(
    var id: String? = null,
    var itemName: String? = null,
    var price: Double? = null,
    var deliveryPrice: Double? = null,
    var quantity: String? = null, // Kept as String? to match input and display
    var totalPrice: Double? = null,
    var unit: String? = null
)