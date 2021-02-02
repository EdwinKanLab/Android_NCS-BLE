package com.ta.ncsble.comm

import com.clj.fastble.data.BleDevice
import java.util.*


class ObserverManager : Observable {
    private object ObserverManagerHolder {
        val sObserverManager = ObserverManager()
    }

    private val observers: MutableList<Observer> =
        ArrayList()

    override fun addObserver(obj: Observer?) {
        if (obj != null) observers.add(obj)
    }

    override fun deleteObserver(obj: Observer?) {
        val i = observers.indexOf(obj)
        if (i >= 0) {
            observers.remove(obj)
        }
    }

    override fun notifyObserver(bleDevice: BleDevice?) {
        for (i in observers.indices) {
            val o = observers[i]
//            o.disConnected(bleDevice)
            o.connectionLost(bleDevice)
        }
    }
//    fun getInstance(): ObserverManager? {
//        return ObserverManagerHolder.sObserverManager
//    }

    companion object {
        fun getInstance(): ObserverManager? {
            return ObserverManagerHolder.sObserverManager
        }

    }
}
