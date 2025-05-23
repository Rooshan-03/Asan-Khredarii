package com.rooshan.AsanKhredari.DataClass

data class CustomerShopItemsDataClass(
    var id: String? = null,
    var itemName: String? = null,
    var price: Double? = null, // Made nullable to handle potential null from Firestore
    var deliveryPrice: Double? = null, // Made nullable
    var quantity: String? = null, // Already nullable, matches Firestore
    var totalPrice: Double? = null, // Keep as Double? if you want to calculate it
    var unit: String? = null,
    var imageBase64: String? = null,
)