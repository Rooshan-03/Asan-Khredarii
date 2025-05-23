package com.rooshan.AsanKhredari.DataClass

data class RetailerItemDataClass(
    var id: String? = null,
    var itemName: String? = null,
    var price: Double? = null,
    var deliveryPrice: Double? = null,
    var quantity: String? = null, // Change to String? to match Firestore
    var totalPrice: Double? = null,
    var unit: String? = null, // Change from itemUnit to unit to match Firestore
    var imageBase64: String? = null
)