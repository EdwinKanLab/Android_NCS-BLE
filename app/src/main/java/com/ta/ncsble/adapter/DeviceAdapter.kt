package com.ta.ncsble.adapter


import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.clj.fastble.BleManager
import com.clj.fastble.data.BleDevice
import com.ta.ncsble.MainActivity
import com.ta.ncsble.R
import java.util.*

class DeviceAdapter(private val context: Context) : BaseAdapter() {
    private var bleDeviceList: MutableList<BleDevice> = ArrayList()
    public var bleRecording:BleDevice? = null
    fun addDevice(bleDevice: BleDevice) {
        removeDevice(bleDevice)
        bleDeviceList.add(bleDevice)
    }

    fun removeDevice(bleDevice: BleDevice) {
        if (bleDeviceList.isEmpty()) return
        val ind = bleDeviceList.indexOf(bleDevice);
        if (ind != -1) bleDeviceList.removeAt(ind)
    }

    fun clearConnectedDevice() {
        if (bleDeviceList.isEmpty()) return
        var newBleDeviceList: MutableList<BleDevice> = ArrayList()
        for (i in 0 until bleDeviceList.size) {
            val device = bleDeviceList[i]
            if (!BleManager.getInstance().isConnected(device)) {
                newBleDeviceList.add(device)
            }
        }
        bleDeviceList = newBleDeviceList
    }

    fun clearScanDevice() {
        if (bleDeviceList.isEmpty()) return
        var newBleDeviceList: MutableList<BleDevice> = ArrayList()
        for (i in 0 until bleDeviceList.size) {
            val device = bleDeviceList[i]
            if (BleManager.getInstance().isConnected(device)) {
                newBleDeviceList.add(device)
            }
        }
        bleDeviceList = newBleDeviceList
    }

    fun clear() {
        clearConnectedDevice()
        clearScanDevice()
    }

    override fun getCount(): Int {
        return bleDeviceList.size
    }

    override fun getItem(position: Int): BleDevice? {
        if (position > bleDeviceList.size){
            return null
        } else{
            return bleDeviceList[position]
        }
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(
        position: Int,
        convView: View?,
        parent: ViewGroup
    ): View {
        var holder: ViewHolder
        var convertView = View.inflate(context, R.layout.adapter_device, null)
        if (convView != null){
            holder = convView.tag as DeviceAdapter.ViewHolder
        } else {
            holder = DeviceAdapter.ViewHolder()
            convertView.tag = holder
            holder.img_blue =
                convertView.findViewById<View>(R.id.img_blue) as ImageView
            holder.txt_name =
                convertView.findViewById<View>(R.id.txt_name) as TextView

            holder.layout_idle =
                convertView.findViewById<View>(R.id.layout_idle) as LinearLayout
            holder.layout_connected =
                convertView.findViewById<View>(R.id.layout_connected) as LinearLayout
            holder.btn_disconnect =
                convertView.findViewById<View>(R.id.btn_disconnect) as Button
            holder.btn_connect =
                convertView.findViewById<View>(R.id.btn_connect) as Button
            holder.btn_record =
                convertView.findViewById<View>(R.id.btn_record) as Button
        }

        val bleDevice = getItem(position)
        if (bleDevice != null) {
            val isConnected = BleManager.getInstance().isConnected(bleDevice)
            val name = bleDevice.name
            holder.txt_name!!.text = name
            if (isConnected) {
                holder.img_blue!!.setImageResource(R.drawable.ic_blue_connected)
                holder.txt_name!!.setTextColor(-0xe2164a)
                holder.layout_idle!!.visibility = View.GONE
                holder.layout_connected!!.visibility = View.VISIBLE
                val isRecording = if (bleRecording == null) false else bleDevice.mac == bleRecording!!.mac
                if (isRecording){
                    holder.btn_record!!.text = context.getString(R.string.stop_record)
                    holder.btn_record!!.background = context.getDrawable(R.drawable.norecord_btn)
                }
                else{
                    holder.btn_record!!.text = context.getString(R.string.start_record)
                    holder.btn_record!!.background = context.getDrawable(R.drawable.record_btn)

                }


            } else {
                holder.img_blue!!.setImageResource(R.drawable.ic_blue_remote)
                holder.txt_name!!.setTextColor(-0x1000000)
                holder.layout_idle!!.visibility = View.VISIBLE
                holder.layout_connected!!.visibility = View.GONE
            }
        }
        holder.btn_connect!!.setOnClickListener {
            if (mListener != null) {
                mListener!!.onConnect(bleDevice)
            }
        }
        holder.btn_disconnect!!.setOnClickListener {
            if (mListener != null) {
                mListener!!.onDisConnect(bleDevice)
            }
        }
        holder.btn_record!!.setOnClickListener {
            if (mListener != null) {
                mListener!!.onRecord(bleDevice)
            }
        }
        if (convView != null) return convView
        return convertView
    }

    class ViewHolder {
        var img_blue: ImageView? = null
        var txt_name: TextView? = null
        var layout_idle: LinearLayout? = null
        var layout_connected: LinearLayout? = null
        var btn_disconnect: Button? = null
        var btn_connect: Button? = null
        var btn_record: Button? = null
    }

    abstract class OnDeviceClickListener {
        fun OnDeviceClickListener() {}
        abstract fun onConnect(bleDevice: BleDevice?)
        abstract fun onDisConnect(bleDevice: BleDevice?)
        abstract fun onRecord(bleDevice: BleDevice?)
    }

    private var mListener: OnDeviceClickListener? = null
    fun setOnDeviceClickListener(listener: OnDeviceClickListener?) {
        mListener = listener
    }

}