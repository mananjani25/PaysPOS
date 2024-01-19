package com.pays.pos.data.model.responseModel.allOrders

data class Data(
    val all_orders: AllOrders,
    val open_orders: OpenOrders,
    val phone_orders: PhoneOrders,
    val third_party_online_orders: ThirdPartyOnlineOrders,
    val web_orders: WebOrders
)