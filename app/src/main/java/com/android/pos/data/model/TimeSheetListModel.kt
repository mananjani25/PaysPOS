package com.android.pos.data.model

data class TimeSheetListModel(
    val customerName: String,
    val totalHours: String,
    val totalWage: String,
    val mon: String,
    val tue: String,
    val wed: String,
    val thu: String,
    val fri: String,
    val sat: String,
    val sun: String
)