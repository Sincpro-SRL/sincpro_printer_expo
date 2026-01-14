package com.sincpro.printer.domain

import android.graphics.Bitmap

interface IPrinter {
    suspend fun connect(config: ConnectionConfig): Result<Unit>
    suspend fun disconnect(): Result<Unit>
    fun isConnected(): Boolean
    suspend fun getStatus(): Result<PrinterStatus>
    suspend fun configure(config: PrinterConfig): Result<Unit>
    suspend fun getInfo(): Result<PrinterInfo>
    fun getDpi(): Int
    suspend fun beginTransaction(media: MediaConfig): Result<Unit>
    suspend fun drawText(text: String, x: Int, y: Int, style: TextStyle): Result<Unit>
    suspend fun drawQR(data: String, x: Int, y: Int, size: Int): Result<Unit>
    suspend fun drawBarcode(data: String, x: Int, y: Int, type: BarcodeType, width: Int, height: Int): Result<Unit>
    suspend fun drawBitmap(bitmap: Bitmap, x: Int, y: Int): Result<Unit>
    suspend fun endTransaction(copies: Int = 1): Result<Unit>
    suspend fun feedPaper(dots: Int): Result<Unit>
    suspend fun cutPaper(): Result<Unit>
}

enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    ERROR
}

data class PrinterStatus(
    val connectionState: ConnectionState,
    val hasPaper: Boolean,
    val isCoverOpen: Boolean,
    val isOverheated: Boolean,
    val isCutterJammed: Boolean,
    val isRibbonError: Boolean,
    val isPrinting: Boolean,
    val isPaused: Boolean,
    val isBufferBuilding: Boolean,
    val hasError: Boolean,
    val errorMessage: String? = null
) {
    val isConnected: Boolean
        get() = connectionState == ConnectionState.CONNECTED
    
    val isReady: Boolean
        get() = isConnected && hasPaper && !hasError && !isPrinting && !isCoverOpen && !isOverheated
    
    companion object {
        fun disconnected() = PrinterStatus(
            connectionState = ConnectionState.DISCONNECTED,
            hasPaper = false,
            isCoverOpen = false,
            isOverheated = false,
            isCutterJammed = false,
            isRibbonError = false,
            isPrinting = false,
            isPaused = false,
            isBufferBuilding = false,
            hasError = false
        )
        
        fun error(message: String) = PrinterStatus(
            connectionState = ConnectionState.ERROR,
            hasPaper = false,
            isCoverOpen = false,
            isOverheated = false,
            isCutterJammed = false,
            isRibbonError = false,
            isPrinting = false,
            isPaused = false,
            isBufferBuilding = false,
            hasError = true,
            errorMessage = message
        )
    }
}

data class TextStyle(
    val fontSize: FontSize = FontSize.MEDIUM,
    val bold: Boolean = false,
    val alignment: Alignment = Alignment.LEFT
)

enum class FontSize { SMALL, MEDIUM, LARGE, XLARGE }

enum class Alignment { LEFT, CENTER, RIGHT }

enum class BarcodeType { CODE128, CODE39, EAN13, EAN8, UPCA, UPCE, CODE93, CODABAR }
