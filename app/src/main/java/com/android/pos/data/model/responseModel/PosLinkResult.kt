package com.android.pos.data.model.responseModel

import org.simpleframework.xml.Element
import org.simpleframework.xml.Namespace
import org.simpleframework.xml.Root

@Root(name = "Result", strict = false)
@Namespace(reference = "http://poslink.com/")
data class PosLinkResult(
    @field:Element(name = "ResultCode")
    var resultCode: Int = 0,

    @field:Element(name = "ResultMsg")
    var resultMsg: String = "",

    @field:Element(name = "IPaddress")
    var ipAddress: String = "",

    @field:Element(name = "Port")
    var port: Int = 0,

    @field:Element(name = "MacAddress")
    var macAddress: String = "",

    @field:Element(name = "TerminalId")
    var terminalId: String = "",

    @field:Element(name = "Token")
    var token: String = "",

    @field:Element(name = "SerialNo")
    var serialNo: String = ""
)