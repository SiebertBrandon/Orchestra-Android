package siebertbrandon.edu.orchestraandroid

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import android.view.Surface
import java.io.FileInputStream
import java.io.IOException

/**
 * Created by Brandon on 2/8/2018.
 */
class VideoDecodeThread : Thread() {
    private val VIDEO = "video/"
    private val TAG = "[VD]"
    private var mExtractor: MediaExtractor? = null
    private var mDecoder: MediaCodec? = null

    private var eosReceived: Boolean = false

    fun init(surface: Surface, filepath: String): Boolean {
        eosReceived = false
        try {
            mExtractor = MediaExtractor()
            var fd = FileInputStream(filepath).getFD()
            mExtractor!!.setDataSource(fd)

            for (i in 0 until mExtractor!!.trackCount) {

                val format = mExtractor!!.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME)

                if (mime.startsWith(VIDEO)) {
                    mExtractor!!.selectTrack(i)
                    mDecoder = MediaCodec.createDecoderByType(mime)
                    try {
                        Log.d(TAG, "Set Video Format: $format")
                        mDecoder!!.configure(format, surface, null, 0 /* Decoder */)

                    } catch (e: IllegalStateException) {
                        Log.e(TAG, "codec '$mime' failed configuration. $e")
                        return false
                    }

                    mDecoder!!.start()
                    break
                }
            }

        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }

        return true
    }

    override fun run() {
        val info = MediaCodec.BufferInfo()
        val inputBuffers = mDecoder!!.inputBuffers
        mDecoder!!.outputBuffers

        var isInput = true
        var first = false
        var startWhen: Long = 0

        while (!eosReceived) {
            if (isInput) {
                val inputIndex = mDecoder!!.dequeueInputBuffer(10000)
                if (inputIndex >= 0) {
                    // fill inputBuffers[inputBufferIndex] with valid data
                    val inputBuffer = inputBuffers[inputIndex]

                    val sampleSize = mExtractor!!.readSampleData(inputBuffer, 0)

                    if (mExtractor!!.advance() && sampleSize > 0) {
                        mDecoder!!.queueInputBuffer(inputIndex, 0, sampleSize, mExtractor!!.sampleTime, 0)

                    } else {
                        Log.d(TAG, "InputBuffer BUFFER_FLAG_END_OF_STREAM")
                        mDecoder!!.queueInputBuffer(inputIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        isInput = false
                    }
                }
            }

            val outIndex = mDecoder!!.dequeueOutputBuffer(info, 10000)
            when (outIndex) {
                MediaCodec.INFO_OUTPUT_BUFFERS_CHANGED -> {
                    Log.d(TAG, "INFO_OUTPUT_BUFFERS_CHANGED")
                    mDecoder!!.outputBuffers
                }

                MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> Log.d(TAG, "INFO_OUTPUT_FORMAT_CHANGED format : " + mDecoder!!.outputFormat)

                MediaCodec.INFO_TRY_AGAIN_LATER -> {
                }

                else -> {
                    if (!first) {
                        startWhen = System.currentTimeMillis()
                        first = true
                    }
                    try {
                        val sleepTime = info.presentationTimeUs / 1000 - (System.currentTimeMillis() - startWhen)
                        Log.d(TAG, "info.presentationTimeUs : " + info.presentationTimeUs / 1000 + " playTime: " + (System.currentTimeMillis() - startWhen) + " sleepTime : " + sleepTime)

                        if (sleepTime > 0)
                            Thread.sleep(sleepTime)
                    } catch (e: InterruptedException) {
                        // TODO Auto-generated catch block
                        e.printStackTrace()
                    }

                    mDecoder!!.releaseOutputBuffer(outIndex, true /* Surface init */)
                }
            }//				Log.d(TAG, "INFO_TRY_AGAIN_LATER");

            // All decoded frames have been rendered, we can stop playing now
            if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) {
                Log.d(TAG, "OutputBuffer BUFFER_FLAG_END_OF_STREAM")
                break
            }
        }

        mDecoder!!.stop()
        mDecoder!!.release()
        mExtractor!!.release()
    }

    fun close() {
        eosReceived = true
    }
}
