# YsBT

包裝好的 Bluetooth模組，供使用者快速使用，並提供 Bluetooth搜尋、連線等其他範例。

## 目錄
- 如何導入到項目
- 使用方法
    - 在需要藍牙的地方繼承 `BTCallBack`，監聽藍芽的狀態
    - **藍牙掃描以及連線**
    - 接收及發送訊息
    - 檢查及開啟藍芽

## 如何導入到項目


> jcenter導入方式
> 

在app專案包的 `build.gradle`中添加

```xml
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

在 module中的 `build.gradle`中的 dependencies 內加入

```
dependencies {
	implementation 'com.github.t109368015:YsBT:v1.1.0'
}
```

## 使用方法

### 在需要藍牙的地方繼承 `BTCallBack`，監聽藍芽的狀態

> Activity範例
> 

```kotlin
class SampleActivity : AppCompatActivity(), BTCallBack {
  private lateinit var btHelper: BTHelper

  override fun onCreate(savedInstanceState: Bundle?) {
      super.onCreate(savedInstanceState)
      setContentView(R.id.activity_main)
      btHelper = BTHelper(this, this)
  }

  override fun onRequestPermission(list: ArrayList<String>) { checkAndRequestPermission(list[0], 0) }

  override fun onScanDeviceResult(device: BluetoothDevice, scanRecord: Binary, rssi: Int) {
       //TODO("Not yet implemented")
  }

  override fun onStatusChange(status: Int) {
      //TODO("Not yet implemented")
  }

  override fun rx(uuid: String, value: ByteArray?) {
      //TODO("Not yet implemented")
  }

  override fun tx(uuid: String, value: ByteArray?) {
      //TODO("Not yet implemented")
  }

  override fun onConnectionStateChange(isConnect: Boolean, serviceList: List<BluetoothGattService>?) {
     //TODO("Not yet implemented")
  }
}
```

### **藍牙掃描以及連線**

- 開始掃描藍芽
    
    ```kotlin
    btHelper.scanDevice()
    ```
    
- 停止掃描藍芽
    
    ```kotlin
    btHelper.stopScanDevice()
    ```
    
- 取的藍芽掃描狀態
    
    ```kotlin
    btHelper.isScanning()
    ```
    
- 取得掃描到的藍芽列表，`onScanDeviceResult`也會將資料回傳，可自行判斷要如何使用。
    
    ```kotlin
    btHelper.getScanDeviceList()
    ```
    
- 與裝置建立連線
    
    address 為搜尋到的藍牙地址，例如：00:12:34:56:78:9A
    
    ```kotlin
    btHelper.connect(address: String)
    ```
    
- 與裝置斷開連縣
    
    ```kotlin
    btHelper.disConnect()
    ```
    
- 取得裝置的服務資料
    
    ```kotlin
    btHelper.getServiceList()
    ```
    

### 接收及發送訊息

- 訂閱頻道
    
    `descriptorChannel(serviceUUID: UUID, characteristicUUID: UUID)`
  
    ***@param*** serviceUUID: UUID
  
    ***@param*** characteristicUUID: UUID
    
    ```kotlin
    btHelper.descriptorChannel(service.uuid, characteristic.uuid)
    ```
    
- 評閱頻道(使用特徵值)
    
    `descriptorChannelByCharacteristic(characteristic: BluetoothGattCharacteristic)`
  
    ***@param*** characteristic: BluetoothGattCharacteristic
    
    ```kotlin
    btHelper.descriptorChannelByCharacteristic(characteristic)
    ```
    
- 發送訊息
    
    send(serviceUUID: UUID, characteristicUUID: UUID, msg: String, type: DataType)
  
    ***@param*** serviceUUID: UUID
  
    ***@param*** characteristicUUID: UUID
  
    ***@param*** msg: String
  
    ***@param*** type: DataType
    
    ```kotlin
    btHelper.send(service.uuid, characteristic.uuid, "123", BTHelper.DataType.String)
    ```
    
- 發送訊息
    
    sendByCharacteristic(characteristic: BluetoothGattCharacteristic, msg: String, type: DataType)
  
    ***@param*** characteristic: BluetoothGattCharacteristic
  
    ***@param*** msg: String
  
    ***@param*** type: DataType
    
    ```kotlin
    btHelper.sendByCharacteristic(characteristic, "123", BTHelper.DataType.String)
    ```
    
- 資料類型
    
    ```kotlin
    enum class DataType {
        Hex, String, ByteArray
    }
    ```
    

### 檢查及開啟藍芽

- 屬性：藍芽是否開啟
    
    ```kotlin
    btHelper.isBTOpen
    ```
    
- 檢查藍芽狀態
    
    ```kotlin
    btHelper.checkBluetoothStatus()
    ```  
    
- 開啟藍芽
    
    ```kotlin
    btHelper.openBT()
    ```
