package sincpro.expo.printer.entrypoint

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.os.Bundle
import expo.modules.core.interfaces.ReactActivityLifecycleListener

class PrinterLifecycleListener : ReactActivityLifecycleListener {
    private var bluetoothAdapter: BluetoothAdapter? = null

    override fun onCreate(
        activity: Activity,
        savedInstanceState: Bundle?,
    ) {
        val bluetoothManager = activity.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
        bluetoothAdapter = bluetoothManager?.adapter
    }

    override fun onPause(activity: Activity) {
        cleanupBluetoothResources()
    }

    override fun onDestroy(activity: Activity) {
        cleanupBluetoothResources()
        bluetoothAdapter = null
    }

    private fun cleanupBluetoothResources() {
        try {
            bluetoothAdapter?.cancelDiscovery()
        } catch (e: SecurityException) {
            android.util.Log.w(this::class.simpleName, "No permission to cancel Bluetooth discovery", e)
        }
    }
}
