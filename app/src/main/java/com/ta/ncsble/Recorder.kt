package com.ta.ncsble

import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.time.LocalDateTime


class Recorder(val ncsDirectory : String, val BLE_MAC : String) {

    private val NCSData = File(ncsDirectory).resolve("NCSData")


    private var binDirName : String = ""
    private var binFileCount  = 0

    private val compressionFrequency = Constants.COMPRESSION_FREQ

    private val packetSize = Constants.PACKET_SIZE
    private val numPackets = Constants.NUM_PACKETS

    private val binWriteBufferOne = ByteBuffer.allocateDirect(packetSize * numPackets)
    private val binWriteBufferTwo = ByteBuffer.allocateDirect(packetSize * numPackets)
    private val writeBuffers = arrayOf(binWriteBufferOne, binWriteBufferTwo)
    private var writeBufferPtr = 0
    private var binDiskWritePtr = 0


    private var isBinWriting = false




    private fun writeBin(ptr : Int){
        try {
            val binDir : File = NCSData.resolve(binDirName)
            binDir.mkdirs()

            val binFile : File =  binDir.resolve("$binFileCount")
            val bfos =  FileOutputStream(binFile)
            val bfoc = bfos.channel

            Log.i(
                "file i/o",
                "writing to file ///////////////////////////$ptr"
            )
            val current = writeBuffers[ptr]
            current.flip()

            while (current.hasRemaining()) {
                bfoc.write(current)
            }
            bfoc.force(false)
            bfoc.close()
            current.clear()
        } catch (e: IOException) {
            Log.i("Exception", "File write failed: $e")
        }
    }

    private val binProcessor = Runnable {
        while (isBinWriting) {
            val current = writeBuffers[binDiskWritePtr]
            while (current.position() < current.limit() && isBinWriting) {}
            if (isBinWriting){
                writeBin(binDiskWritePtr)
                binDiskWritePtr = writeBufferPtr
                binFileCount++
                if (binFileCount == compressionFrequency){
                    binFileCount = 0
                    binDirName = Utils.newBinDirName(BLE_MAC)
                }
            }
            else{
                writeBin(writeBufferPtr)
            }

        }

    }

    private var binThread = Thread(binProcessor)
//    private var compressorThread = Thread(compressorProcessor)


    fun startRecord(){
        NCSData.mkdirs()
        isBinWriting = true

        binFileCount = 0
        binWriteBufferOne.clear()
        binWriteBufferTwo.clear()
        writeBufferPtr = 0
        binDiskWritePtr = 0


        binThread = Thread(binProcessor)

        binDirName = Utils.newBinDirName(BLE_MAC)

        binThread.start()



    }

    fun stopRecord(){
        isBinWriting = false
        try {
            binThread.join()
        } catch (e: Exception) {
            Log.i("writeThread", "///////////////////////////////Bin thread failed to join.")
        }
        binWriteBufferOne.clear()
        binWriteBufferTwo.clear()
        writeBufferPtr = 0
        binDiskWritePtr = 0
        NCSData.resolve(Utils.newBinDirName(BLE_MAC)).mkdirs()



    }

    fun putData(data : ByteArray?){
        val current = writeBuffers[writeBufferPtr]
        current.put(data!!)
        if (current.position() == current.limit()) {
            writeBufferPtr = (writeBufferPtr + 1) % 2
        }

    }

}