package com.sincpro.printer.infrastructure

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer as AndroidPdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Base64
import android.util.Log
import java.io.File
import java.io.FileOutputStream

object PdfRenderer {
    
    private const val TAG = "PdfRenderer"

    fun renderPageToBitmap(base64Data: String, page: Int, targetWidth: Int): Bitmap? {
        return try {
            val pdfBytes = Base64.decode(base64Data, Base64.DEFAULT)
            renderPdfPageToBitmap(pdfBytes, page, targetWidth)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to decode Base64 PDF: ${e.message}")
            null
        }
    }

    fun getPageCount(base64Data: String): Int {
        return try {
            val pdfBytes = Base64.decode(base64Data, Base64.DEFAULT)
            getPdfPageCount(pdfBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get page count: ${e.message}")
            0
        }
    }

    private fun renderPdfPageToBitmap(pdfBytes: ByteArray, page: Int, targetWidth: Int): Bitmap? {
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: AndroidPdfRenderer? = null

        return try {
            tempFile = File.createTempFile("pdf_render_", ".pdf")
            FileOutputStream(tempFile).use { it.write(pdfBytes) }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = AndroidPdfRenderer(pfd)

            val pageIndex = page - 1
            if (pageIndex < 0 || pageIndex >= renderer.pageCount) {
                Log.e(TAG, "Invalid page number: $page (total pages: ${renderer.pageCount})")
                return null
            }

            renderer.openPage(pageIndex).use { pdfPage ->
                val scale = targetWidth.toFloat() / pdfPage.width
                val scaledHeight = (pdfPage.height * scale).toInt()

                val bitmap = Bitmap.createBitmap(targetWidth, scaledHeight, Bitmap.Config.ARGB_8888)
                pdfPage.render(bitmap, null, null, AndroidPdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                
                Log.d(TAG, "Rendered page $page: ${bitmap.width}x${bitmap.height}")
                bitmap
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to render PDF page: ${e.message}")
            null
        } finally {
            renderer?.close()
            pfd?.close()
            tempFile?.delete()
        }
    }

    private fun getPdfPageCount(pdfBytes: ByteArray): Int {
        var tempFile: File? = null
        var pfd: ParcelFileDescriptor? = null
        var renderer: AndroidPdfRenderer? = null

        return try {
            tempFile = File.createTempFile("pdf_count_", ".pdf")
            FileOutputStream(tempFile).use { it.write(pdfBytes) }

            pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = AndroidPdfRenderer(pfd)
            renderer.pageCount
        } catch (e: Exception) {
            Log.e(TAG, "Failed to count PDF pages: ${e.message}")
            0
        } finally {
            renderer?.close()
            pfd?.close()
            tempFile?.delete()
        }
    }
}
