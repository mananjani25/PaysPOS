package com.pays.pos.utils.scanner.helpers

import com.pays.pos.utils.scanner.helpers.CustomProgressDialog
import com.pays.pos.utils.scanner.helpers.Foreground
import com.pays.pos.utils.scanner.helpers.ManagedVibrator
import com.zebra.scannercontrol.FirmwareUpdateEvent
import com.pays.pos.utils.scanner.helpers.ScannerAppEngine.IScannerAppEngineDevListDelegate
import com.pays.pos.utils.scanner.helpers.ScannerAppEngine.IScannerAppEngineDevConnectionsDelegate
import com.pays.pos.utils.scanner.helpers.ScannerAppEngine.IScannerAppEngineDevEventsDelegate
import com.zebra.scannercontrol.DCSScannerInfo
import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_RESULT
import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_MODE
import com.zebra.scannercontrol.DCSSDKDefs.DCSSDK_COMMAND_OPCODE
import com.pays.pos.utils.scanner.helpers.SSASymbologyType
import com.zebra.scannercontrol.RMDAttributes
import com.pays.pos.utils.scanner.helpers.Symbology
import java.io.Serializable
import java.util.ArrayList

/**
 * Created by BPallewela on 11/30/2017.
 */
class SSASymbologyType(  // member variables
    var symbologyName: String
) : Serializable {
    private var attrIDDecodeCountHexValue: String? = null
    private var attrIDDecodeCount: Int? = null
    private var attrIDMinDecodeTime: Int? = null
    private var attrIDMaxDecodeTime: Int? = null
    private var attrIDAvgDecodeTime: Int? = null
    private var attrIDSlowestDecodeData: Int? = null
    private var attrIDScanSpeedHistogram: Int? = null
    fun setAttrIDDecodeCountHexValue(attrIDDecodeCountHexValue: String?) {
        this.attrIDDecodeCountHexValue = attrIDDecodeCountHexValue
    }

    fun getAttrIDDecodeCountHexValue(): String? {
        return attrIDDecodeCountHexValue
    }

    fun setAttrIDDecodeCount(attrIDDecodeCount: Int?) {
        this.attrIDDecodeCount = attrIDDecodeCount
    }

    fun getAttrIDDecodeCount(): Int? {
        return attrIDDecodeCount
    }

    fun setAttrIDMinDecodeTime(attrIDMinDecodeTime: Int?) {
        this.attrIDMinDecodeTime = attrIDMinDecodeTime
    }

    fun getAttrIDMinDecodeTime(): Int? {
        return attrIDMinDecodeTime
    }

    fun setAttrIDMaxDecodeTime(attrIDMaxDecodeTime: Int?) {
        this.attrIDMaxDecodeTime = attrIDMaxDecodeTime
    }

    fun getAttrIDMaxDecodeTime(): Int? {
        return attrIDMaxDecodeTime
    }

    fun setAttrIDAvgDecodeTime(attrIDAvgDecodeTime: Int?) {
        this.attrIDAvgDecodeTime = attrIDAvgDecodeTime
    }

    fun getAttrIDAvgDecodeTime(): Int? {
        return attrIDAvgDecodeTime
    }

    fun setAttrIDSlowestDecodeData(attrIDSlowestDecodeData: Int?) {
        this.attrIDSlowestDecodeData = attrIDSlowestDecodeData
    }

    fun getAttrIDSlowestDecodeData(): Int? {
        return attrIDSlowestDecodeData
    }

    fun setAttrIDScanSpeedHistogram(attrIDScanSpeedHistogram: Int?) {
        this.attrIDScanSpeedHistogram = attrIDScanSpeedHistogram
    }

    fun getAttrIDScanSpeedHistogram(): Int? {
        return attrIDScanSpeedHistogram
    }

    override fun toString(): String {
        return symbologyName
    }

    companion object {
        //    public static SSASymbologyType getUPCSymbplogyObject(){
        //        SSASymbologyType retSSASymboType = new SSASymbologyType("UPC");
        //        retSSASymboType.attrIDAvgDecodeTime = 1;
        //        retSSASymboType.attrIDDecodeCount = 2;
        //        return retSSASymboType;
        //    }
        fun getSSASymbologyList(supportedIDList: List<Int?>): List<SSASymbologyType> {
            val resultSymList: MutableList<SSASymbologyType> = ArrayList()
            for (supportId in supportedIDList) {
                var tempSymType: SSASymbologyType
                when (supportId) {
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_UPC -> {
                        // UPC
                        tempSymType = SSASymbologyType("UPC")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_UPC // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_UPC // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_UPC // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_UPC // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_UPC // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_UPC // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_UPC // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_EAN_JAN -> {
                        // EAN/JAN
                        tempSymType = SSASymbologyType("EAN/JAN")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_EAN_JAN // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_EAN_JAN // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_EAN_JAN // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_EAN_JAN // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_EAN_JAN // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_EAN_JAN // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_EAN_JAN // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_2_OF_5 -> {
                        // 2 of 5
                        tempSymType = SSASymbologyType("2 of 5")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_2_OF_5 // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_2_OF_5 // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_2_OF_5 // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_2_OF_5 // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_2_OF_5 // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_2_OF_5 // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_2_OF_5 // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_CODEBAR -> {
                        // Codebar
                        tempSymType = SSASymbologyType("Codebar")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_CODEBAR // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_CODEBAR // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_CODEBAR // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_CODEBAR // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_CODEBAR // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_CODEBAR // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_CODEBAR // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_CODE_11 -> {
                        // Code 11
                        tempSymType = SSASymbologyType("Code 11")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_CODE_11 // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_CODE_11 // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_CODE_11 // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_CODE_11 // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_CODE_11 // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_CODE_11 // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_CODE_11 // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_CODE_128 -> {
                        // Code 128
                        tempSymType = SSASymbologyType("Code 128")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_CODE_128 // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_CODE_128 // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_CODE_128 // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_CODE_128 // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_CODE_128 // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_CODE_128 // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_CODE_128 // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_CODE_39 -> {
                        // Code 39
                        tempSymType = SSASymbologyType("Code 39")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_CODE_39 // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_CODE_39 // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_CODE_39 // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_CODE_39 // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_CODE_39 // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_CODE_39 // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_CODE_39 // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_CODE_93 -> {
                        // Code 93
                        tempSymType = SSASymbologyType("Code 93")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_CODE_93 // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_CODE_93 // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_CODE_93 // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_CODE_93 // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_CODE_93 // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_CODE_93 // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_CODE_93 // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_COMPOSITE -> {
                        // Composite
                        tempSymType = SSASymbologyType("Composite")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_COMPOSITE // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_COMPOSITE // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_COMPOSITE // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_COMPOSITE // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_COMPOSITE // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_COMPOSITE // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_COMPOSITE // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_GS1_DATABAR -> {
                        // GS1 Databar
                        tempSymType = SSASymbologyType("GS1 Databar")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_GS1_DATABAR // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_GS1_DATABAR // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_GS1_DATABAR // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_GS1_DATABAR // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_GS1_DATABAR // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_GS1_DATABAR // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_GS1_DATABAR // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_MSI -> {
                        // MSI
                        tempSymType = SSASymbologyType("MSI")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_MSI // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_MSI // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_MSI // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_MSI // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_MSI // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_MSI // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_MSI // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_DATAMARIX -> {
                        // Datamatrix
                        tempSymType = SSASymbologyType("Datamatrix")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_DATAMARIX // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_DATAMARIX // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_DATAMARIX // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_DATAMARIX // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_DATAMARIX // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_DATAMARIX // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_DATAMARIX // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_PDF -> {
                        // PDF
                        tempSymType = SSASymbologyType("PDF")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_PDF // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_PDF // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_PDF // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_PDF // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_PDF // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_PDF // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_PDF // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_POSTAL_CODES -> {
                        // Postal Codes
                        tempSymType = SSASymbologyType("Postal Codes")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_POSTAL_CODES // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_POSTAL_CODES // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_POSTAL_CODES // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_POSTAL_CODES // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_POSTAL_CODES // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_POSTAL_CODES // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_POSTAL_CODES // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_QR -> {
                        // QR
                        tempSymType = SSASymbologyType("QR")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_QR // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_QR // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_QR // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_QR // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_QR // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_QR // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_QR // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_AZTEC -> {
                        // Aztec
                        tempSymType = SSASymbologyType("Aztec")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_AZTEC // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_AZTEC // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_AZTEC // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_AZTEC // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_AZTEC // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_AZTEC // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_AZTEC // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_OCR -> {
                        // OCR
                        tempSymType = SSASymbologyType("OCR")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_OCR // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_OCR // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_OCR // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_OCR // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_OCR // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_OCR // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_OCR // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_MAXICODE -> {
                        // Maxicode
                        tempSymType = SSASymbologyType("Maxicode")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_MAXICODE // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_MAXICODE // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_MAXICODE // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_MAXICODE // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_MAXICODE // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_MAXICODE // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_MAXICODE // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_GS1_DATAMATRIX -> {
                        // GS1-Datamatrix
                        tempSymType = SSASymbologyType("GS1-Datamatrix")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_GS1_DATAMATRIX // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_GS1_DATAMATRIX // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_GS1_DATAMATRIX // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_GS1_DATAMATRIX // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_GS1_DATAMATRIX // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_GS1_DATAMATRIX // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_GS1_DATAMATRIX // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_GS1_QR_CODE -> {
                        // GS1-QR Code
                        tempSymType = SSASymbologyType("GS1-QR Code")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_GS1_QR_CODE // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_GS1_QR_CODE // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_GS1_QR_CODE // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_GS1_QR_CODE // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_GS1_QR_CODE // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_GS1_QR_CODE // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_GS1_QR_CODE // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_COUPON -> {
                        // Coupon
                        tempSymType = SSASymbologyType("Coupon")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_COUPON // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_COUPON // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_COUPON // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_COUPON // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_COUPON // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_COUPON // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_COUPON // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_DIGIMARC_UPC -> {
                        // Digimarc UPC
                        tempSymType = SSASymbologyType("Digimarc UPC")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_DIGIMARC_UPC // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_DIGIMARC_UPC // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_DIGIMARC_UPC // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_DIGIMARC_UPC // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_DIGIMARC_UPC // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_DIGIMARC_UPC // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_DIGIMARC_UPC // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_DIGIMARC_EAN_JAN -> {
                        // Digimarc EAN/JAN
                        tempSymType = SSASymbologyType("Digimarc EAN/JAN")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_DIGIMARC_EAN_JAN // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_DIGIMARC_EAN_JAN // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_DIGIMARC_EAN_JAN // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_DIGIMARC_EAN_JAN // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_DIGIMARC_EAN_JAN // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_DIGIMARC_EAN_JAN // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_DIGIMARC_EAN_JAN // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_DIGIMARC_OTHER -> {
                        // Digimarc Other
                        tempSymType = SSASymbologyType("Digimarc Other")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_DIGIMARC_OTHER // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_DIGIMARC_OTHER // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_DIGIMARC_OTHER // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_DIGIMARC_OTHER // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_DIGIMARC_OTHER // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_DIGIMARC_OTHER // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_DIGIMARC_OTHER // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_OTHER_1D -> {
                        // Other 1D
                        tempSymType = SSASymbologyType("Other 1D")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_OTHER_1D // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_OTHER_1D // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_OTHER_1D // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_OTHER_1D // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_OTHER_1D // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_OTHER_1D // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_OTHER_1D // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_OTHER_2D -> {
                        // Other 2D
                        tempSymType = SSASymbologyType("Other 2D")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_OTHER_2D // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_OTHER_2D // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_OTHER_2D // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_OTHER_2D // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_OTHER_2D // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_OTHER_2D // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_OTHER_2D // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_OTHER -> {
                        // Other
                        tempSymType = SSASymbologyType("Other")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_OTHER // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_OTHER // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_OTHER // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_OTHER // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_OTHER // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_OTHER // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_OTHER // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                    RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_UNUSED_ID -> {
                        // UNUSED STATISTIC ID
                        tempSymType = SSASymbologyType("UNUSED STATISTIC ID")
                        tempSymType.attrIDDecodeCountHexValue =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_HEX_UNUSED_ID // Decode count Hex value (Little Endian)
                        tempSymType.attrIDDecodeCount =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_DECODE_COUNT_UNUSED_ID // Decode count
                        tempSymType.attrIDMinDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MIN_DECODE_TIME_UNUSED_ID // Minimum Decode time
                        tempSymType.attrIDMaxDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_MAX_DECODE_TIME_UNUSED_ID // Maximum(Slowest) Decode Time
                        tempSymType.attrIDAvgDecodeTime =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_AVG_DECODE_TIME_UNUSED_ID // Average Decode Time
                        tempSymType.attrIDSlowestDecodeData =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_SLOW_DECODE_DATA_UNUSED_ID // Slowest Decode Data
                        tempSymType.attrIDScanSpeedHistogram =
                            RMDAttributes.RMD_ATTR_VALUE_SSA_HISTOGRAM_UNUSED_ID // Scan Speed Histogram
                        resultSymList.add(tempSymType)
                    }
                }
            }
            return resultSymList
        }
    }
}