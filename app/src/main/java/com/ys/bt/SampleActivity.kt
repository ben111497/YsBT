package com.orange.obd.test

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlertDialog
import android.app.Dialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.orange.obd.test.databinding.ActivitySampleBinding
import com.orange.obd.test.utils.DialogController
import com.orange.obd.test.utils.SqlController
import com.orange.tpms.adapter.CommandAddAdapter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.UUID

class SampleActivity : AppCompatActivity(), BTCallBack {
    private lateinit var binding: ActivitySampleBinding
    private lateinit var deviceListAdapter: BTListAdapter
    private lateinit var detailAdapter: BTDetailAdapter
    private lateinit var btHelper: BTHelper
    private val devices = HashSet<BluetoothDevice>()
    private val selectedDevices = ArrayList<BluetoothDevice>()

    private var rxChannel = "00008D81-0000-1000-8000-00805F9B34FB"
    private var txChannel = "00008D82-0000-1000-8000-00805F9B34FB"
    private var rxCharacteristic: BluetoothGattCharacteristic? = null
    private var txCharacteristic: BluetoothGattCharacteristic? = null

    private val db = SqlController(this)
    private var adapter: CommandAddAdapter? = null
    private var addCmdList = ArrayList<MutableMap<String, Any>>()

    override fun onRequestPermission(list: ArrayList<String>) { checkAndRequestPermission(list[0], 0) }

    @SuppressLint("MissingPermission")
    override fun onScanDeviceResult(device: BluetoothDevice, scanRecord: Binary, rssi: Int) {
        if (device.name.lowercase().contains("obd")) {
            devices.add(device)
        }
    }

    override fun onStatusChange(status: Int) {
        when (status) {
            BluetoothAdapter.STATE_OFF -> { btStatusChange(false) }
            BluetoothAdapter.STATE_TURNING_OFF -> {}
            BluetoothAdapter.STATE_ON -> { btStatusChange(true) }
            BluetoothAdapter.STATE_TURNING_ON -> {}
        }
    }

    @SuppressLint("SetTextI18n")
    override fun rx(uuid: String, value: ByteArray?) {
        if (value == null) return
        binding.svLog.post {
            val txt = binding.tvLog.text.toString() + "\n${Date().format()} rx: ${Binary(value).toHEX()}"
            binding.tvLog.text = txt
            binding.svLog.fullScroll(View.FOCUS_DOWN)
        }
    }

    @SuppressLint("SetTextI18n")
    override fun tx(uuid: String, value: ByteArray?) {
        if (value == null) return
        binding.svLog.post {
            val txt = binding.tvLog.text.toString() + "\n${Date().format()} tx: ${Binary(value).toHEX()}"
            binding.tvLog.text = txt
            binding.svLog.fullScroll(View.FOCUS_DOWN)
        }
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
        db.createAndCheckTable()
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
                edTx.setText("")
                tvLog.text = ""
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
                @SuppressLint("MissingPermission")
                override fun onClick(device: BluetoothDevice) {
                    Toast.makeText(this@SampleActivity, "Connecting..", Toast.LENGTH_SHORT).show()
                    btHelper.connect(device.address)
                    binding.tvName.text = device.name.toString()
                    binding.tvMac.text = device.address
                }
            })
            binding.listView.adapter = deviceListAdapter
        }
        deviceListAdapter.notifyDataSetChanged()
    }

    @SuppressLint("SetTextI18n")
    private fun detailFragment(serviceList: List<BluetoothGattService>) {
        binding.run {
            clDetail.visibility = View.VISIBLE

            serviceList.find { it.uuid == UUID.fromString("00007722-0000-1000-8000-00805f9b34fb") }?.let {
                it.characteristics?.find { it.uuid == UUID.fromString(rxChannel) }?.let { x ->
                    rxCharacteristic = x
                    btHelper.descriptorChannelByCharacteristic(x)
                }

                it.characteristics?.find { it.uuid == UUID.fromString(txChannel) }?.let { x ->
                    txCharacteristic = x
                }
            }

            binding.edTx.addTextChangedListener(object : TextWatcher {
                private var isFormatting = false
                private var lastFormatted = ""
                private var isCenterSpace = false
                private var lastCursor = 0

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {
                    if (isFormatting) return
                    lastFormatted = s.toString()
                    lastCursor = binding.edTx.selectionStart
                    isCenterSpace = s.subSequence(start, start + count).toString() == " "
                }

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {
                }

                override fun afterTextChanged(s: Editable) {
                    Log.e(".test", "s.toString(): ${s.toString()}")
                    if (isFormatting) {
                        isFormatting = false
                        return
                    }

                    val raw = s.toString().replace(" ", "")
                    val formatted = raw.chunked(2).joinToString(" ")

                    if (formatted != s.toString()) {
                        val diff = formatted.length - (lastFormatted.length + if (isCenterSpace) 1 else 0)
                        isFormatting = true
                        s.replace(0, s.length, formatted)

                        val newPos = (lastCursor + diff).coerceIn(0, formatted.length)
                        binding.edTx.setSelection(newPos)
                    }
                }
            })


            btnSend.setOnClickListener {
                if (!btHelper.isBTOpen) return@setOnClickListener
                val data = edTx.text.toString().trim().replace(" ", "")
                if (data.isEmpty()) return@setOnClickListener
                val type = BTHelper.DataType.Hex
                btHelper.sendByCharacteristic(
                    txCharacteristic ?: return@setOnClickListener,
                    data,
                    type
                )
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(edTx.windowToken, 0)
            }

            btnClearCmd.setOnClickListener { tvLog.text = "" }

            btnSaveCmd.setOnClickListener {
                DialogController.showHint(this@SampleActivity, { it, dialog ->
                    addCmdList = db.getAddCommand()
                    Log.e(".obd", "addCmdList: ${addCmdList}")
                    adapter = CommandAddAdapter(addCmdList)
                    adapter?.l = object: CommandAddAdapter.Listener {
                        override fun remove(id: String) {
                            db.deleteAddCommand(id)
                            Log.e(".obd", "Remove: ${id}")
                            addCmdList.removeIf { it["id"].toString() == id }
                            runOnUiThread { adapter?.notifyDataSetChanged() }
                        }

                        override fun click(command: String) {
                            binding.edTx.setText(command)
                            dialog.dismiss()
                        }
                    }
                    it.lv.adapter = adapter
                    adapter?.notifyDataSetChanged()
                })
            }

            btnAddCmd.setOnClickListener {
                if (edTx.text.toString().trim().replace(" ", "").isEmpty()) return@setOnClickListener
                if (addCmdList.any { it["command"].toString() == edTx.text.toString().trim().replace(" ", "") }) return@setOnClickListener
                Log.e(".obd", "增加command: ${edTx.text.toString().trim()}")
                db.addCommand(edTx.text.toString().trim().replace(" ", ""))
                addCmdList = db.getAddCommand()
            }

            tvLog.apply {
                isLongClickable = true
                setTextIsSelectable(true)
            }

            tvLog.setOnLongClickListener {
                val text = tvLog.text.toString()
                if (text.isNotEmpty()) {
                    val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clip = ClipData.newPlainText("log", text)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(this@SampleActivity, "已複製", Toast.LENGTH_SHORT).show()
                }
                true
            }
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

    fun Date.format(): String = SimpleDateFormat("yyyy-MM-dd HH:mm:ss:SSS").format(this)
}