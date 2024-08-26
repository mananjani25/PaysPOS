package com.pays.pos.logger

data class MessageEvent(
    var data: String,
    var newTrack:Boolean=false
)