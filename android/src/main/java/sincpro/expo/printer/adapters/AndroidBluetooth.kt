package sincpro.expo.printer.adapters

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.util.Log
import sincpro.expo.printer.domain.BluetoothDevice
import sincpro.expo.printer.domain.BluetoothTransport
import android.bluetooth.BluetoothDevice as AndroidDevice

private const val TAG = "SincproPrinter"

/**
 * Context: the Bixolon SDK never reports a printer-side drop (power off, out of range), so the
 * link is watched through the system ACL broadcast for the connected MAC address.
 */
class AndroidBluetooth(private val context: Context) {
    private val adapter: BluetoothAdapter? =
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    private var linkReceiver: BroadcastReceiver? = null

    private fun transportOf(type: Int): BluetoothTransport =
        when (type) {
            AndroidDevice.DEVICE_TYPE_CLASSIC, AndroidDevice.DEVICE_TYPE_DUAL -> BluetoothTransport.CLASSIC
            AndroidDevice.DEVICE_TYPE_LE -> BluetoothTransport.BLE
            else -> BluetoothTransport.UNKNOWN
        }

    private fun deviceOf(intent: Intent): AndroidDevice? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(AndroidDevice.EXTRA_DEVICE, AndroidDevice::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(AndroidDevice.EXTRA_DEVICE)
        }

    /** Empty when Bluetooth is off or the BLUETOOTH_CONNECT permission is missing (1.x behaviour). */
    @SuppressLint("MissingPermission")
    fun pairedDevices(): List<BluetoothDevice> =
        try {
            adapter?.bondedDevices.orEmpty().map { device ->
                BluetoothDevice(device.name ?: "Unknown", device.address, transportOf(device.type))
            }
        } catch (error: SecurityException) {
            Log.w(TAG, "Paired devices unavailable: missing Bluetooth permission", error)
            emptyList()
        }

    fun watchLink(address: String, onLost: () -> Unit) {
        unwatchLink()
        val receiver =
            object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    if (deviceOf(intent)?.address.equals(address, ignoreCase = true)) onLost()
                }
            }
        val filter = IntentFilter(AndroidDevice.ACTION_ACL_DISCONNECTED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
        linkReceiver = receiver
    }

    fun unwatchLink() {
        linkReceiver?.let { runCatching { context.unregisterReceiver(it) } }
        linkReceiver = null
    }
}
