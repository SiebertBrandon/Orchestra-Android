package siebertbrandon.edu.orchestraandroid

import android.app.Activity
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanRecord
import android.bluetooth.le.ScanResult
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.Rect
import android.media.Image
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.os.Bundle
import android.os.Environment
import android.os.SystemClock
import android.support.v8.renderscript.*
import android.util.Log
import android.view.Choreographer
import android.view.SurfaceHolder
import edu.siebertbrandon.colorspace.ScriptC_yuv420888
import kotlinx.android.synthetic.main.video_layout.*
import java.io.IOException
import kotlin.concurrent.thread
import kotlin.math.abs


/**
 * Created by Brandon on 2/13/2018.
 */

class FullScreenVideoView : Activity(), SurfaceHolder.Callback {

    // View Objects
    companion object {
        lateinit var FRAME_SURFACE: SurfaceHolder
        @Volatile
        var FRAME_BUFFER = CircularFrameBuffer(10)
        lateinit var MEDIA_CODEC: MediaCodec
        var BLUETOOTH_TRIGGER: Boolean = false
        var AVERAGE_START_GUESS: Long = 0L
        var START_VIDEO: Boolean = true
        var DEBUG: Boolean = true
    }

    enum class ScrollState {
        TOPLEFT, TOPRIGHT, BOTTOMRIGHT, BOTTOMLEFT
    }

    private var currentScrollState = ScrollState.TOPLEFT
    private var viewPrefix = "[VV]"

    // Decode Objects
    private val filePath: String = Environment.getExternalStorageDirectory().path + "/color.mp4"
    private val videoTag = "video/"
    private val decodePrefix = "[VD]"
    private var mExtractor: MediaExtractor = MediaExtractor()
    private lateinit var rs: RenderScript

    // Render Objects
    private var frame_number: Int = 0
    private val chor = android.view.Choreographer.getInstance()

    // Bluetooth Objects
    private lateinit var mBluetoothScanner: BluetoothLeScanner
    private lateinit var mScanCallback: ScanCallback
    private var currentlyScanning: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        // Initialize this view object
        super.onCreate(savedInstanceState)
        setContentView(R.layout.video_layout)
        //this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        rs = RenderScript.create(applicationContext)

        // Create and size the surface holder
        FRAME_SURFACE = surfaceView.holder
        FRAME_SURFACE.setFixedSize(1280 * 5, 720 * 5)
        FRAME_SURFACE.addCallback(this)

        // Callbacks for buttons
        toggleScroll.setOnClickListener { _ ->
            when (currentScrollState) {
                ScrollState.TOPLEFT -> {
                    currentScrollState = ScrollState.TOPRIGHT
                    hScrollView.scrollTo(5000, 0)
                    vScrollView.scrollTo(0, 0)
                }
                ScrollState.TOPRIGHT -> {
                    currentScrollState = ScrollState.BOTTOMRIGHT
                    hScrollView.scrollTo(5000, 0)
                    vScrollView.scrollTo(0, 2000)
                }
                ScrollState.BOTTOMRIGHT -> {
                    currentScrollState = ScrollState.BOTTOMLEFT
                    hScrollView.scrollTo(0, 0)
                    vScrollView.scrollTo(0, 2000)
                }
                ScrollState.BOTTOMLEFT -> {
                    currentScrollState = ScrollState.TOPLEFT
                    hScrollView.scrollTo(0, 0)
                    vScrollView.scrollTo(0, 0)
                }
            }
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        Log.d(decodePrefix, "The Surface has been Destroyed!")
        mBluetoothScanner.stopScan(mScanCallback)
        MEDIA_CODEC.stop()
        MEDIA_CODEC.release()
        mExtractor.release()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        Log.d(decodePrefix, "Surface change event")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        // Initialize, begin, and attach Media Decode
        Log.d(viewPrefix, "Attempting to load video from $filePath")

        val step1: Boolean = startMediaDecode()
        if (!step1) Log.d(viewPrefix, "ERROR: Step1 has failed!")

        // Initialize, begin, and attach Media Render
        val step2 = if (step1) startMediaRender() else false
        if (!step2) Log.d(viewPrefix, "ERROR: Step2 has failed!")

        // Initialize, begin, and attach Bluetooth Listener
        val step3: Boolean = if (step2) startBluetoothListen() else false
        if (!step3) Log.d(viewPrefix, "ERROR: Step3 has failed!")
        else Log.d(viewPrefix, "All setup steps have been completed successfully!")

        Log.d(viewPrefix, "Starting MediaCodec")
        MEDIA_CODEC.start()
    }

    /**
     *
     */
    private fun startMediaDecode(): Boolean {
        try {
            Log.d(decodePrefix, "Initializing media extraction")
            mExtractor.setDataSource(filePath)
            Log.d(decodePrefix, "File source: $filePath")
            val format = mExtractor.getTrackFormat(0)
            val mime = format.getString(MediaFormat.KEY_MIME)

            // If the resource is a video, create a decoder for it
            if (mime.startsWith(videoTag)) {

                // Prime the extractor
                mExtractor.selectTrack(0)

                // Set the decoder
                MEDIA_CODEC = MediaCodec.createDecoderByType(mime)

                // Attempt to configure the decoder for the encrypted format
                try {
                    Log.d(decodePrefix, "Set Video Format: $format")
                    MEDIA_CODEC.configure(format, null, null, 0 /* Decoder */)

                } catch (e: IllegalStateException) {
                    Log.e(decodePrefix, "IllegalStateException: codec '$mime' failed configuration. $e")
                    return false
                } catch (e: IllegalArgumentException) {
                    Log.e(decodePrefix, "IllegalArgumentException: codec '$mime' failed configuration. $e")
                    return false
                } catch (e: MediaCodec.CryptoException) {
                    Log.e(decodePrefix, "CryptoException: codec '$mime' failed configuration. $e")
                    return false
                } catch (e: MediaCodec.CodecException) {
                    Log.e(decodePrefix, "CodecException: codec '$mime' failed configuration. $e")
                    return false
                }
                Log.d(decodePrefix, "Configuration Complete")
                // Set Decoder callbacks for frame sending and receiving
                MEDIA_CODEC.setCallback(object : MediaCodec.Callback() {

                    // If the media output format changes, this will be called
                    override fun onOutputFormatChanged(codec: MediaCodec?, format: MediaFormat?) {
                        // Not implemented.
                    }

                    // If the decoder has an input buffer available
                    override fun onInputBufferAvailable(mc: MediaCodec, inputBufferId: Int) {

                        // Initialize buffer reference and read data chunk
                        val inputBuffer = MEDIA_CODEC.getInputBuffer(inputBufferId)
                        val sampleSize = mExtractor.readSampleData(inputBuffer, 0)

                        //Log.d("InputBuffer", "Feeding new Input Buffer")
                        // If not at the end of the stream fill input buffer and queue it
                        if (mExtractor.advance() && sampleSize > 0) {

                            MEDIA_CODEC.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    sampleSize,
                                    mExtractor.sampleTime,
                                    0)

                            //Log.d(decodePrefix, "Input buffer queued")
                        }

                        // If at the end of the stream, set END_OF_STREAM flag
                        else {
                            if (DEBUG) Log.d(decodePrefix, "InputBuffer BUFFER_FLAG_END_OF_STREAM")

                            MEDIA_CODEC.queueInputBuffer(
                                    inputBufferId,
                                    0,
                                    0,
                                    0,
                                    MediaCodec.BUFFER_FLAG_END_OF_STREAM)

                            mExtractor.seekTo(0, MediaExtractor.SEEK_TO_CLOSEST_SYNC)
                            if (DEBUG) Log.d(decodePrefix, "Input stream has ended")
                        }
                    }

                    // Called when the Decoder finishes decoding a frame
                    override fun onOutputBufferAvailable(
                            mc: MediaCodec,
                            outputBufferId: Int,
                            info: MediaCodec.BufferInfo?) {

                        // Initialize buffer reference
                        val outputImage = MEDIA_CODEC.getOutputImage(outputBufferId)
                        val bitmap: Bitmap = yuv420RGB(
                                outputImage,
                                outputImage.width,
                                outputImage.height)

                        //Log.d("[IMAGE INFO]", "Image: ${image.format}")
                        //Log.d(decodePrefix, "Queueing Output Buffer. Bitmap size: ${bitmap
                        // .byteCount}")
                        FRAME_BUFFER.add(Pair(bitmap, outputBufferId))
                    }

                    // If there is an error regading the decoder (too many buffers out, etc...)
                    override fun onError(codec: MediaCodec?, e: MediaCodec.CodecException?) {
                        e?.printStackTrace() ?: Log.d(decodePrefix, "Unknown error from the codec")
                        return
                    }
                })
                Log.d(decodePrefix, "Callbacks Set")
            }
        }
        // Last chance to catch an error
        catch (e: IOException) {
            Log.e(viewPrefix, "ERROR during setup: ${e.message}")
            return false
        }
        return true
    }

    /**
     *
     */
    private fun startMediaRender(): Boolean {

        chor.postFrameCallback(object : Choreographer.FrameCallback {
            override fun doFrame(frameTimeNanos: Long) {
                if (START_VIDEO) {
                    frame_number++
                    if (DEBUG) Log.d("[REN]", "RENDER_EVENT frame: ${frame_number}")
                    var frame = FRAME_BUFFER.render()

                    // Crop
                    if (frame != null) frame = Bitmap.createBitmap(frame, 100, 100,
                            frame.width - (frame.width - 1),
                            frame.height - (frame.height - 1))

                    if (frame != null) {
                        val canvas = FRAME_SURFACE.lockCanvas()
                        if (canvas != null) {
                            canvas.drawBitmap(
                                    frame,
                                    null,
                                    //Rect(0, 0, frame.width, frame.height),
                                    Rect(0, 0, 1440, 2560),
                                    Paint()
                            )
                            FRAME_SURFACE.unlockCanvasAndPost(canvas)
                        }
                    }
                }
                chor.postFrameCallback(this)
            }
        })
        return true

    }

    /**
     *
     */
    private fun startBluetoothListen(): Boolean {
        try {
            mBluetoothScanner = BluetoothControlSettings.mBluetoothAdapter!!.bluetoothLeScanner
        } catch (e: Exception) {
            e.printStackTrace()
        }
        BluetoothControlSettings.enabled = true
        mScanCallback = BluetoothScanCallback()
        mBluetoothScanner.startScan(mScanCallback)
        return true
    }

    private class BluetoothScanCallback : ScanCallback() {
        lateinit var startTrigger: Thread
        override fun onBatchScanResults(results: List<ScanResult>) {
            if (DEBUG) println("[BTT] Got a batch scan result!")
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            if (DEBUG) println("[BTT] Scan failed!")
        }

        override fun onScanResult(callbackType: Int, resultNullable: ScanResult?) {
            resultNullable?.let {
                val record: ScanRecord = it.scanRecord

                if (DEBUG) Log.d("[BTT]", "BLUEOOTH_EVENT: " +
                        "Name: ${it.device.name}, " +
                        "MAC: ${it.device.address}, " +
                        "Power: ${it.rssi}")

                // If we have the right device
                if (it.device.name != null && it.device.name.startsWith("orchestra")) {
                    //Log.d("[BTT]", "Packet Data: " + it.scanRecord.bytes)
                    val listBytes = it.scanRecord.bytes!!.contentToString()
                            .removeSurrounding("[", "]")
                            .split(",")
                            .map { it.trim().toInt() }
                    //Log.d("[BTT]", "Stringed Bytes Test: $listBytes")

                    val bytesStartTime = listBytes.slice(16..20)
                    var valueStartTime: Long = 0
                    for (i in 0 until bytesStartTime.size) {
                        valueStartTime = (valueStartTime shl 8) + (bytesStartTime[i] and 0xff)
                    }
                    //Log.d("[BTT]", "Bytes Start Time: $bytesStartTime")
                    //Log.d("[BTT]", "Value Start Time: $valueStartTime")

                    //val conductorStartTime = java.lang.Long.parseLong(valueStartTime, 16)
                    val bytesCurrentTime = listBytes.slice(21..25)

                    var valueCurrentTime: Long = 0
                    for (i in 0 until bytesCurrentTime.size) {
                        valueCurrentTime = (valueCurrentTime shl 8) + (bytesCurrentTime[i] and 0xff)
                    }
                    //Log.d("[BTT]", "Bytes Current Time: $bytesCurrentTime")
                    //Log.d("[BTT]", "Value Current Time: $valueCurrentTime")

                    val bytesBluetoothTime = listBytes.slice(26..30)
                    var valueBluetoothTime: Long = 0
                    for (i in 0 until bytesBluetoothTime.size) {
                        valueBluetoothTime = (valueBluetoothTime shl 8) + (bytesBluetoothTime[i] and 0xff)
                    }
                    //Log.d("[BTT]", "Bytes BT Time: $bytesBluetoothTime")
                    //Log.d("[BTT]", "Value BT Time: $valueBluetoothTime")

                    val currentLocalTime = SystemClock.elapsedRealtimeNanos() / 1000

                    // Guess Packet Start Time
                    if (DEBUG) Log.d("[BTT]", "Packet Time Event: Reference Time = $valueStartTime")
                    if (DEBUG) Log.d("[BTT]", "[CS]: $valueCurrentTime -> [CB]: $valueBluetoothTime " +
                            "~~> [LB] -> ${it.timestampNanos / 1000} -> [LS]: $currentLocalTime")
                    val diffLocalCurrentBT = abs(currentLocalTime - it.timestampNanos / 1000)
                    if (AVERAGE_START_GUESS != 0L) AVERAGE_START_GUESS = listOf<Long>(
                            AVERAGE_START_GUESS, AVERAGE_START_GUESS, AVERAGE_START_GUESS,
                            currentLocalTime - diffLocalCurrentBT - valueCurrentTime + valueStartTime)
                            .average().toLong()
                    else AVERAGE_START_GUESS = currentLocalTime - diffLocalCurrentBT -
                            valueCurrentTime + valueStartTime
                    //var inferredLocalStart = inferredLocalInit
                    if (DEBUG) Log.d("[BTT]", "Average Start Guess : ${FullScreenVideoView
                            .AVERAGE_START_GUESS}")

                    if (BLUETOOTH_TRIGGER == false) {
                        Log.d("[BTT]", "BLUEOOTH_EVENT: Found Conductor!")
                        startTrigger = thread(start = true) {
                            while (SystemClock.elapsedRealtimeNanos() / 1000 < AVERAGE_START_GUESS
                                    || AVERAGE_START_GUESS == 0L) {
                                // Wait
                            }
                            START_VIDEO = true
                        }
                        BLUETOOTH_TRIGGER = true
                    }
                }
            }
        }
    }

    private fun yuv420RGB(image: Image, width: Int, height: Int): Bitmap {
        // Get the three image planes
        val planes = image.planes
        var buffer = planes[0].buffer
        val y = ByteArray(buffer.remaining())
        buffer.get(y)

        buffer = planes[1].buffer
        val u = ByteArray(buffer.remaining())
        buffer.get(u)

        buffer = planes[2].buffer
        val v = ByteArray(buffer.remaining())
        buffer.get(v)

        // get the relevant RowStrides and PixelStrides
        // (we know from documentation that PixelStride is 1 for y)
        val yRowStride = planes[0].rowStride
        val uvRowStride = planes[1].rowStride  // we know from   documentation that RowStride is the same for u and v.
        val uvPixelStride = planes[1].pixelStride  // we know from   documentation that PixelStride is the same for u and v.


        val mYuv420 = ScriptC_yuv420888(rs)

        // Y,U,V are defined as global allocations, the out-Allocation is the Bitmap.
        // Note also that uAlloc and vAlloc are 1-dimensional while yAlloc is 2-dimensional.
        val typeUcharY = Type.Builder(rs, Element.U8(rs))
        typeUcharY.setX(yRowStride).setY(height)
        val yAlloc = Allocation.createTyped(rs, typeUcharY.create())
        yAlloc.copyFrom(y)
        mYuv420._ypsIn = yAlloc

        val typeUcharUV = Type.Builder(rs, Element.U8(rs))
        // note that the size of the u's and v's are as follows:
        //      (  (width/2)*PixelStride + padding  ) * (height/2)
        // =    (RowStride                          ) * (height/2)
        // but I noted that on the S7 it is 1 less...
        typeUcharUV.setX(u.size)
        val uAlloc = Allocation.createTyped(rs, typeUcharUV.create())
        uAlloc.copyFrom(u)
        mYuv420._uIn = uAlloc

        val vAlloc = Allocation.createTyped(rs, typeUcharUV.create())
        vAlloc.copyFrom(v)
        mYuv420._vIn = vAlloc

        // handover parameters
        mYuv420._picWidth = width.toLong()
        mYuv420._uvRowStride = uvRowStride.toLong()
        mYuv420._uvPixelStride = uvPixelStride.toLong()

        val outBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val outAlloc = Allocation.createFromBitmap(rs, outBitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT)

        val lo = Script.LaunchOptions()
        lo.setX(0, width)  // by this we ignore the y’s padding zone, i.e. the right side of x between width and yRowStride
        lo.setY(0, height)

        mYuv420.forEach_doConvert(outAlloc, lo)
        outAlloc.copyTo(outBitmap)

        return outBitmap
    }
}