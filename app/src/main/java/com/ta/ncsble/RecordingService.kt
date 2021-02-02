package com.ta.ncsble

import android.app.*
import android.app.NotificationManager.IMPORTANCE_LOW
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.clj.fastble.BleManager
import com.clj.fastble.callback.BleGattCallback
import com.clj.fastble.callback.BleMtuChangedCallback
import com.clj.fastble.callback.BleNotifyCallback
import com.clj.fastble.callback.BleScanCallback
import com.clj.fastble.data.BleDevice
import com.clj.fastble.exception.BleException
import com.clj.fastble.scan.BleScanRuleConfig
import com.ta.ncsble.Constants.NUM_RETRIES
import com.ta.ncsble.Constants.RECORDER_NOTIFICATION_CHANNEL_ID
import com.ta.ncsble.Constants.RECORDER_NOTIFICATION_CHANNEL_NAME
import com.ta.ncsble.Constants.RECORDER_NOTIFICATION_ID
import com.ta.ncsble.Constants.UUID_CHARACTERISTIC_NOTIFY
import com.ta.ncsble.Constants.UUID_SERVICE
import com.ta.ncsble.adapter.DeviceAdapter
import com.ta.ncsble.comm.ObserverManager
import kotlinx.coroutines.delay
import java.util.*

class RecordingService : Service() {
    private var myBleDevice: BleDevice? = null
    private val TAG = RecordingService::class.java.simpleName
    private var myRecorder : Recorder = Recorder("", "")
    private var ncsDirectory : String? = ""
    private var isFirst = true
    private val binder = LocalBinder()

    private var isRecording = false
    private var isConnected = false
    private var progressDialog: ProgressDialog? = null
    private var mDeviceAdapter: DeviceAdapter? = null
    private var img_loading: ImageView? = null
    private var btn_scan: Button? = null
    private var numRetries = NUM_RETRIES
    private var operatingAnim: Animation? = null



    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
//        return super.onStartCommand(intent, flags, startId)
        if (intent != null){
            firstSetup()
        }
        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return binder
    }

    inner class LocalBinder : Binder() {
        // Return this instance of LocalService so clients can call public methods
        fun getService(): RecordingService = this@RecordingService
    }


    fun setState(
        bleDevice: BleDevice?,
        ncsDir: String,
        pdialog: ProgressDialog?,
        deviceAdapter: DeviceAdapter,
        imgLoading: ImageView?,
        btnScan: Button?,
        opAnim: Animation?
    ){
        myBleDevice = bleDevice
        ncsDirectory = ncsDir
        progressDialog = pdialog
        mDeviceAdapter = deviceAdapter
        img_loading = imgLoading
        btn_scan = btnScan
        operatingAnim = opAnim
    }

    fun connectBLE() {
        Log.i(TAG, "///////////////////////////////////Trying to connect BLE.")
        BleManager.getInstance().connect(myBleDevice, object : BleGattCallback() {
            override fun onStartConnect() {
                progressDialog!!.show()
            }

            override fun onConnectFail(bleDevice: BleDevice, exception: BleException) {
                img_loading!!.clearAnimation()
                img_loading!!.visibility = View.INVISIBLE
                btn_scan!!.text = getString(R.string.start_scan)
                progressDialog!!.dismiss()
                Toast.makeText(
                    this@RecordingService,
                    getString(R.string.connect_fail),
                    Toast.LENGTH_LONG
                ).show()
                retryConnection()

            }

            override fun onConnectSuccess(
                bleDevice: BleDevice,
                gatt: BluetoothGatt,
                status: Int
            ) {
                Log.i(
                    TAG,
                    "////////////////////////////////////////////////////////////////////////////////device connected"
                )
                progressDialog!!.dismiss()
                mDeviceAdapter!!.addDevice(bleDevice)
                mDeviceAdapter!!.notifyDataSetChanged()
                setMtu(bleDevice, 223)
                requestConnectionPriority(bleDevice, BluetoothGatt.CONNECTION_PRIORITY_HIGH)
                isConnected = true
                numRetries = NUM_RETRIES

                if (isRecording) {
                    isRecording = false
                    mDeviceAdapter!!.bleRecording = null
                    Thread.sleep(1000)
                    startRecord(bleDevice)
                }
            }

            override fun onDisConnected(
                isActiveDisConnected: Boolean,
                bleDevice: BleDevice,
                gatt: BluetoothGatt,
                status: Int
            ) {
                progressDialog!!.dismiss()
                mDeviceAdapter!!.removeDevice(bleDevice)
                mDeviceAdapter!!.notifyDataSetChanged()

                if (isRecording) {
                    myRecorder.stopRecord()
                }

                isConnected = false
                if (isActiveDisConnected) {
                    Toast.makeText(
                        this@RecordingService,
                        getString(R.string.active_disconnected),
                        Toast.LENGTH_LONG
                    ).show()
                    isRecording = false
                    stopForegroundService()

                } else {
                    Toast.makeText(
                        this@RecordingService,
                        getString(R.string.disconnected),
                        Toast.LENGTH_LONG
                    ).show()
                    ObserverManager.getInstance()!!.notifyObserver(bleDevice)
                    retryConnection()

                }
            }
        })
//        while (!isConnected){
//            Log.i(TAG, "/////////////////Waiting to connect")
//        }
//        return true
    }

    fun startRecord(bleDevice: BleDevice?) {
        if (bleDevice == null || myBleDevice == null) return
        if (bleDevice.mac != myBleDevice!!.mac) return
        if (!isRecording) {
            myRecorder = Recorder(ncsDirectory!!, myBleDevice!!.mac)
            BleManager.getInstance().notify(
                myBleDevice,
                UUID_SERVICE,
                UUID_CHARACTERISTIC_NOTIFY,
                recordCallback
            )
        }
    }

    fun stopRecord(bleDevice: BleDevice?){
        if (bleDevice == null || myBleDevice == null) return
        if (bleDevice.mac != myBleDevice!!.mac) return
        BleManager.getInstance().stopNotify(
            myBleDevice,
            UUID_SERVICE,
            UUID_CHARACTERISTIC_NOTIFY
        )
        myRecorder.stopRecord()
        isRecording = false
        mDeviceAdapter!!.bleRecording = null
        mDeviceAdapter!!.notifyDataSetChanged()
    }

    private fun firstSetup(){
        if(isFirst) {
            startForegroundService()
            isFirst = false

        }
    }

    fun getState() : Boolean{
        return isRecording

    }

    private fun stopForegroundService(){
        stopForeground(true)
        stopSelf()
    }

    private fun startForegroundService() {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE)
                as NotificationManager

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        val notificationBuilder = NotificationCompat
            .Builder(this, RECORDER_NOTIFICATION_CHANNEL_ID)
            .setAutoCancel(false)
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_blue_connected)
            .setContentTitle("NCS BLE")
            .setContentText("Connected")
            .setContentIntent(getMainActivityPendingIntent())

        startForeground(RECORDER_NOTIFICATION_ID, notificationBuilder.build())
    }
    private fun getMainActivityPendingIntent() = PendingIntent.getActivity(
        this,
        0,
        Intent(this, MainActivity::class.java),
        FLAG_UPDATE_CURRENT
    )

    private fun retryConnection(){
        if (!isRecording || numRetries == 0){
            stopForegroundService()
        }
        else{
            //Retry connection
            setScanRule()
            startScan()

        }

    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(notificationManager: NotificationManager) {
        val channel = NotificationChannel(
            RECORDER_NOTIFICATION_CHANNEL_ID,
            RECORDER_NOTIFICATION_CHANNEL_NAME,
            IMPORTANCE_LOW
        )
        notificationManager.createNotificationChannel(channel)
    }


    private val recordCallback : BleNotifyCallback = object : BleNotifyCallback(){
        override fun onNotifySuccess(){ notifySuccess() }
        override fun onNotifyFailure(exception: BleException){ notifyFailure(exception) }
        override fun onCharacteristicChanged(data: ByteArray?) { characteristicChanged(data) }
    }


    private fun notifySuccess(){
        Log.i(
            TAG,
            "////////////////////////////////////////////////////////////////notifying"
        )
        myRecorder.startRecord()
        isRecording = true
        mDeviceAdapter!!.bleRecording = myBleDevice
        mDeviceAdapter!!.notifyDataSetChanged()

    }
    private fun notifyFailure(exception: BleException){
        Log.i(TAG, "//////////////////////////////NOTIFICATION FAILURE")
        Log.i(
            TAG,
            "////////////////////////////////////////////////////////////////$exception"
        )

    }
    private fun characteristicChanged(data: ByteArray?){
        myRecorder.putData(data!!)

    }

    private fun setMtu(bleDevice: BleDevice, mtu: Int) {
        BleManager.getInstance().setMtu(bleDevice, mtu, object : BleMtuChangedCallback() {
            override fun onSetMTUFailure(exception: BleException) {
                Log.i(
                    TAG,
                    "//////////////////////////////////////////////////////////////onsetMTUFailure$exception"
                )
            }

            override fun onMtuChanged(mtu: Int) {
                Log.i(
                    TAG,
                    "//////////////////////////////////////////////////////////onMtuChanged: $mtu"
                )
            }
        })
    }

    private fun requestConnectionPriority(
        bleDevice: BleDevice,
        connectionPriority: Int
    ): Boolean {
        return BleManager.getInstance().requestConnectionPriority(bleDevice, connectionPriority)
    }


    private fun setScanRule() {
        val uuids: Array<String>? = arrayOf(UUID_SERVICE)
        var serviceUuids: Array<UUID?>? = null
        if (uuids != null && uuids.size > 0) {
            serviceUuids = arrayOfNulls(uuids.size)
            for (i in uuids.indices) {
                val name = uuids[i]
                val components =
                    name.split("-".toRegex()).toTypedArray()
                if (components.size != 5) {
                    serviceUuids[i] = null
                } else {
                    serviceUuids[i] = UUID.fromString(uuids[i])
                }
            }
        }
        val scanRuleConfig = BleScanRuleConfig.Builder().
        setServiceUuids(serviceUuids).
        setDeviceMac(myBleDevice!!.mac).
        setScanTimeOut(20000).build()
        BleManager.getInstance().initScanRule(scanRuleConfig)
    }

    private fun startScan() {
        BleManager.getInstance().scan(object : BleScanCallback() {
            override fun onScanStarted(success: Boolean) {
                mDeviceAdapter!!.clearScanDevice()
                mDeviceAdapter!!.notifyDataSetChanged()
                img_loading!!.startAnimation(operatingAnim)
                img_loading!!.visibility = View.VISIBLE
                btn_scan!!.text = getString(R.string.stop_scan)
            }

            override fun onLeScan(bleDevice: BleDevice) {
                super.onLeScan(bleDevice)
            }

            override fun onScanning(bleDevice: BleDevice) {
//                mDeviceAdapter!!.addDevice(bleDevice)
//                mDeviceAdapter!!.notifyDataSetChanged()
                if (bleDevice.mac ==  myBleDevice!!.mac) connectBLE()
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {
                img_loading!!.clearAnimation()
                img_loading!!.visibility = View.INVISIBLE
                btn_scan!!.text = getString(R.string.start_scan)
                if (!isConnected){
                    numRetries--
                    retryConnection()
                }

            }
        })
    }
}
