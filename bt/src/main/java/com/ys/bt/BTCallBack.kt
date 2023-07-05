package com.ys.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService

interface BTCallBack {
    fun onRequestPermission(list: ArrayList<String>)
    fun onScanDeviceResult(device: BluetoothDevice, scanRecord: Binary, rssi:Int)
    fun onStatusChange(status: Int)
    fun rx(uuid: String, value: ByteArray?)
    fun tx(uuid: String, value: ByteArray?)
    fun onConnectionStateChange(isConnect: Boolean, serviceList: List<BluetoothGattService>?)
}