package com.pays.pos.data.model.requestModel


import com.google.gson.annotations.SerializedName

data class CreateNoteRequest(
    @SerializedName("note")
    var note: Note = Note()
) {
    data class Note(
        @SerializedName("is_active")
        var isActive: Boolean = false,
        @SerializedName("location_id")
        var locationId: Int = 0,
        @SerializedName("name")
        var name: String = ""
    )
}
