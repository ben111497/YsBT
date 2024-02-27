package com.ys.bt

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.ContentValues
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ys.bt.databinding.ActivitySampleBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Timer
import java.util.TimerTask
import kotlin.collections.ArrayList
import kotlin.math.abs

class SampleActivity : AppCompatActivity(), BTCallBack {
    private lateinit var binding: ActivitySampleBinding
    private lateinit var adapter: BTSpecifyAdapter
    private lateinit var btHelper: BTHelper
    private var dataList = ArrayList<BTSpecifyAdapter.BTData>()
    private var specifyMac = ""
    private var timeTag = System.currentTimeMillis()
    private var log = ""
    private val savePerMinute = 1
    private val maxLog = 300
    private var timer: Timer = Timer()
    private var originSize = 0
    private val textWatcher = object : TextWatcher {
        override fun beforeTextChanged(charSequence: CharSequence?, start: Int, count: Int, after: Int) {
            originSize = binding.edSearch.text.length
        }

        override fun onTextChanged(charSequence: CharSequence?, start: Int, before: Int, count: Int) {}

        override fun afterTextChanged(editable: Editable?) {
            //formatBluetoothMac(binding.edSearch)
        }
    }

    override fun onRequestPermission(list: ArrayList<String>) { checkAndRequestPermission(list[0], 0) }

    override fun onScanDeviceResult(device: BluetoothDevice, scanRecord: Binary, rssi: Int) {
        if (device.address.uppercase().contains(specifyMac.uppercase())) {
            val res = scanRecord.toHEX().replace("0x", "").substring(0, 60)
            var data = ""
            for (i in res.indices step 2) {
                data += "0x${res.substring(i, i + 2)} "
            }

            val date = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(Date())
            dataList.add(BTSpecifyAdapter.BTData(device.address, date, data))
            if (dataList.size > maxLog) {
                val halfSize = dataList.size / 2
                dataList = ArrayList(dataList.subList(halfSize, dataList.size))
            }

            log += "$date\n${device.address} -> \n$data\n\n"
            Log.e("scanBack", "device: ${device.address}, byteArray: $data")

            val current = System.currentTimeMillis()
            try {
                if (abs(current - timeTag) > savePerMinute * 60 * 1000) {
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "Download/BT_Log")
                        put(MediaStore.Images.Media.DISPLAY_NAME, "BT_Log_${SimpleDateFormat("yyyy-MM-dd_HH.mm.ss").format(Date(timeTag))}.txt")
                        put(MediaStore.Files.FileColumns.MIME_TYPE, "text/plain")
                    }

                    val resolver = contentResolver
                    val uri: Uri? = resolver.insert(MediaStore.Files.getContentUri("external"), values)

                    if (uri != null) {
                        resolver.openOutputStream(uri)?.use { outputStream ->
                            outputStream.write(log.toByteArray())
                        }
                    }

                    timeTag = current
                    log = ""
                    Log.e("sys:", "local save succeed, path: ${uri?.path}")
                    runOnUiThread { Toast.makeText(this, "local save succeed", Toast.LENGTH_SHORT).show() }
                }
            } catch (e: Exception) {
                timeTag = current
                Log.e("sys:", "local save failed: ${e.message}")
            }
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

    override fun rx(uuid: String, value: ByteArray?) {
        //TODO("Not yet implemented")
    }

    override fun tx(uuid: String, value: ByteArray?) {
        //TODO("Not yet implemented")
    }

    override fun onConnectionStateChange(isConnect: Boolean, serviceList: List<BluetoothGattService>?) {}

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
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(binding.root)
        init()
        setListener()
    }

    private fun init() {
        if (checkAndRequestPermission(Manifest.permission.BLUETOOTH_CONNECT, 1) &&
            checkAndRequestPermission(Manifest.permission.BLUETOOTH_SCAN, 2) && checkAndRequestPermission(Manifest.permission.BLUETOOTH_SCAN, 3)) {
            initBTHelper()
        }

        if (!::adapter.isInitialized) {
            adapter = BTSpecifyAdapter(this, dataList)
            binding.listView.adapter = adapter
        }
    }

    private fun initBTHelper() {
        btHelper = BTHelper(this, this)
        btStatusChange(btHelper.isBTOpen)
    }

    private fun setListener() {
        binding.run {
            btnScan.setOnClickListener {
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.edSearch.windowToken, 0)
                if (!checkBT()) return@setOnClickListener
                btHelper.scanDevice()
                specifyMac = binding.edSearch.text.toString()

                timer.schedule(object : TimerTask() {
                    override fun run() {
                        runOnUiThread {
                            if (::adapter.isInitialized) {
                                adapter.notifyDataSetChanged()
                                //binding.listView.smoothScrollToPosition( binding.listView.count - 1)
                            }
                        }
                    }
                }, 0, if (specifyMac.isEmpty()) 2000 else 250)
            }

            btStop.setOnClickListener {
                val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                inputMethodManager.hideSoftInputFromWindow(binding.edSearch.windowToken, 0)
                if (!checkBT()) return@setOnClickListener
                btHelper.stopScanDevice()
                timer.purge()
            }

            edSearch.addTextChangedListener(textWatcher)
        }
    }

    private fun btStatusChange(isOpen: Boolean) {
        binding.clBTNotOpen.visibility = if (isOpen) View.GONE else View.VISIBLE
    }

    private fun checkBT(): Boolean {
        if (!::btHelper.isInitialized) initBTHelper()
        return if (!btHelper.isBTOpen) {
            btHelper.openBT()
            false
        } else true
    }

    fun checkAndRequestPermission(permission: String, TAG: Int): Boolean {
        return if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, Array<String>(1) { permission }, TAG)
            false
        } else true
    }

    fun askPermission(): Boolean {
        return if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            true
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), 123)
            false
        }
    }

    @SuppressLint("SetTextI18n")
    private fun formatBluetoothMac(ed: EditText) {
        ed.removeTextChangedListener(textWatcher)

        ed.setText(ed.text.replace(Regex("[^0-9A-Fa-f:]"), ""))
        if (originSize == 1 && ed.text.length == 2) {
            ed.setText(ed.text.toString() + ":")
        }

        when (originSize to ed.text.length) {
            1 to 2 -> ed.setText(ed.text.toString() + ":")
            4 to 5 -> ed.setText(ed.text.toString() + ":")
            7 to 8 -> ed.setText(ed.text.toString() + ":")
            10 to 11 -> ed.setText(ed.text.toString() + ":")
            13 to 14 -> ed.setText(ed.text.toString() + ":")
        }

        if (ed.text.length > 16) {
            ed.setText(ed.text.toString().substring(0, 17))
        }

        ed.setSelection(ed.text.length)

        ed.addTextChangedListener(textWatcher)
    }
}