package siebertbrandon.edu.orchestraandroid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.support.v4.app.ActivityCompat.startActivityForResult
import android.content.Intent
import android.app.ActivityManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.os.SystemClock
import android.util.Log
import siebertbrandon.edu.orchestraandroid.BluetoothControl as BC


/**
 * Created by Brandon on 3/5/2018.
 */
class BluetoothThread(context: Context) : Thread() {
    var mBluetoothScanner : BluetoothLeScanner? = null
    var currentlyScanning : Boolean = false
    val mContext : Context = context

    init {
        // Try to initialize Bluetooth Thread. Exit if unsuccessful
        Log.d("[BTT]","Init Bluetooth Thread")
        try {
            mBluetoothScanner = BC.mBluetoothAdapter!!.bluetoothLeScanner
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun run() {
        Log.d("[BTT]","Began Bluetooth Thread")
        while (BC.bluetoothEnabled) {
            if (!currentlyScanning) {
                startScan()
                currentlyScanning = true
            }
            sleep(1000)
            Log.d("[BTT]","Debug Looping")
        }
        if (currentlyScanning) {
            Log.d("[BTT]","Stopping Scan")
            stopScan()
            currentlyScanning = false
        }
        Log.d("[BTT]","Exiting")
    }

    private fun startScan() {
        mBluetoothScanner!!.startScan(ScanReciever())
    }

    private fun stopScan() {
        mBluetoothScanner!!.stopScan(ScanReciever())
    }


    private class ScanReciever() : ScanCallback() {
        override fun onBatchScanResults (results : List<ScanResult>) {
            println("[BTT] Got a batch scan result!")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            println("[BTT] Scan failed!")
        }

        override fun onScanResult(callbackType: Int, resultNullable: ScanResult?) {
            val result = resultNullable!!
            val record : ScanRecord = result.scanRecord
            /*
            Print packet details

            Log.d("[BTT]", "New Scan Result! -------------------------------")
            Log.d("[BTT]", "DeviceID: " + result.device.name)
            Log.d("[BTT]", "DeviceAD: " + result.device.address)
            Log.d("[BTT]", "TimeStmp: " + (SystemClock.elapsedRealtimeNanos() - result.timestampNanos))
            //Log.d("[BTT]", "UptimeMl: " + SystemClock.uptimeMillis()*1000)
            //Log.d("[BTT]", "NanoTime: " + System.nanoTime())
            //Log.d("[BTT]", "ElRTNano: " + SystemClock.elapsedRealtimeNanos())
            Log.d("[BTT]", "ManuData: " + record.manufacturerSpecificData)
            Log.d("[BTT]", "ServData: " + record.serviceData)
            Log.d("[BTT]", "TxPowerL: " + record.txPowerLevel)
            Log.d("[BTT]", "AdvertFl: " + record.advertiseFlags)
            var testoutput = ""
            for (b in record.bytes) {
                testoutput += String.format("%02X", b)
            }
            Log.d("[BTT]", "AllBytes: " + testoutput)
            Log.d("[BTT]", "------------------------------------------------")
            */
        }
    }
}