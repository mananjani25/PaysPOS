package com.pays.pos.ui.fragments.magtekPro

import java.lang.Exception
import java.lang.StringBuilder
import java.util.ArrayList
import java.util.HashMap
import kotlin.experimental.and

object MTParser {
    fun parseTLV(data: ByteArray?): List<HashMap<String, String>> {
        val fillMaps: MutableList<HashMap<String, String>> = ArrayList()
        if (data != null) {
            val dataLen = data.size
            if (dataLen >= 2) {
                val tlvLen = data.size
                if (data != null) {
                    var iTLV: Int
                    var iTag: Int
                    var iLen: Int
                    var bTag: Boolean
                    var bMoreTagBytes: Boolean
                    var bConstructedTag: Boolean
                    var byteValue: Byte
                    var lengthValue: Int
                    var tagBytes: ByteArray? = null
                    val MoreTagBytesFlag1 = 0x1F.toByte()
                    val MoreTagBytesFlag2 = 0x80.toByte()
                    val ConstructedFlag = 0x20.toByte()
                    val SpecialTagFlag = 0xF0.toByte()
                    val MoreLengthFlag = 0x80.toByte()
                    val OneByteLengthMask = 0x7F.toByte()
                    val TagBuffer = ByteArray(50)
                    bTag = true
                    iTLV = 0
                    while (iTLV < data.size) {
                        byteValue = data[iTLV]
                        if (bTag) {
                            // Get Tag
                            iTag = 0
                            bMoreTagBytes = true
                            while (bMoreTagBytes && iTLV < data.size) {
                                byteValue = data[iTLV]
                                iTLV++
                                TagBuffer[iTag] = byteValue
                                bMoreTagBytes = if (iTag == 0) {
                                    byteValue and MoreTagBytesFlag1 == MoreTagBytesFlag1
                                } else {
                                    byteValue and MoreTagBytesFlag2 == MoreTagBytesFlag2
                                }
                                iTag++
                            }
                            tagBytes = ByteArray(iTag)
                            System.arraycopy(TagBuffer, 0, tagBytes, 0, iTag)
                            bTag = false
                        } else {
                            // Get Length
                            lengthValue = 0
                            if (byteValue and MoreLengthFlag == MoreLengthFlag) {
                                val nLengthBytes = (byteValue and OneByteLengthMask) as Int
                                iTLV++
                                iLen = 0
                                while (iLen < nLengthBytes && iTLV < data.size) {
                                    byteValue = data[iTLV]
                                    iTLV++
                                    lengthValue =
                                        (lengthValue and 0x000000FF shl 8) + (byteValue and 0x000000FF.toByte()) as Int
                                    iLen++
                                }
                            } else {
                                lengthValue = (byteValue and OneByteLengthMask) as Int
                                iTLV++
                            }
                            if (tagBytes != null) {
                                val tagByte = tagBytes[0].toInt()
                                bConstructedTag =
                                    tagByte and ConstructedFlag.toInt() == ConstructedFlag.toInt()
                                if (bConstructedTag) {
                                    // Constructed
                                    val map = HashMap<String, String>()
                                    map["tag"] =
                                        getHexString(
                                            tagBytes
                                        )
                                    map["len"] = "" + lengthValue
                                    map["value"] = "[Container]"
                                    fillMaps.add(map)
                                } else {
                                    // Primitive
                                    var endIndex = iTLV + lengthValue
                                    if (endIndex > data.size) endIndex = data.size
                                    var valueBytes: ByteArray? = null
                                    val len = endIndex - iTLV
                                    if (len > 0) {
                                        valueBytes = ByteArray(len)
                                        System.arraycopy(
                                            data,
                                            iTLV,
                                            valueBytes,
                                            0,
                                            len
                                        )
                                    }
                                    val map = HashMap<String, String>()
                                    map["tag"] =
                                        getHexString(
                                            tagBytes
                                        )
                                    map["len"] = "" + lengthValue
                                    if (valueBytes != null) map["value"] =
                                        getHexString(
                                            valueBytes
                                        ) else map["value"] = ""
                                    fillMaps.add(map)
                                    iTLV += lengthValue
                                }
                            }
                            bTag = true
                        }
                    }
                }
            }
        }
        return fillMaps
    }

    fun getTagValue(fillMaps: List<HashMap<String, String>>, tagString: String?): String? {
        var valueString = ""
        val it = fillMaps.listIterator()
        while (it.hasNext()) {
            val map = it.next()
            if (map["tag"].equals(tagString, ignoreCase = true)) {
                valueString = map["value"].toString()
            }
        }
        return valueString
    }

    fun getTagByteArrayValue(
        fillMaps: List<HashMap<String, String>>,
        tagString: String?
    ): ByteArray? {
        var valueBytes: ByteArray? = null
        val valueString = getTagValue(fillMaps, tagString)
        valueBytes = getByteArrayFromHexString(valueString)
        return valueBytes
    }

    fun getTextString(data: ByteArray?, start: Int): String {
        var result = ""
        if (data != null && data.size > 0) {
            result = getTextString(data, start, data.size)
        }
        return result
    }

    fun getTextString(data: ByteArray?, start: Int, length: Int): String {
        var result = ""
        if (data != null && data.size > 0) {
            val stringBuilder = StringBuilder(data.size + 1)
            for (i in start until length) {
                try {
                    stringBuilder.append(String.format("%c", data[i]))
                } catch (ex: Exception) {
                    stringBuilder.append("<?>")
                }
            }
            result = stringBuilder.toString()
        }

//        Log.i(TAG, "Data: " + result);
        return result
    }

    fun getHexString(data: ByteArray?): String {
        var result = ""
        if (data != null && data.size > 0) {
            val byteLength = 2
            val stringBuilder = StringBuilder(data.size * byteLength + 1)
            for (i in data.indices) {
                try {
                    stringBuilder.append(String.format("%02X", data[i]))
                } catch (ex: Exception) {
                    stringBuilder.append("  ")
                }
            }
            result = stringBuilder.toString()
        }
        return result
    }

    fun getByteArrayFromHexString(hexString: String?): ByteArray? {
        val byteLength = 2
        var result: ByteArray? = null
        if (hexString != null) {
            result = ByteArray(hexString.length / byteLength)
            val hexCharArray = hexString.toUpperCase().toCharArray()
            var sbCurrent: StringBuffer
            for (i in result.indices) {
                sbCurrent = StringBuffer("")
                sbCurrent.append(hexCharArray[i * byteLength].toString())
                sbCurrent.append(hexCharArray[i * byteLength + 1].toString())
                try {
                    result[i] = sbCurrent.toString().toInt(16).toByte()
                } catch (ex: Exception) {
                }
            }
        }
        return result
    }
}