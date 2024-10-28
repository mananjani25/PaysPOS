package com.pays.pos.logger

data class SyncCustomerEvent(
    var performCreate:Boolean=false,
    var customerName: String
)