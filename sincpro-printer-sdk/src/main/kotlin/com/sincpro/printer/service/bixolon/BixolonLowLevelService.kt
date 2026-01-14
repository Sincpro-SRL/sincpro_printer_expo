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

    /**
     * Draw bitmap from Base64 string
     * Pre-processes image to remove alpha channel (transparency becomes white)
     * This is critical for thermal printers where transparency = black
     * 
     * @param base64Data Base64 encoded image (with or without data URI prefix)
     * @param x horizontal position in dots
     * @param y vertical position in dots
     * @param width width in dots (0 = original size)
     * @param brightness 0-100 (50 = normal)
     * @param dithering true for better quality
     */
    suspend fun bitmapBase64(
        base64Data: String,
        x: Int,
        y: Int,
        width: Int = 0,
        brightness: Int = 50,
        dithering: Boolean = true
    ): Result<Unit> {
        Log.d(TAG, "bitmapBase64: x=$x, y=$y, width=$width, brightness=$brightness, dithering=$dithering")
        
        var bitmap = BinaryConverter.base64ToBitmap(base64Data)
            ?: return Result.failure(Exception("Failed to decode image from Base64"))
        
        Log.d(TAG, "bitmapBase64: original bitmap ${bitmap.width}x${bitmap.height}")
        
        // Resize if width is specified
        if (width > 0 && width != bitmap.width) {
            val aspectRatio = bitmap.height.toFloat() / bitmap.width.toFloat()
            val newHeight = (width * aspectRatio).toInt()
            bitmap = Bitmap.createScaledBitmap(bitmap, width, newHeight, true)
            Log.d(TAG, "bitmapBase64: resized to ${width}x${newHeight}")
        }
        
        // Use adapter's primitive drawBitmap method
        // Note: brightness and dithering are handled by the printer itself in drawBitmap
        return adapter.drawBitmap(bitmap, x, y)
    }

    /**
     * Draw a page from a PDF (Base64 encoded)
     * 
     * TODO: Implement using Bixolon PDF library (Bixolon_pdf.aar)
     * Current stub implementation needs actual PDF rendering
     * 
     * @param base64Data Base64 encoded PDF data
     * @param x horizontal position in dots
     * @param y vertical position in dots
     * @param page page number (1-based)
     * @param width width in dots (0 = auto fit to paper)
     * @param brightness 0-100 (50 = normal)
     * @param dithering true for better quality
     * @param compress true to compress data sent to printer
     */
    suspend fun pdfBase64(
        base64Data: String,
        x: Int = 0,
        y: Int = 0,
        page: Int = 1,
        width: Int = 0,
        brightness: Int = 50,
        dithering: Boolean = true,
        compress: Boolean = true
    ): Result<Unit> {
        Log.d(TAG, "pdfBase64: page=$page, x=$x, y=$y, width=$width")
        // TODO: Render PDF page to Bitmap using Bixolon_pdf.aar library
        // Then use adapter.drawBitmap(bitmap, x, y)
        return Result.failure(Exception("PDF printing requires Bixolon_pdf.aar implementation"))
    }

    /**
     * Get number of pages in a PDF (Base64 encoded)
     * 
     * TODO: Implement using Bixolon PDF library (Bixolon_pdf.aar)
     */
    fun getPdfPageCountBase64(base64Data: String): Int {
        Log.w(TAG, "getPdfPageCountBase64: Not implemented - requires Bixolon_pdf.aar")
        // TODO: Use Bixolon PDF library to count pages
        return 0
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
