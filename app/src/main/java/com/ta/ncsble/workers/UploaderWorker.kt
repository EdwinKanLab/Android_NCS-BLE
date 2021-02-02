package com.ta.ncsble.workers


import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.ta.ncsble.Constants
import com.ta.ncsble.Constants.STORAGE
import com.ta.ncsble.Utils
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.lang.Exception
import java.nio.ByteBuffer
import java.util.*

import com.google.firebase.storage.UploadTask
import java.util.concurrent.atomic.AtomicInteger

class UploaderWorker(appContext: Context, workerParams: WorkerParameters):
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        Log.i("Uploader", "/////////////////Starting uploader worker")

        val ncsDirectory : String = inputData.getString("ncsDirectory") ?: return Result.retry()
        Log.i("Uploader", "/////////////////ncsDirectory $ncsDirectory")

        val Compressed = File(ncsDirectory).resolve("Compressed")

        val gzInCompressed: Array<File>? = Compressed.listFiles()

        val done = AtomicInteger(0)
        var listTasks = LinkedList<UploadTask>()

        if (gzInCompressed != null){

            if (gzInCompressed.size > 0){
                var gzInProg = gzInCompressed[0]
//                var gzToUpload = gzInCompressed[0]

                for (f in gzInCompressed){
                    if (Utils.compareDirNames(f.name, gzInProg.name) > 0){
                        gzInProg = f
                    }
                }

                for (f in gzInCompressed){
                    done.getAndIncrement()

                    if (Utils.compareDirNames(f.name, gzInProg.name) != 0){
                        val splits = f.name.split('.')
                        if (splits.size == 3 && splits[1] == "empty"){ f.delete(); done.getAndDecrement(); continue}
                        Log.i("Uploader", "/////////////////Uploading ${f.name}")
                        val newSplits = f.name.split('_')
                        val mac = newSplits[0]
                        val date = newSplits[1]
                        val gzRef = STORAGE.reference.child(mac + '/' + date + '/'+f.name)
                        val uri = Uri.fromFile(f)
                        val uploadTask = gzRef.putFile(uri)
                        uploadTask.addOnSuccessListener {
                            Log.i("Uploader", "/////////////////Uploaded ${f.name}")
                            f.delete(); done.getAndDecrement() }
                            .addOnFailureListener{done.getAndDecrement()}
                        listTasks.add(uploadTask)
                    }
                }

//                val gzRef = STORAGE.reference.child(gzToUpload.name)
//                val uri = Uri.fromFile(gzToUpload)
//                val uploadTask = gzRef.putFile(uri)
//                uploadTask.addOnSuccessListener {
//                    Log.i("Uploader", "/////////////////Uploaded ${gzToUpload.name}")
//                    gzToUpload.delete(); done = true }
//                    .addOnFailureListener{done = true}
            }
        }

        while (done.get() != 0){}


        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }

}
