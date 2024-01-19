package com.pays.pos.data.model

data class SingleMemberTimeSheetListModel(
    val clockIn: String,
    val clockOut: String,
    val wage: String,
    val break1: String,
    val workingHours: String,
    val totalWage: String
)