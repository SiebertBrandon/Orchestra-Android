package siebertbrandon.edu.orchestraandroid

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.os.Bundle
import android.os.Environment
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup
import android.widget.RelativeLayout
import kotlinx.android.synthetic.main.*
import kotlinx.android.synthetic.main.video_layout.*


/**
 * Created by Brandon on 2/13/2018.
 */

class OrchestraVideoActivity : Activity(), SurfaceHolder.Callback {

    enum class ScrollState {
        TOPLEFT, TOPRIGHT, BOTTOMRIGHT, BOTTOMLEFT
    }
    var currentScrollState = ScrollState.TOPLEFT
    var surfaceHolder : SurfaceHolder? = null
    var video_decode_thread: VideoDecodeThread? = null
    private val FILE_PATH: String = Environment.getExternalStorageDirectory().path + "/simpsons.mp4"

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_layout)

        // Create and handle surface view initialization parameters. Callback will be this class.
        surfaceHolder = surfaceView.holder
        surfaceHolder!!.setFixedSize(5760, 3240)
        surfaceHolder!!.addCallback(this)

        // Create Video View Thread
        video_decode_thread = VideoDecodeThread()

        // Callbacks for buttons
        toggleScroll.setOnClickListener { _ ->
            if (currentScrollState == ScrollState.TOPLEFT) {
                currentScrollState = ScrollState.TOPRIGHT
                hScrollView.scrollTo(5000, 0)
                vScrollView.scrollTo(0, 0)
            } else if (currentScrollState == ScrollState.TOPRIGHT) {
                currentScrollState = ScrollState.BOTTOMRIGHT
                hScrollView.scrollTo(5000, 0)
                vScrollView.scrollTo(0, 2000)
            } else if (currentScrollState == ScrollState.BOTTOMRIGHT) {
                currentScrollState = ScrollState.BOTTOMLEFT
                hScrollView.scrollTo(0, 0)
                vScrollView.scrollTo(0, 2000)
            } else if (currentScrollState == ScrollState.BOTTOMLEFT) {
                currentScrollState = ScrollState.TOPLEFT
                hScrollView.scrollTo(0, 0)
                vScrollView.scrollTo(0, 0)
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        if (video_decode_thread != null) {
            video_decode_thread?.close();
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        if (video_decode_thread != null) {
            if (video_decode_thread?.init(holder.getSurface(), FILE_PATH)!!) {
                video_decode_thread?.start();

            } else {
                video_decode_thread = null;
            }

        }
    }

    override fun surfaceCreated(holder: SurfaceHolder) {

    }
}