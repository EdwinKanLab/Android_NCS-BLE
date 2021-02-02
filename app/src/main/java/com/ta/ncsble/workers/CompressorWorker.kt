package com.ta.ncsble.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ta.ncsble.Constants
import com.ta.ncsble.Utils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.*
import java.util.zip.GZIPOutputStream

class CompressorWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.i("Uploader", "/////////////////Starting compressor worker")

        val ncsDirectory : String = inputData.getString("ncsDirectory") ?: return Result.retry()

        val NCSData = File(ncsDirectory).resolve("NCSData")
        val Compressed = File(ncsDirectory).resolve("Compressed")
        Compressed.mkdirs()

        val dirsInNCSData: Array<File>? = NCSData.listFiles()
        if (dirsInNCSData != null){
            if (dirsInNCSData.size > 1){
                var maxDir = dirsInNCSData[0]
                for (f in dirsInNCSData){
                    if (Utils.compareDirNames(f.name, maxDir.name) > 0){
                        maxDir = f
                    }
                }
                val dirsToCompress : LinkedList<File> = LinkedList<File>()
                for (f in dirsInNCSData){
                    if (Utils.compareDirNames(f.name, maxDir.name) != 0) {
                        dirsToCompress.add(f)
                    }
                }
                try {
                    compressBinDir(dirsToCompress, Compressed)
                } catch(e: IOException) {
                    return Result.failure()
                }
            }
        }


        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
    private fun compressBinDir(dirsToCompress : LinkedList<File>, Compressed : File) {
        val packetSize = Constants.PACKET_SIZE
        val numPackets = Constants.NUM_PACKETS
        val binReadBuffer = ByteBuffer.allocateDirect(packetSize*numPackets)

        for (dirFile in dirsToCompress) {
            binReadBuffer.clear()
            val binFiles: Array<File>? = dirFile.listFiles()

            if (binFiles != null) {
                if (binFiles.isEmpty()) {
                    dirFile.delete()
                    val zfos = FileOutputStream(Compressed.resolve("${dirFile.name}.empty.gz"))
                    val gzos = GZIPOutputStream(zfos)
                    val barray = "empty".toByteArray()
                    gzos.write(barray, 0, barray.size)
                    gzos.close()
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

                    for (file in binFiles) {
                        file.delete()
                    }
                    dirFile.delete()
                } catch (e: IOException) {
                    Log.i("Exception", "Compression failed: $e")
                    throw e
                }
            }
        }

    }
}
