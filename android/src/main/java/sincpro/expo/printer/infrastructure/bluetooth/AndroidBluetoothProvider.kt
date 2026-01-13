package sincpro.expo.printer.infrastructure.bluetooth

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import sincpro.expo.printer.domain.BluetoothDeviceInfo
import sincpro.expo.printer.domain.BluetoothDeviceType
import sincpro.expo.printer.domain.BluetoothException
import sincpro.expo.printer.domain.IBluetoothProvider

/**
 * INFRASTRUCTURE - Android Bluetooth Provider
 *
 * Pure wrapper over Android Bluetooth APIs.
 * NO business logic - only Android SDK calls.
 *
 * This is the ONLY place where we import android.bluetooth.*
 */
class AndroidBluetoothProvider(
    context: Context,
) : IBluetoothProvider {
    private val bluetoothManager: BluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager

    private val bluetoothAdapter: BluetoothAdapter? =
        bluetoothManager?.adapter

    override fun isSupported(): Boolean = bluetoothAdapter != null

    override fun isEnabled(): Boolean =
        try {
            bluetoothAdapter?.isEnabled == true
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Error checking Bluetooth status", e)
            false
        }

    override fun getPairedDevices(): Result<List<BluetoothDeviceInfo>> {
        return try {
            if (bluetoothAdapter == null) {
                return Result.failure(BluetoothNotSupportedException())
            }

            if (!bluetoothAdapter.isEnabled) {
                return Result.failure(BluetoothDisabledException())
            }

            val pairedDevices = bluetoothAdapter.bondedDevices
            val deviceList =
                pairedDevices.map { device ->
                    BluetoothDeviceInfo(
                        name = device.name ?: "Unknown Device",
                        address = device.address,
                        type = mapDeviceType(device.type),
                        isPrinter = isPrinterDevice(device.name),
                    )
                }

            Result.success(deviceList)
        } catch (e: SecurityException) {
            Log.e(this::class.simpleName, "Bluetooth permission denied", e)
            Result.failure(BluetoothPermissionDeniedException())
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Error getting paired devices", e)
            Result.failure(e)
        }
    }

    override fun startDiscovery(): Result<Boolean> {
        return try {
            if (bluetoothAdapter == null) {
                return Result.failure(BluetoothNotSupportedException())
            }

            if (!bluetoothAdapter.isEnabled) {
                return Result.failure(BluetoothDisabledException())
            }

            val started = bluetoothAdapter.startDiscovery()
            Result.success(started)
        } catch (e: SecurityException) {
            Log.e(this::class.simpleName, "Bluetooth permission denied", e)
            Result.failure(BluetoothPermissionDeniedException())
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Error starting discovery", e)
            Result.failure(e)
        }
    }

    override fun stopDiscovery(): Result<Boolean> =
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
            Result.success(true)
        } catch (e: SecurityException) {
            Log.e(this::class.simpleName, "Bluetooth permission denied", e)
            Result.failure(BluetoothPermissionDeniedException())
        } catch (e: Exception) {
            Log.e(this::class.simpleName, "Error stopping discovery", e)
            Result.failure(e)
        }

    override fun isDiscovering(): Boolean =
        try {
            bluetoothAdapter?.isDiscovering == true
        } catch (e: Exception) {
            false
        }

    private fun mapDeviceType(type: Int): BluetoothDeviceType =
        when (type) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> BluetoothDeviceType.CLASSIC
            BluetoothDevice.DEVICE_TYPE_LE -> BluetoothDeviceType.LE
            BluetoothDevice.DEVICE_TYPE_DUAL -> BluetoothDeviceType.DUAL
            else -> BluetoothDeviceType.UNKNOWN
        }

    private fun isPrinterDevice(name: String?): Boolean {
        if (name == null) return false
        val printerKeywords =
            listOf(
                "bixolon",
                "printer",
                "spp",
                "srp",
                "zebra",
                "epson",
                "star",
                "tsc",
                "xprinter",
                "pos",
                "thermal",
            )
        val lowerName = name.lowercase()
        return printerKeywords.any { lowerName.contains(it) }
    }
}
