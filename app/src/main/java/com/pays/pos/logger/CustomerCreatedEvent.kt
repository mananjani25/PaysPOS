package com.pays.pos.logger

import com.pays.pos.data.entities.TbCustomer

data class CustomerCreatedEvent(
    var created:Boolean,
    var customer: TbCustomer
)