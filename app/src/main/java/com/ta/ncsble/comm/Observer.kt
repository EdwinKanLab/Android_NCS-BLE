package com.ta.ncsble.comm

import com.clj.fastble.data.BleDevice


interface Observer {
    fun disConnected(bleDevice: BleDevice?)
    fun connectionLost(bleDevice: BleDevice?)
}