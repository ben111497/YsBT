package com.ys.bt

import android.Manifest
import android.bluetooth.*
import android.content.Context
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.core.app.ActivityCompat
import com.ys.bt.databinding.ActivitySampleBinding
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

class SampleActivity : AppCompatActivity(), BTCallBack {
    private lateinit var binding: ActivitySampleBinding
    private lateinit var deviceListAdapter: BTListAdapter
    private lateinit var detailAdapter: BTDetailAdapter
    private lateinit var btHelper: BTHelper
    private val devices = HashSet<BluetoothDevice>()
    private val selectedDevices = ArrayList<BluetoothDevice>()

    override fun onRequestPermission(list: ArrayList<String>) { checkAndRequestPermission(list[0], 0) }

    override fun onScanDeviceResult(device: BluetoothDevice, scanRecord: Binary, rssi: Int) { devices.add(device) }

    override fun onStatusChange(status: Int) {
        when (status) {
            BluetoothAdapter.STATE_OFF -> { btStatusChange(false) }
            BluetoothAdapter.STATE_TURNING_OFF -> {}
            BluetoothAdapter.STATE_ON -> { btStatusChange(true) }
            BluetoothAdapter.STATE_TURNING_ON -> {}
        }
    }

    override fun rx(uuid: String, value: ByteArray?) {
        //TODO("Not yet implemented")
    }

    override fun tx(uuid: String, value: ByteArray?) {
        //TODO("Not yet implemented")
    }

    override fun onConnectionStateChange(isConnect: Boolean, serviceList: List<BluetoothGattService>?) {
        runOnUiThread {
            if (isConnect && serviceList != null) detailFragment(serviceList) else Toast.makeText(this@SampleActivity, "Connect error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            when (requestCode) {
                0 -> checkAndRequestPermission(Manifest.permission.BLUETOOTH_CONNECT, 1)
                1 -> checkAndRequestPermission(Manifest.permission.BLUETOOTH_SCAN, 2)
                2 -> if (!::btHelper.isInitialized) initBTHelper()
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySampleBinding.inflate(layoutInflater)
        setContentView(binding.root)
        init()
        setListener()
    }
    private fun init() {
        if (checkAndRequestPermission(Manifest.permission.BLUETOOTH_CONNECT, 1) &&
            checkAndRequestPermission(Manifest.permission.BLUETOOTH_SCAN, 2) && checkAndRequestPermission(Manifest.permission.BLUETOOTH_SCAN, 3)) {
            initBTHelper()
        }
    }

    private fun initBTHelper() {
        btHelper = BTHelper(this, this)
        btStatusChange(btHelper.isBTOpen)
    }

    private fun setListener() {
        binding.run {
            btnScan.setOnClickListener {
                if (!checkBT()) return@setOnClickListener
                devices.clear()
                selectedDevices.clear()
                btHelper.scanDevice()
                binding.pbBTScan.visibility = View.VISIBLE
                Handler(Looper.myLooper()!!).postDelayed({
                    runOnUiThread {
                        btHelper.stopScanDevice()
                        binding.pbBTScan.visibility = View.GONE
                        setDeviceListView()
                    } }, 1000)
            }

            tvEnable.setOnClickListener {
                if (!checkBT()) return@setOnClickListener
                btHelper.openBT()
            }

            imgDisconnect.setOnClickListener {
                if (!checkBT()) return@setOnClickListener
                btHelper.disConnect()
                clDetail.visibility = View.GONE
            }

            imgSearch.setOnClickListener {
                if (!checkBT()) return@setOnClickListener
                if (!checkPermission()) return@setOnClickListener

                if (binding.edSearch.text.isEmpty()) {
                    selectedDevices.clear()
                    selectedDevices.addAll(devices)
                } else {
                    val list = ArrayList<BluetoothDevice>()
                    list.addAll(ArrayList(devices))
                    list.removeAll { it.name == null }
                    selectedDevices.clear()
                    selectedDevices.addAll(ArrayList(list.filter { it.name.lowercase().contains(binding.edSearch.text.toString().lowercase()) }))
                }

                deviceListAdapter.notifyDataSetChanged()

                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.edSearch.windowToken, 0)
            }
        }
    }

    private fun setDeviceListView() {
        if (!checkPermission()) return

        if (binding.edSearch.text.toString().isNotEmpty())
            selectedDevices.addAll(devices.filter { it.name != null && it.name.lowercase().contains(binding.edSearch.text.toString().lowercase()) })
        else
            selectedDevices.addAll(devices)

        if (!::deviceListAdapter.isInitialized) {
            deviceListAdapter = BTListAdapter(this, selectedDevices)
            deviceListAdapter.setListener(object: BTListAdapter.BTListClickListener {
                override fun onClick(device: BluetoothDevice) {
                    Toast.makeText(this@SampleActivity, "Connecting..", Toast.LENGTH_SHORT).show()
                    btHelper.connect(device.address)
                }
            })
            binding.listView.adapter = deviceListAdapter
        }
        deviceListAdapter.notifyDataSetChanged()
    }

    private fun detailFragment(serviceList: List<BluetoothGattService>) {
        binding.run {
            clDetail.visibility = View.VISIBLE
            tvUUID.visibility = View.GONE
            detailAdapter = BTDetailAdapter(this@SampleActivity, serviceList)
            detailAdapter.setListener(object: BTDetailAdapter.BTListClickListener {
                override fun onSend(service: BluetoothGattService, characteristic: BluetoothGattCharacteristic) {
                    if (!checkPermission()) return
                    binding.clInput.visibility = View.VISIBLE

                    binding.btnSend.setOnClickListener {
                        if (!btHelper.isBTOpen) return@setOnClickListener

                        //因輸入沒有 byteArray 所以皆以 hex表示，若有需要再自行更改。
                        val type = when (binding.rgType.checkedRadioButtonId) {
                            binding.rbHex.id, binding.rbByteArray.id -> BTHelper.DataType.Hex
                            else -> BTHelper.DataType.String
                        }

                        //btHelper.send(service.uuid, characteristic.uuid, binding.edInput.text.toString(), type)
                        btHelper.sendByCharacteristic(characteristic, binding.edInput.text.toString(), type)
                        binding.edInput.setText("")
                        binding.clInput.visibility = View.GONE

                        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(binding.edInput.windowToken, 0)
                    }

                    binding.btnCancel.setOnClickListener {
                        if (!btHelper.isBTOpen) return@setOnClickListener
                        binding.edInput.setText("")
                        binding.clInput.visibility = View.GONE

                        val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                        inputMethodManager.hideSoftInputFromWindow(binding.edInput.windowToken, 0)
                    }
                }

                override fun onGet(service: BluetoothGattService, characteristic: BluetoothGattCharacteristic) {
                    //btHelper.descriptorChannel(service.uuid, characteristic.uuid)
                    btHelper.descriptorChannelByCharacteristic(characteristic)
                }
            })
            lvDetail.adapter = detailAdapter
            detailAdapter.notifyDataSetChanged()
        }
    }

    private fun btStatusChange(isOpen: Boolean) {
        binding.clBTNotOpen.visibility = if (isOpen) View.GONE else View.VISIBLE
        if (!isOpen && ::deviceListAdapter.isInitialized) {
            devices.clear()
            selectedDevices.clear()
            deviceListAdapter.clear()
        }
    }

    private fun checkBT(): Boolean {
        if (!::btHelper.isInitialized) initBTHelper()
        return if (!btHelper.isBTOpen) {
            btHelper.openBT()
            false
        } else true
    }

    fun checkPermission(): Boolean {
        if (!checkAndRequestPermission(Manifest.permission.ACCESS_COARSE_LOCATION, 0)) return false
        if (!checkAndRequestPermission(Manifest.permission.BLUETOOTH_CONNECT, 1)) return false
        if (!checkAndRequestPermission(Manifest.permission.BLUETOOTH_SCAN, 2)) return false
        return true
    }

    fun checkAndRequestPermission(permission: String, TAG: Int): Boolean {
        return if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array<String>(1) { permission }, TAG)
            false
        } else true
    }
}