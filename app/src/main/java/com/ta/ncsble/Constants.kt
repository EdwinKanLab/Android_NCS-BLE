package com.ta.ncsble

import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage

object Constants{
    const val PACKET_SIZE = 220
    const val NUM_PACKETS = 100
    const val COMPRESSION_FREQ = 10
    const val GZIP_BUFF_SIZE = PACKET_SIZE* NUM_PACKETS*50

    const val COMPRESSION_WORK_NAME = "ncsble_compression_work"
    const val UPLOADER_WORK_NAME = "ncsble_uploader_work"
    val STORAGE = Firebase.storage("gs://ncsble.appspot.com")


    const val RECORDER_NOTIFICATION_CHANNEL_ID = "recording_channel"
    const val RECORDER_NOTIFICATION_CHANNEL_NAME = "Recording"
    const val RECORDER_NOTIFICATION_ID = 1

    const val START_RECORD_ACTION = "start_record"
    const val STOP_RECORD_ACTION = "stop_record"
    const val CONNECT_BLE_DEVICE_ACTION = "connect_ble"
    const val ACTIVE_DISCONNECT_BLE_DEVICE_ACTION = "active_disconnect_ble"
    const val PASSIVE_DISCONNECT_BLE_DEVICE_ACTION = "passive_disconnect_ble"

    const val BLE_CONN_STATUS = "NCS_BLE_CONN_STATUS"
    const val BLE_RECORD_STATUS = "NCS_BLE_RECORD_STATUS"


    const val KEY_DATA = "key_data"
    const val UUID_SERVICE = "f000c0c0-0451-4000-b000-000000000000"
    const val UUID_CHARACTERISTIC_NOTIFY = "f000c0c2-0451-4000-b000-000000000000"

    const val NUM_RETRIES = 10

}