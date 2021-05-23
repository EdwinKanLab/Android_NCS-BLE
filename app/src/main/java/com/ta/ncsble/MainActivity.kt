package com.ta.ncsble


import android.Manifest
import android.app.ProgressDialog
import android.bluetooth.BluetoothAdapter
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import android.view.animation.LinearInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.work.*
import com.clj.fastble.BleManager
import com.clj.fastble.callback.*
import com.clj.fastble.data.BleDevice
import com.clj.fastble.scan.BleScanRuleConfig
import com.ta.ncsble.Constants.UUID_SERVICE
import com.ta.ncsble.adapter.DeviceAdapter
import com.ta.ncsble.workers.CompressorWorker
import com.ta.ncsble.workers.UploaderWorker
import java.util.*
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), View.OnClickListener {

    private val TAG = MainActivity::class.java.simpleName
    private val REQUEST_CODE_OPEN_GPS = 1
    private val REQUEST_CODE_PERMISSION_LOCATION = 2

    private var btn_scan: Button? = null
    private var img_loading: ImageView? = null

    private var operatingAnim: Animation? = null
    private var mDeviceAdapter: DeviceAdapter? = null
    private var progressDialog: ProgressDialog? = null

    private var ncsDirectory : String = ""

    private var mShouldUnbind = false

    private var mService: RecordingService? = null
    private var myBleDevice : BleDevice? = null

    /** Defines callbacks for service binding, passed to bindService()  */
    private val connection = object : ServiceConnection {

        override fun onServiceConnected(className: ComponentName, service: IBinder) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            Log.i(
                TAG,
                "////////////////////////////////////////////////////////////////////////////////service connected"
            )
            val binder = service as RecordingService.LocalBinder
            mService = binder.getService()
            if (mService == null){
                Log.i(
                    TAG,
                    "////////////////////////////////////////////////////////////////////////////////mservice null"
                )
            }
            mService!!.setState(
                        myBleDevice!!, ncsDirectory, progressDialog, mDeviceAdapter!!,
                        img_loading, btn_scan, operatingAnim
                    )
            mService!!.connectBLE()
        }
        override fun onServiceDisconnected(arg0: ComponentName) {
            Log.i(
                TAG,
                "////////////////////////////////////////////////////////////////////////////////service disconnected"
            )
            mShouldUnbind = false
        }
    }

//    private fun updateState(){
//        var newConnection = false
//        //Try connecting to service if not bound
//        if (){
//            newConnection = setupService()
//        }
//        if (newConnection){
//            if (mService!!.getState()){
//                btn_record!!.text = getString(R.string.stop_record)
//                btn_record!!.background = getDrawable(R.drawable.norecord_btn)
//            }
//        }
//
//    }

    private fun doBindService() {
        // Attempts to establish a connection with the service.  We use an
        // explicit class name because we want a specific service
        // implementation that we know will be running in our own process
        // (and thus won't be supporting component replacement by other
        // applications).
        if (bindService(
                Intent(this, RecordingService::class.java),
                connection, Context.BIND_AUTO_CREATE
            )
        ) {
            Log.i(TAG, "//////////////////////////////////////////Service bound")
            mShouldUnbind = true
        } else {
            Log.i(
                TAG, "Error: The requested service doesn't " +
                        "exist, or this client isn't allowed access to it."
            )
        }
    }

    private fun doUnbindService() {
        if (mShouldUnbind) {
            // Release information about the service's state.
            unbindService(connection)
            mShouldUnbind = false
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initView()
        ncsDirectory = getExternalFilesDir(null)!!.absolutePath

        buildAndRunWorkers()

        BleManager.getInstance().init(application)
        BleManager.getInstance()
            .enableLog(true)
            .setReConnectCount(0, 5000)
            .setConnectOverTime(20000).operateTimeout = 5000
    }

    override fun onResume() {
        super.onResume()
        showConnectedDevice()
    }

    override fun onDestroy() {
        super.onDestroy()
        BleManager.getInstance().disconnectAllDevice()
        BleManager.getInstance().destroy()
        if (mShouldUnbind){
            Log.i(TAG, "/////////////////Unbinding from service")
            doUnbindService()
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_scan -> if (btn_scan!!.text == getString(R.string.start_scan)) {
                checkPermissions()
            } else if (btn_scan!!.text == getString(R.string.stop_scan)) {
                BleManager.getInstance().cancelScan()
            }
        }
    }

    private fun initView() {
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        btn_scan = findViewById<Button>(R.id.btn_scan)
        btn_scan!!.text = getString(R.string.start_scan)
        btn_scan!!.setOnClickListener(this)

        img_loading = findViewById<ImageView>(R.id.img_loading)
        operatingAnim = AnimationUtils.loadAnimation(this, R.anim.rotate)
        operatingAnim!!.interpolator = LinearInterpolator()
        progressDialog = ProgressDialog(this)
        mDeviceAdapter = DeviceAdapter(this)
        mDeviceAdapter!!.setOnDeviceClickListener(object : DeviceAdapter.OnDeviceClickListener() {
            override fun onConnect(bleDevice: BleDevice?) {
                if (!BleManager.getInstance().isConnected(bleDevice)) {
                    BleManager.getInstance().cancelScan()
                    myBleDevice = bleDevice!!
                    setupService()
                }
            }

            override fun onDisConnect(bleDevice: BleDevice?) {
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    doUnbindService()
                    BleManager.getInstance().disconnect(bleDevice)
                }
            }

            override fun onRecord(bleDevice: BleDevice?) {
                if (BleManager.getInstance().isConnected(bleDevice)) {
                    val btn_record = findViewById<Button>(R.id.btn_record)
                    if (btn_record!!.text == getString(R.string.start_record)) {
                        mService!!.startRecord(bleDevice)
                    } else if (btn_record!!.text == getString(R.string.stop_record)) {
                        mService!!.stopRecord(bleDevice)
                    }
                }
            }
        })
        val listViewDevice = findViewById<ListView>(R.id.list_device)
        listViewDevice.adapter = mDeviceAdapter
    }


    private fun showConnectedDevice() {
        val deviceList =
            BleManager.getInstance().allConnectedDevice
        mDeviceAdapter!!.clearConnectedDevice()
        for (bleDevice in deviceList) {
            mDeviceAdapter!!.addDevice(bleDevice)
        }
        mDeviceAdapter!!.notifyDataSetChanged()
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
        setServiceUuids(serviceUuids).build()
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
                mDeviceAdapter!!.addDevice(bleDevice)
                mDeviceAdapter!!.notifyDataSetChanged()
            }

            override fun onScanFinished(scanResultList: List<BleDevice>) {
                img_loading!!.clearAnimation()
                img_loading!!.visibility = View.INVISIBLE
                btn_scan!!.text = getString(R.string.start_scan)
            }
        })
    }

    private fun setupService(){
        doUnbindService()
        val intent = Intent(this, RecordingService::class.java)
//        startService(intent)
        Log.i(TAG, "///////////////////////////////////////Setting up service")
        startForegroundService(intent)
        doBindService()
//        while (mService == null){
//            Log.i(TAG, "/////////////////Waiting to bind")
//        }

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String?>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSION_LOCATION -> if (grantResults.isNotEmpty()) {
                var i = 0
                while (i < grantResults.size) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) {
                        onPermissionGranted(permissions[i])
                    }
                    i++
                }
            }
        }
    }

    private fun checkPermissions() {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        if (!bluetoothAdapter.isEnabled) {
            Toast.makeText(this, getString(R.string.please_open_blue), Toast.LENGTH_LONG).show()
            return
        }
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.ACCESS_WIFI_STATE,
            Manifest.permission.READ_EXTERNAL_STORAGE
        )
        val permissionDeniedList: MutableList<String> =
            ArrayList()
        for (permission in permissions) {
            val permissionCheck: Int = ContextCompat.checkSelfPermission(this, permission)
            if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted(permission)
            } else {
                permissionDeniedList.add(permission)
            }
        }
        if (permissionDeniedList.isNotEmpty()) {
            val deniedPermissions =
                permissionDeniedList.toTypedArray()
            ActivityCompat.requestPermissions(
                this,
                deniedPermissions,
                REQUEST_CODE_PERMISSION_LOCATION
            )
        }
    }

    private fun onPermissionGranted(permission: String?) {
        when (permission) {
            Manifest.permission.ACCESS_FINE_LOCATION -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !checkGPSIsOpen()) {
                AlertDialog.Builder(this)
                    .setTitle(R.string.notifyTitle)
                    .setMessage(R.string.gpsNotifyMsg)
                    .setNegativeButton(
                        R.string.cancel
                    ) { dialog, which -> finish() }
                    .setPositiveButton(
                        R.string.setting
                    ) { dialog, which ->
                        val intent =
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        startActivityForResult(intent, REQUEST_CODE_OPEN_GPS)
                    }
                    .setCancelable(false)
                    .show()
            } else {
                setScanRule()
                startScan()
            }
        }
    }

    private fun checkGPSIsOpen(): Boolean {
        val locationManager =
            this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
                ?: return false
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OPEN_GPS) {
            if (checkGPSIsOpen()) {
                setScanRule()
                startScan()
            }
        }
    }

    fun buildAndRunWorkers(){
        val compressorBuilder =
            PeriodicWorkRequestBuilder<CompressorWorker>(17, TimeUnit.MINUTES)


        val compressorWork =
            compressorBuilder
                .setInputData(Data.Builder().putString("ncsDirectory", ncsDirectory).build())
                .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                Constants.COMPRESSION_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE, compressorWork
            )

        val uploaderBuilder =
            PeriodicWorkRequestBuilder<UploaderWorker>(23, TimeUnit.MINUTES)

        val uploaderConstraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.UNMETERED)
            .build()

        val uploaderWork =
            uploaderBuilder
                .setInputData(Data.Builder().putString("ncsDirectory", ncsDirectory).build())
                .setConstraints(uploaderConstraints)
                .build()

        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork(
                Constants.UPLOADER_WORK_NAME,
                ExistingPeriodicWorkPolicy.REPLACE, uploaderWork
            )

    }



}