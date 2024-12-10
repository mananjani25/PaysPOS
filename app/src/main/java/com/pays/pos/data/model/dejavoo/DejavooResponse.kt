package com.pays.pos.data.model.dejavoo

import org.simpleframework.xml.Element
import org.simpleframework.xml.Root

@Root(name = "response", strict = false)
data class DejavooResponse(
    @field:Element(name = "Message", required = false)
    var message: String? = null,

    @field:Element(name = "ResultCode", required = false)
    var resultCode: String? = null,

    @field:Element(name = "RespMSG", required = false)
    var respMSG: String? = null
)
