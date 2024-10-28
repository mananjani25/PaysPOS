package com.pays.pos.logger


data class CreateCustomerEvent(
    var performCreate:Boolean=false,
    var phoneNumber: String
)