package com.pays.pos.utils.callback

import android.bluetooth.BluetoothDevice
import android.view.View
import com.pays.pos.data.entities.Employee
import com.pays.pos.data.model.responseModel.EmployeeListResponse
import com.magtek.mobile.android.mtlib.IMTCardData
import com.magtek.mobile.android.mtlib.MTConnectionState

interface magtekCallback {
    fun startScanning()
    fun processStart(s: String, b: Boolean)
    fun stopScanning()
    fun onConnect(deviceState: MTConnectionState)
    fun onDeviceResponse(response: String)
    fun onDeviceList(bluetoothDevice: BluetoothDevice)
    fun OnCardDataReceived(imtCardData: IMTCardData)
    fun OnARQCReceived(bytes: ByteArray)
}