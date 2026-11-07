package sincpro.expo.printer.infrastructure

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.util.Base64

object BitmapDecoder {
    fun stripDataUri(base64: String): String {
        val comma = base64.indexOf(',')
        return if (base64.startsWith("data:") && comma != -1) base64.substring(comma + 1) else base64
    }

    fun decodeBytes(base64: String): ByteArray = Base64.decode(stripDataUri(base64), Base64.DEFAULT)

    /** Context: transparent pixels print black on thermal heads, so the image is flattened on white. */
    fun decode(base64: String): Bitmap? {
        val bytes = runCatching { decodeBytes(base64) }.getOrNull() ?: return null
        val source = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        if (source.config == Bitmap.Config.RGB_565) return source

        val flattened = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.RGB_565)
        Canvas(flattened).apply {
            drawColor(Color.WHITE)
            drawBitmap(source, 0f, 0f, null)
        }
        source.recycle()
        return flattened
    }
}
