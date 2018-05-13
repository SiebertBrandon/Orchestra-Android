package siebertbrandon.edu.orchestraandroid

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager

/**
 * Created by Brandon on 3/9/2018.
 */
class BluetoothControlSettings {
    companion object {
        var enabled: Boolean = false
        var mBluetoothAdapter : BluetoothAdapter? = null
        var mBluetoothManager : BluetoothManager? = null
    }
}