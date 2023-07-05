package com.ys.bt

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.util.Log
import java.util.HashSet

class BTScanDevice(private val btHelper: BTHelper) {
    private var TAG = BTScanDevice::class.java.simpleName
    val scanDeviceList = HashSet<BluetoothDevice>()
    var isScanning = false

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            try {
                Log.d("YsBT: $TAG", "onScanResult -> device: ${result.device}. scanRecord: ${Binary(result.scanRecord!!.bytes).toHEX()}, rssi: ${result.rssi}")
                btHelper.nCallBack.onScanDeviceResult(result.device, Binary(result.scanRecord!!.bytes), result.rssi)
                scanDeviceList.add(result.device)
            } catch (e:Exception) {
                Log.d("YsBT: $TAG", "Scan error: $e")
            }
        }

        override fun onScanFailed(errorCode: Int) {
            Log.d("YsBT: $TAG", "Scan error: $errorCode")
        }
    }

    fun startScanDevice() {
        if (!btHelper.checkAndRequestPermission()) return
        if (!btHelper.checkBluetoothStatus()) return
        btHelper.bluetoothAdapter!!.bluetoothLeScanner.startScan(null, getScanSetting(), scanCallback)
        scanDeviceList.clear()
        isScanning = true
    }

    fun stopScanDevice() {
        if (!btHelper.checkAndRequestPermission()) return
        if (!btHelper.checkBluetoothStatus()) return
        btHelper.bluetoothAdapter!!.bluetoothLeScanner.stopScan(scanCallback)
        isScanning = false
    }

    private fun getScanSetting(): ScanSettings {
        val builder = ScanSettings.Builder().also { it.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY) }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) builder.setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
        return builder.build()
    }
}