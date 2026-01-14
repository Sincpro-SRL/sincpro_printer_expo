package com.sincpro.printer.infrastructure

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.sincpro.printer.domain.BluetoothDevice
import com.sincpro.printer.domain.BluetoothType
import com.sincpro.printer.domain.IBluetooth

class AndroidBluetoothProvider(context: Context) : IBluetooth {

    private val adapter: BluetoothAdapter? = 
        (context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager)?.adapter

    override fun isSupported() = adapter != null

    override fun isEnabled() = adapter?.isEnabled == true

    @SuppressLint("MissingPermission")
    override fun getPairedDevices(): Result<List<BluetoothDevice>> {
        return try {
            val devices = adapter?.bondedDevices?.map { device ->
                BluetoothDevice(
                    name = device.name ?: "Unknown",
                    address = device.address,
                    type = mapDeviceType(device.type),
                    isPrinter = isPrinterDevice(device.name),
                    rssi = null
                )
            } ?: emptyList()
            Result.success(devices)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun startDiscovery(): Result<Boolean> {
        return try {
            Result.success(adapter?.startDiscovery() == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun stopDiscovery(): Result<Boolean> {
        return try {
            Result.success(adapter?.cancelDiscovery() == true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    override fun isDiscovering() = adapter?.isDiscovering == true

    private fun isPrinterDevice(name: String?): Boolean {
        if (name == null) return false
        val keywords = listOf("printer", "bixolon", "spp", "sppr", "zebra", "tsc", "honeywell")
        return keywords.any { name.lowercase().contains(it) }
    }
    
    private fun mapDeviceType(androidType: Int): BluetoothType {
        return when (androidType) {
            android.bluetooth.BluetoothDevice.DEVICE_TYPE_CLASSIC -> BluetoothType.CLASSIC
            android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE -> BluetoothType.BLE
            android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL -> BluetoothType.DUAL
            else -> BluetoothType.UNKNOWN
        }
    }
}
