package com.sincpro.printer.service.bixolon

import android.graphics.Bitmap
import android.util.Log
import com.sincpro.printer.adapter.BixolonPrinterAdapter
import com.sincpro.printer.domain.BarcodeType
import com.sincpro.printer.domain.IPrinter
import com.sincpro.printer.domain.MediaConfig
import com.sincpro.printer.domain.PrinterConfig
import com.sincpro.printer.domain.TextStyle
import com.sincpro.printer.infrastructure.BinaryConverter
import com.sincpro.printer.infrastructure.PdfRenderer

class BixolonLowLevelService(private val adapter: BixolonPrinterAdapter) {

    private val printer: IPrinter get() = adapter

    companion object {
        private const val TAG = "BixolonLowLevelService"
    }

    suspend fun configure(config: PrinterConfig): Result<Unit> {
        return printer.configure(config)
    }

    suspend fun begin(media: MediaConfig = MediaConfig.continuous80mm()): Result<Unit> {
        return printer.beginTransaction(media)
    }

    suspend fun text(text: String, x: Int, y: Int, style: TextStyle = TextStyle()): Result<Unit> {
        return printer.drawText(text, x, y, style)
    }

    suspend fun qr(data: String, x: Int, y: Int, size: Int = 5): Result<Unit> {
        return printer.drawQR(data, x, y, size)
    }

    suspend fun barcode(
        data: String, x: Int, y: Int,
        type: BarcodeType = BarcodeType.CODE128,
        width: Int = 2, height: Int = 60
    ): Result<Unit> {
        return printer.drawBarcode(data, x, y, type, width, height)
    }

    suspend fun bitmap(image: Bitmap, x: Int, y: Int): Result<Unit> {
        return printer.drawBitmap(image, x, y)
    }

    suspend fun bitmapBase64(
        base64Data: String,
        x: Int,
        y: Int,
        width: Int = 0
    ): Result<Unit> {
        Log.d(TAG, "bitmapBase64: x=$x, y=$y, width=$width")
        
        var bitmap = BinaryConverter.base64ToBitmap(base64Data)
            ?: return Result.failure(Exception("Failed to decode image from Base64"))
        
        if (width > 0 && width != bitmap.width) {
            val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
            val newHeight = (width * aspectRatio).toInt()
            bitmap = Bitmap.createScaledBitmap(bitmap, width, newHeight, true)
        }
        
        return adapter.drawBitmap(bitmap, x, y)
    }

    suspend fun pdfBase64(
        base64Data: String,
        x: Int = 0,
        y: Int = 0,
        page: Int = 1,
        width: Int = 0
    ): Result<Unit> {
        Log.d(TAG, "pdfBase64: page=$page, x=$x, y=$y, width=$width")
        
        val targetWidth = if (width > 0) width else adapter.getDpi() * 3 // Default ~3 inches
        
        val bitmap = PdfRenderer.renderPageToBitmap(base64Data, page, targetWidth)
            ?: return Result.failure(Exception("Failed to render PDF page $page"))
        
        return adapter.drawBitmap(bitmap, x, y)
    }

    fun getPdfPageCount(base64Data: String): Int {
        return PdfRenderer.getPageCount(base64Data)
    }

    suspend fun end(copies: Int = 1): Result<Unit> {
        return printer.endTransaction(copies)
    }

    suspend fun feed(dots: Int): Result<Unit> {
        return printer.feedPaper(dots)
    }

    suspend fun cut(): Result<Unit> {
        return printer.cutPaper()
    }
}
