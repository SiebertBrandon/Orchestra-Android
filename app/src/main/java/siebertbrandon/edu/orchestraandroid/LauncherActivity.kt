package siebertbrandon.edu.orchestraandroid

import android.Manifest
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.SurfaceHolder
import android.view.SurfaceView

import kotlinx.android.synthetic.main.activity_main_video.*
import android.Manifest.permission
import android.Manifest.permission.WRITE_EXTERNAL_STORAGE
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.support.v4.app.ActivityCompat
import android.content.pm.PackageManager
import android.app.Activity





class LauncherActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private val FILE_PATH: String = Environment.getExternalStorageDirectory().path + "/simpsons.mp4"
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf<String>(Manifest.permission.READ_EXTERNAL_STORAGE,
                                                      Manifest.permission.WRITE_EXTERNAL_STORAGE)

    private var videodecoder: VideoDecodeThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        verifyStoragePermissions(this)
        println(Environment.getExternalStorageDirectory().getPath() + "/")
        Log.d("Test", "Test!")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_video)
        setSupportActionBar(toolbar)

        val surfaceView = SurfaceView(this)
        surfaceView.holder.addCallback(this)
        setContentView(surfaceView)
        surfaceView.holder.setFixedSize(1920, 1080)

        videodecoder = VideoDecodeThread()

    }

    override fun surfaceDestroyed(p0: SurfaceHolder) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun surfaceCreated(p0: SurfaceHolder) {
        //TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {
        if (videodecoder?.init(surface = p0.surface, filepath = FILE_PATH)!!) {
            videodecoder?.start()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main_video, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            //R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    fun verifyStoragePermissions(activity: Activity) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE)

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            )
        }
    }
}
