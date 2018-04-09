package siebertbrandon.edu.orchestraandroid

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.util.Log
import java.io.FileInputStream
import java.io.IOException
import java.nio.ByteBuffer
import kotlin.concurrent.thread

class AsyncVideoDecode {
    companion object {
        @Volatile var FRAME_BUFFER = CircularFrameBuffer(10)
    }
    private val videoTag = "video/"
    private val debugTag = "[VD]"
    private lateinit var mExtractor: MediaExtractor
    private lateinit var mDecoder: MediaCodec

    fun init(filepath: String): Boolean {
        try {
            val fd = FileInputStream(filepath).getFD()
            mExtractor = MediaExtractor()
            mExtractor.setDataSource(fd)

            val format = mExtractor.getTrackFormat(0)
            val mime = format.getString(MediaFormat.KEY_MIME)

            if (mime.startsWith(videoTag)) {
                mExtractor.selectTrack(0)
                mDecoder = MediaCodec.createDecoderByType(mime)
                try {
                    Log.d(debugTag, "Set Video Format: $format")
                    mDecoder.configure(format, null, null, 0 /* Decoder */)

                } catch (e: IllegalStateException) {
                    Log.e(debugTag, "IllegalStateException: codec '$mime' failed configuration. $e")
                    return false
                } catch (e: IllegalArgumentException) {
                    Log.e(debugTag, "IllegalArgumentException: codec '$mime' failed configuration. $e")
                    return false
                } catch (e: MediaCodec.CryptoException) {
                    Log.e(debugTag, "CryptoException: codec '$mime' failed configuration. $e")
                    return false
                } catch (e: MediaCodec.CodecException) {
                    Log.e(debugTag, "CodecException: codec '$mime' failed configuration. $e")
                    return false
                }
                mDecoder.setCallback(object: MediaCodec.Callback() {
                    override fun onOutputFormatChanged(codec: MediaCodec?, format: MediaFormat?) {
                        // Do nothing
                    }

                    override fun onInputBufferAvailable(mc : MediaCodec, inputBufferId: Int) {

                        val inputBuffer = mDecoder.getInputBuffer(inputBufferId)
                        val sampleSize = mExtractor.readSampleData(inputBuffer, 0)

                        if (mExtractor.advance() && sampleSize > 0) {
                            mDecoder.queueInputBuffer(inputBufferId, 0, sampleSize, mExtractor.sampleTime, 0)

                        } else {
                            Log.d(debugTag, "InputBuffer BUFFER_FLAG_END_OF_STREAM")
                            mDecoder.queueInputBuffer(inputBufferId, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        }
                    }

                    override fun onOutputBufferAvailable(mc : MediaCodec, outputBufferId: Int, info: MediaCodec.BufferInfo?) {

                        val outputBuffer = mDecoder.getOutputBuffer(outputBufferId)
                        val bufferFormat = mDecoder.getOutputFormat(outputBufferId)

                        // This is where we want to copy the contents of the output buffer into the shared buffer resource
                        while (!FRAME_BUFFER.add(newBuffer = Pair(outputBuffer, outputBufferId))) {
                            Log.d(debugTag, "Unable to queue frame. Waiting to try again")
                            Thread.sleep(30)
                            val releaseInfo : Pair<ByteBuffer, Int>? = FRAME_BUFFER.releaseAnyFinishedBuffer()

                            releaseInfo ?. let {
                                val (releaseBuffer, releaseID) = releaseInfo
                                mDecoder.releaseOutputBuffer(releaseID, false)
                            }
                        }
                    }

                    override fun onError(codec: MediaCodec?, e: MediaCodec.CodecException?) {
                        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
                    }
                })
            }
                mDecoder.start()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }
        /*
            MediaCodec codec = MediaCodec.createByCodecName(name);
            MediaFormat mOutputFormat; // member variable


                @Override
                void onOutputBufferAvailable(MediaCodec mc, int outputBufferId, …) {
                ByteBuffer outputBuffer = codec.getOutputBuffer(outputBufferId);
                MediaFormat bufferFormat = codec.getOutputFormat(outputBufferId); // option A
                // bufferFormat is equivalent to mOutputFormat
                // outputBuffer is ready to be processed or rendered.
                …
                codec.releaseOutputBuffer(outputBufferId, …);
            }

                @Override
                void onOutputFormatChanged(MediaCodec mc, MediaFormat format) {
                // Subsequent data will conform to new format.
                // Can ignore if using getOutputFormat(outputBufferId)
                mOutputFormat = format; // option B
            }

                @Override
                void onError(…) {
                …
            }
            });
            codec.configure(format, …);
            mOutputFormat = codec.getOutputFormat(); // option B
            codec.start();
            // wait for processing to complete
            codec.stop();
            codec.release()
            */
}