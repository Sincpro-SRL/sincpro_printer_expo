package com.sincpro.printer.service.bixolon

import com.sincpro.printer.domain.BluetoothDevice
import com.sincpro.printer.domain.ConnectionConfig
import com.sincpro.printer.domain.IBluetooth
import com.sincpro.printer.domain.IPrinter
import com.sincpro.printer.domain.PrinterConfig
import com.sincpro.printer.domain.PrinterInfo
import com.sincpro.printer.domain.PrinterStatus

class BixolonConnectivityService(
    private val printer: IPrinter,
    private val bluetooth: IBluetooth
) {
    private var defaultConfig: PrinterConfig = PrinterConfig.DEFAULT_80MM_RECEIPT

    suspend fun connectBluetooth(
        address: String,
        timeoutMs: Long = 10000,
        config: PrinterConfig = defaultConfig
    ): Result<Unit> {
        return printer.connect(ConnectionConfig.bluetooth(address, timeoutMs))
            .onSuccess { printer.configure(config) }
    }

    suspend fun connectWifi(
        ip: String,
        port: Int = 9100,
        timeoutMs: Long = 10000,
        config: PrinterConfig = defaultConfig
    ): Result<Unit> {
        return printer.connect(ConnectionConfig.wifi(ip, port, timeoutMs))
            .onSuccess { printer.configure(config) }
    }

    suspend fun connectUsb(config: PrinterConfig = defaultConfig): Result<Unit> {
        return printer.connect(ConnectionConfig.usb())
            .onSuccess { printer.configure(config) }
    }

    fun setDefaultConfig(config: PrinterConfig) {
        defaultConfig = config
    }

    suspend fun disconnect(): Result<Unit> {
        return printer.disconnect()
    }

    fun isConnected(): Boolean = printer.isConnected()

    suspend fun getStatus(): Result<PrinterStatus> = printer.getStatus()

    suspend fun getInfo(): Result<PrinterInfo> = printer.getInfo()

    fun getDpi(): Int = printer.getDpi()

    fun getPairedDevices(): Result<List<BluetoothDevice>> = bluetooth.getPairedDevices()

    fun getPairedPrinters(): Result<List<BluetoothDevice>> {
        return bluetooth.getPairedDevices().map { devices ->
            devices.filter { it.isPrinter }
        }
    }
}
