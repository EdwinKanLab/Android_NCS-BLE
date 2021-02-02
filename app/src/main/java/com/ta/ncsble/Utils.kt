package com.ta.ncsble

import android.content.Context
import androidx.work.*
import com.ta.ncsble.workers.CompressorWorker
import com.ta.ncsble.workers.UploaderWorker
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

object Utils {
    fun compareDirNames(a : String,  b : String) : Int {
        val an = a.split(".")[0]
        val bn = b.split(".")[0]
        val aSplit = an.split("_").toTypedArray()
        val bSplit = bn.split("_").toTypedArray()
        if (BuildConfig.DEBUG && !(aSplit.size == 8 && bSplit.size == 8)) {
            error("Assertion failed")
        }

        val aDate = aSplit[1].split("-").toTypedArray()
        val aYear = Integer.valueOf(aDate[0])
        val aMonth = Integer.valueOf(aDate[1])
        val aDay = Integer.valueOf(aDate[2])
        val aHour = Integer.valueOf(aSplit[2])
        val aMinute = Integer.valueOf(aSplit[3])
        val aSecond = Integer.valueOf(aSplit[4])
        val aNS = Integer.valueOf(aSplit[5])*1000000 +
                Integer.valueOf(aSplit[6])*1000 +
                Integer.valueOf(aSplit[7])

        val aLDT = LocalDateTime.of(aYear, aMonth, aDay, aHour, aMinute, aSecond, aNS)

        val bDate = bSplit[1].split("-").toTypedArray()
        val bYear = Integer.valueOf(bDate[0])
        val bMonth = Integer.valueOf(bDate[1])
        val bDay = Integer.valueOf(bDate[2])
        val bHour = Integer.valueOf(bSplit[2])
        val bMinute = Integer.valueOf(bSplit[3])
        val bSecond = Integer.valueOf(bSplit[4])
        val bNS = Integer.valueOf(bSplit[5])*1000000 +
                Integer.valueOf(bSplit[6])*1000 +
                Integer.valueOf(bSplit[7])

        val bLDT = LocalDateTime.of(bYear, bMonth, bDay, bHour, bMinute, bSecond, bNS)

        return aLDT.compareTo(bLDT)

    }

    fun newBinDirName(BLE_MAC : String) : String{
        val LDTNow = LocalDateTime.now()
        val date = LDTNow.toLocalDate().toString()
        val hour ="%02d".format(LDTNow.hour)
        val minute = "%02d".format(LDTNow.minute)
        val second = "%02d".format(LDTNow.second)
        val ns = "%09d".format(LDTNow.nano)
        return "${BLE_MAC}_${date}_${hour}_${minute}_${second}" +
                "_${ns.substring(0, 3)}_${ns.substring(3,6)}_${ns.substring(6,9)}"
    }



}