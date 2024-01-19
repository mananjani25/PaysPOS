package com.pays.pos.data.model

data class ClockinOutReportModel(
    var empName: String ="",
    var clockIn: String ="",
    var clockOutval: String="",
    var actualTime: String="",
    var totalTime: String=""
)