package siebertbrandon.edu.orchestraandroid

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable
import android.util.Log
import java.lang.Math.abs
import java.util.*


class CircularFrameBuffer(val size: Int) : Parcelable {

    private enum class BufferStatus {
        SPOTS_EMPTY, NEGATIVE_BALANCE, BALANCED, POSITIVE_BALANCE, UNKNOWN
    }

    private var buffer = arrayOfNulls<Bitmap?>(size)
    private var queue: Queue<Pair<Bitmap, Int>> = LinkedList<Pair<Bitmap, Int>>()
    private var head = 0
    private var cur = 0
    private var fill = 0

    constructor(parcel: Parcel) : this(parcel.readInt()) {
        buffer = parcel.createTypedArray(Bitmap.CREATOR)
        head = parcel.readInt()
        cur = parcel.readInt()
        fill = parcel.readInt()
    }

    fun add(newBufferPair: Pair<Bitmap, Int>) {
        //Log.d("[CFB]", "Adding new Buffer to queue")
        queue.add(newBufferPair)

        var status = checkBalance()
        rebalance()
        status = checkBalance()

        if (FullScreenVideoView.DEBUG) Log.d("[CFB]", "ADD_EVENT: status: $status, queue size: ${queue.size}, cur: " +
                "$cur," +
                " head:" +
                " $head, fill: $fill")
        var footprint: String = ""
        for (i in 0 until buffer.size) {
            if (i == cur) {
                footprint += "cur_"
            } else if (i == head) {
                footprint += "head"
            } else if (i == fill) {
                footprint += "fill"
            } else if (buffer[i] is Bitmap) {
                footprint += "frme"
            } else {
                footprint += "null"
            }
            if (i < buffer.size - 1) footprint += " -> "
        }
        if (FullScreenVideoView.DEBUG) Log.d("[CFB]", footprint)
    }

    fun setCurrentFrame(relativeFrameNumber: Int) {
        if ((cur + relativeFrameNumber) % buffer.size != head)
            cur = ((cur + relativeFrameNumber) % buffer.size)
    }

    fun render(): Bitmap? {
        val frame: Bitmap? = buffer[cur]
        if (frame != null && (cur + 1) % buffer.size != head) {
            setCurrentFrame(1)
            rebalance()
        }
        return frame
    }

    private fun checkBalance(): BufferStatus {

        if (buffer.contains(null)) {

            // Don't render until all spots are full
            return BufferStatus.SPOTS_EMPTY
        }
        when {

            (head == cur) -> return BufferStatus.POSITIVE_BALANCE

            ((head > cur) && abs(cur - head) > abs(cur - (head - buffer.size)))
            -> return BufferStatus.POSITIVE_BALANCE

            ((head > cur) && abs(cur - head) < abs(cur - (head - buffer.size)))
            -> return BufferStatus.NEGATIVE_BALANCE

            ((head > cur) && abs(cur - head) == abs(cur - (head - buffer.size)))
            -> return BufferStatus.BALANCED

            ((head < cur) && abs(cur - head) < abs(cur - (head + buffer.size)))
            -> return BufferStatus.POSITIVE_BALANCE

            ((head < cur) && abs(cur - head) > abs(cur - (head + buffer.size)))
            -> return BufferStatus.NEGATIVE_BALANCE

            ((head < cur) && abs(cur - head) == abs(cur - (head + buffer.size)))
            -> return BufferStatus.BALANCED

            else -> return BufferStatus.UNKNOWN
        }
    }

    private fun rebalance() {

        if (queue.isEmpty()) {
            return
        }

        when (checkBalance()){
            BufferStatus.SPOTS_EMPTY -> {
                val (queuedBuffer: Bitmap, queuedIndex: Int) = queue.remove()
                buffer[fill] = queuedBuffer
                fill = (fill + 1) % buffer.size
                FullScreenVideoView.MEDIA_CODEC.releaseOutputBuffer(queuedIndex, false)
            }
            BufferStatus.NEGATIVE_BALANCE -> {
                if ((head + 1) % buffer.size != cur) {
                    val (queuedBuffer: Bitmap, queuedIndex: Int) = queue.remove()
                    head = (head + 1) % buffer.size
                    buffer[head] = queuedBuffer
                    FullScreenVideoView.MEDIA_CODEC.releaseOutputBuffer(queuedIndex, false)
                }
            }
            BufferStatus.BALANCED, BufferStatus.UNKNOWN, BufferStatus.POSITIVE_BALANCE -> {
            }
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(size)
        parcel.writeTypedArray(buffer, flags)
        parcel.writeInt(head)
        parcel.writeInt(cur)
        parcel.writeInt(fill)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<CircularFrameBuffer> {
        override fun createFromParcel(parcel: Parcel): CircularFrameBuffer {
            return CircularFrameBuffer(parcel)
        }

        override fun newArray(size: Int): Array<CircularFrameBuffer?> {
            return arrayOfNulls(size)
        }
    }

}