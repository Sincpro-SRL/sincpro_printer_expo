package com.sincpro.printer.domain

interface IBluetooth {
    fun isSupported(): Boolean
    fun isEnabled(): Boolean
    fun getPairedDevices(): Result<List<BluetoothDevice>>
    fun startDiscovery(): Result<Boolean>
    fun stopDiscovery(): Result<Boolean>
    fun isDiscovering(): Boolean
}

enum class BluetoothType { CLASSIC, BLE, DUAL, UNKNOWN }

data class BluetoothDevice(
    val name: String,
    val address: String,
    val type: BluetoothType = BluetoothType.UNKNOWN,
    val isPrinter: Boolean,
    val rssi: Int? = null
) {
    val hasStrongSignal: Boolean
        get() = rssi?.let { it > -60 } ?: false
    
    val hasWeakSignal: Boolean
        get() = rssi?.let { it < -80 } ?: false
}

data class ConnectionConfig(
    val address: String,
    val type: ConnectionType = ConnectionType.BLUETOOTH,
    val port: Int = 9100,
    val timeoutMs: Long = 10000
) {
    companion object {
        fun bluetooth(address: String, timeoutMs: Long = 10000) =
            ConnectionConfig(address, ConnectionType.BLUETOOTH, 0, timeoutMs)

        fun wifi(ip: String, port: Int = 9100, timeoutMs: Long = 10000) =
            ConnectionConfig(ip, ConnectionType.WIFI, port, timeoutMs)

        fun usb() = ConnectionConfig("", ConnectionType.USB, 0, 10000)
    }
}

enum class ConnectionType { BLUETOOTH, WIFI, USB }
