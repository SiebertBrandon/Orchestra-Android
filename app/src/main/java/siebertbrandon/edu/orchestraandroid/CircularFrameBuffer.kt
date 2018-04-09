package siebertbrandon.edu.orchestraandroid

import java.lang.Math.abs
import java.nio.ByteBuffer


class CircularFrameBuffer (val size: Int) {
    enum class BufferStatus {
        SPOTS_EMPTY, NEGATIVE_BALANCE, BALANCED, POSITIVE_BALANCE, UNKNOWN
    }
    var buffer = arrayOfNulls<Pair<ByteBuffer, Int>?>(size)
    //var tailIndex = 0
    var head = 0
    var cur = 0
    var fill = 0
    var releaseByteBuffer : Pair<ByteBuffer, Int>? = null
    var acceptingBuffers = true

    fun add(newBuffer : Pair<ByteBuffer, Int>) : Boolean {

        // Return if the buffer is still full
        if (!acceptingBuffers) {
            return false
        }

        // Only allow an add if the buffer is not balanced
        when (checkBalance()){

            // TODO: This case does not deal with an arbitrary null element
            BufferStatus.SPOTS_EMPTY -> {
                fill = (fill + 1) % buffer.size
                if (buffer[fill] != null) {
                    releaseByteBuffer = buffer[fill]
                    acceptingBuffers = false
                }
                buffer[fill] = newBuffer
                return true
            }
            BufferStatus.NEGATIVE_BALANCE -> {
                head = (head + 1) % buffer.size
                buffer[head] = newBuffer
                return true
            }
            BufferStatus.BALANCED, BufferStatus.POSITIVE_BALANCE -> {
                return false
            }
            else -> {
                return false
            }
        }
    }

    fun setCurrentFrame(relativeFrameNumber : Int) {
        cur = (cur + relativeFrameNumber) % buffer.size
    }

    fun releaseAnyFinishedBuffer () : Pair<ByteBuffer, Int>? {
        acceptingBuffers = true
        return releaseByteBuffer
    }

    fun checkBalance() : BufferStatus {

        if (buffer.contains(null)) {
            return BufferStatus.SPOTS_EMPTY
        }
        when {
            (abs(cur - head) < buffer.size / 2) -> return BufferStatus.NEGATIVE_BALANCE
            (abs(cur - head) == buffer.size / 2) -> return BufferStatus.BALANCED
            (abs(cur - head) > buffer.size / 2) -> return BufferStatus.POSITIVE_BALANCE
        }
        return BufferStatus.UNKNOWN
    }

}