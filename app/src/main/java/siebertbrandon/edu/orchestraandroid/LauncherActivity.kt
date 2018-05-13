package siebertbrandon.edu.orchestraandroid

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import kotlinx.android.synthetic.main.main_menu.*
import siebertbrandon.edu.orchestraandroid.BluetoothControlSettings as BC

class LauncherActivity : AppCompatActivity() {

    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE,
                                                      Manifest.permission.WRITE_EXTERNAL_STORAGE)
    private val PERMISSIONS_LOCATION = arrayOf<String>(Manifest.permission.ACCESS_FINE_LOCATION)

    override fun onCreate(savedInstanceState: Bundle?) {

        // Verify that the app has
        verifyStoragePermissions(this)
        BC.enabled = false

        println(Environment.getExternalStorageDirectory().path + "/")

        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_menu)

        BC.mBluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        BC.mBluetoothAdapter = BC.mBluetoothManager!!.adapter
        if (BC.mBluetoothAdapter == null || !BC.mBluetoothAdapter!!.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, 1)
        }

        Log.d("[OrchestraUI]", "onCreate -> Main Menu")


        buttonVideo.setOnClickListener { name ->
            startActivity(Intent(this, FullScreenVideoView::class.java).apply {
                // Add any extra intent payloads here
            })
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        BC.enabled = false
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        return true
    }

    private fun verifyStoragePermissions(activity: Activity) {
        // Check if we have write permission
        val permission_write_external = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)
        val permission_read_external = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE)
        val permission_bluetooth = ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH)
        val permission_bluetooth_admin = ActivityCompat.checkSelfPermission(activity, Manifest.permission.BLUETOOTH_ADMIN)
        val permission_fine_location = ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION)


        if (permission_write_external != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
        if (permission_fine_location != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_LOCATION,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }
}
