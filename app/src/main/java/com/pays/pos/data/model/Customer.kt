package com.pays.pos.data.model

data class Customer(
    val addresses: List<Any>,
    val birth_date: Any,
    val company: Any,
    val email: Any,
    val enroll_to_loyalty: Boolean,
    val final_reward: Int,
    val first_name: String,
    val id: Int,
    val last_name: String,
    val phones: List<Phone>
)