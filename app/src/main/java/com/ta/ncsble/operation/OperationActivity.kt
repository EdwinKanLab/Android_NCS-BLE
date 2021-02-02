package com.ta.ncsble.operation

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.os.Bundle
import android.view.KeyEvent
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.clj.fastble.BleManager
import com.clj.fastble.data.BleDevice
import com.ta.ncsble.R
import com.ta.ncsble.comm.Observer
import com.ta.ncsble.comm.ObserverManager
import java.util.*


class OperationActivity : AppCompatActivity(), Observer {
    var bleDevice: BleDevice? = null
        private set
    var bluetoothGattService: BluetoothGattService? = null
    var characteristic: BluetoothGattCharacteristic? = null
    var charaProp = 0
    private var toolbar: Toolbar? = null
//    private val fragments: MutableList<Fragment> =
//        ArrayList()
    private var currentPage = 0
    private var titles = arrayOfNulls<String>(3)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_operation)
        initData()
        initView()
        initPage()
        ObserverManager.getInstance()?.addObserver(this)
    }

    override fun onDestroy() {
        super.onDestroy()
        BleManager.getInstance().clearCharacterCallback(bleDevice)
        ObserverManager.getInstance()?.deleteObserver(this)
    }

    override fun disConnected(device: BleDevice?) {
        if (device != null && bleDevice != null && device.key == bleDevice!!.key) {
            finish()
        }
    }

    override fun connectionLost(bleDevice: BleDevice?) {
        Log.i("Exception", "///////////////Connection lost, trying to reconnect.")
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        return if (keyCode == KeyEvent.KEYCODE_BACK) {
            if (currentPage != 0) {
                currentPage--
                changePage(currentPage)
                true
            } else {
                finish()
                true
            }
        } else super.onKeyDown(keyCode, event)
    }

    private fun initView() {
        toolbar = findViewById(R.id.toolbar) as Toolbar
        toolbar!!.title = titles[0]
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        toolbar!!.setNavigationOnClickListener {
            if (currentPage != 0) {
                currentPage--
                changePage(currentPage)
            } else {
                finish()
            }
        }
    }

    private fun initData() {
        bleDevice = intent.getParcelableExtra(KEY_DATA)
        if (bleDevice == null) finish()
        titles = arrayOf(
            getString(R.string.service_list),
            getString(R.string.characteristic_list),
            getString(R.string.console)
        )
    }

    private fun initPage() {
        prepareFragment()
        changePage(0)
    }

//    fun changePage(page: Int) {
//        currentPage = page
//        toolbar!!.title = titles[page]
//        updateFragment(page)
//        if (currentPage == 1) {
//            (fragments[1] as CharacteristicListFragment).showData()
//        } else if (currentPage == 2) {
//            (fragments[2] as CharacteristicOperationFragment).showData()
//        }
//    }
fun changePage(page: Int) {
}

//    private fun prepareFragment() {
//        fragments.add(ServiceListFragment())
//        fragments.add(CharacteristicListFragment())
//        fragments.add(CharacteristicOperationFragment())
//        for (fragment in fragments) {
//            supportFragmentManager.beginTransaction().add(R.id.fragment, fragment)
//                .hide(fragment).commit()
//        }
//    }
private fun prepareFragment() {
}


//    private fun updateFragment(position: Int) {
//        if (position > fragments.size - 1) {
//            return
//        }
//        for (i in fragments.indices) {
//            val transaction = supportFragmentManager.beginTransaction()
//            val fragment = fragments[i]
//            if (i == position) {
//                transaction.show(fragment)
//            } else {
//                transaction.hide(fragment)
//            }
//            transaction.commit()
//        }
//    }
    private fun updateFragment(position: Int) {
    }

    companion object {
        const val KEY_DATA = "key_data"
    }
}

