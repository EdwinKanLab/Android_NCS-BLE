package com.ta.ncsble;

import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.time.LocalDateTime
import java.util.LinkedList
import java.util.Queue
import java.util.zip.GZIPOutputStream

public class Compressor(val ncsDirectory : String){

    private val NCSData = File(ncsDirectory).resolve("NCSData")
    private val Compressed = File(ncsDirectory).resolve("Compressed")



    private val packetSize = Constants.PACKET_SIZE
    private val numPackets = Constants.NUM_PACKETS


    private val binReadBuffer = ByteBuffer.allocateDirect(packetSize*numPackets)

    private var isCompressing = false



    private fun compressBinDir(dirFile : File){
        //Read all files in sorted order in directory dirName
        val binFiles: Array<File>? = dirFile.listFiles()

        if (binFiles != null) {
            if (binFiles.isEmpty()){
                dirFile.delete()
                return
            }
            try {
                binFiles.sort()
                val zfos = FileOutputStream(Compressed.resolve("${dirFile.name}.gz"))
                val gzos = GZIPOutputStream(zfos, Constants.GZIP_BUFF_SIZE)
                binReadBuffer.clear()


                for (file in binFiles) {
                    val bfis = FileInputStream(file)
                    val bfic = bfis.channel
                    bfic.read(binReadBuffer)
                    bfic.close()

                    binReadBuffer.flip()
                    gzos.write(binReadBuffer.array(), 0, binReadBuffer.array().size)
                    binReadBuffer.clear()
                }
                gzos.close()

                for (file in binFiles){ file.delete() }
                dirFile.delete()
            }
            catch (e: IOException) {
                Log.i("Exception", "Compression failed: $e")
            }
        }

    }


    private val compressorProcessor = Runnable{
        while (isCompressing){
            val dirsInNCSData: Array<File>? = NCSData.listFiles()
            if (dirsInNCSData != null){
                if (dirsInNCSData.size > 1){
                    var minDir = dirsInNCSData[0]
                    for (f in dirsInNCSData){
                        if (Utils.compareDirNames(f.name, minDir.name) < 0){
                            minDir = f
                        }
                    }
                    compressBinDir(minDir)
                }
            }
        }
    }

    private var compressorThread = Thread(compressorProcessor)


    fun startCompress(){
        NCSData.mkdirs()
        Compressed.mkdirs()
        isCompressing = true

        binReadBuffer.clear()


        compressorThread = Thread(compressorProcessor)
        compressorThread.start()
    }

    fun stopCompress(){
        isCompressing = false
        try {
            compressorThread.join()
        } catch (e: Exception) {
            Log.i("compressorThread", "///////////////////////////////Compressor thread failed to join.")
        }

        binReadBuffer.clear()

    }
}
