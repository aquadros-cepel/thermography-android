package com.tech.thermography.android.flir

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import com.flir.thermalsdk.image.ThermalImage
import com.flir.thermalsdk.log.ThermalLog
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Captures the entire screen (GL + Compose UI) and saves it as a FLIR overlay.
 * Uses PixelCopy API (Android 8.0+) to capture the window surface.
 */
class SnapshotManager @Inject constructor() {

    companion object {
        private const val TAG = "SnapshotManager"
    }

    interface Callback {
        fun onSuccess(file: File)
        fun onError(error: String, exception: Throwable? = null)
    }
    
    /**
     * Captures the ENTIRE SCREEN (GL thermal + Compose UI) synchronously and returns a JavaImageBuffer for overlay.
     * This combines: GL framebuffer (thermal stream) + Compose UI (toolbar, temperature bar, Bx1/Bx2).
     * 
     * Strategy:
     * 1. Capture GL framebuffer with glReadPixels (thermal background)
     * 2. Capture Compose UI with PixelCopy (overlay elements)
     * 3. Composite both images (thermal as background + UI on top)
     * 
     * This method MUST be called from the GL thread while ThermalImage is still valid.
     * 
     * @param activity The activity whose window will be captured
     * @param width GL surface width
     * @param height GL surface height
     * @return JavaImageBuffer with the complete composite (thermal + UI), or null if failed
     */
    fun captureGLFramebufferAsOverlay(activity: Activity, width: Int, height: Int): com.flir.thermalsdk.image.JavaImageBuffer? {
        if (width <= 0 || height <= 0) {
            ThermalLog.e(TAG, "Invalid surface size: ${width}x$height")
            return null
        }
        
        try {
            ThermalLog.d(TAG, "Capturing composite: GL thermal (${width}x$height) + Compose UI")
            
            // ===== STEP 1: Capture GL framebuffer (thermal stream) =====
            ThermalLog.d(TAG, "Step 1: Capturing GL framebuffer (thermal)...")
            val thermalBitmap = captureGLFramebuffer(width, height)
            if (thermalBitmap == null) {
                ThermalLog.e(TAG, "Failed to capture GL framebuffer")
                return null
            }
            
            // ===== STEP 2: Capture Compose UI with PixelCopy =====
            ThermalLog.d(TAG, "Step 2: Capturing Compose UI with PixelCopy...")
            val uiBitmap = captureComposeUI(activity, width, height)
            
            // ===== STEP 3: Composite both images =====
            ThermalLog.d(TAG, "Step 3: Compositing thermal + UI...")
            val compositeBitmap = if (uiBitmap != null) {
                // Composite: thermal background + UI overlay
                compositeBitmaps(thermalBitmap, uiBitmap)
            } else {
                // Fallback: use only thermal if UI capture failed
                ThermalLog.w(TAG, "UI capture failed, using only thermal")
                thermalBitmap
            }
            
            // ===== DEBUG: Save composite to verify =====
            try {
                val debugFile = File(
                    android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES),
                    "debug_composite_${System.currentTimeMillis()}.jpg"
                )
                debugFile.outputStream().use { out ->
                    compositeBitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
                }
                ThermalLog.i(TAG, "🔍 DEBUG: Composite saved to: ${debugFile.absolutePath}")
                ThermalLog.i(TAG, "🔍 DEBUG: Should show thermal + toolbar + temperature bar + Bx1 + Bx2")
            } catch (e: Exception) {
                ThermalLog.w(TAG, "Failed to save debug composite: ${e.message}")
            }
            // ===== END DEBUG =====
            
            // ===== STEP 4: Convert to JavaImageBuffer =====
            val imageSize = compositeBitmap.byteCount
            val rawBuffer = ByteBuffer.allocate(imageSize)
            compositeBitmap.copyPixelsToBuffer(rawBuffer)
            rawBuffer.rewind()
            
            val overlay = createJavaImageBuffer(rawBuffer, compositeBitmap.width, compositeBitmap.height)
            
            // Clean up bitmaps
            compositeBitmap.recycle()
            if (uiBitmap != null && uiBitmap != compositeBitmap) {
                uiBitmap.recycle()
            }
            
            if (overlay != null) {
                ThermalLog.i(TAG, "✓ Composite (thermal + UI) created as JavaImageBuffer")
            } else {
                ThermalLog.e(TAG, "❌ Failed to create JavaImageBuffer from composite")
            }
            
            return overlay
            
        } catch (e: Exception) {
            ThermalLog.e(TAG, "Failed to capture composite: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Capture GL framebuffer (thermal stream) using glReadPixels.
     * Returns thermal image as Bitmap.
     */
    private fun captureGLFramebuffer(width: Int, height: Int): Bitmap? {
        try {
            // Ensure all GL commands are finished
            android.opengl.GLES20.glFinish()
            android.opengl.GLES20.glPixelStorei(android.opengl.GLES20.GL_PACK_ALIGNMENT, 1)
            
            // Allocate buffer for RGBA data
            val pixelCount = width * height
            val byteCount = pixelCount * 4
            val glReadBuffer = ByteBuffer.allocate(byteCount)
            
            // Read pixels from GL framebuffer
            android.opengl.GLES20.glReadPixels(
                0, 0, width, height,
                android.opengl.GLES20.GL_RGBA,
                android.opengl.GLES20.GL_UNSIGNED_BYTE,
                glReadBuffer
            )
            glReadBuffer.rewind()
            
            // Convert RGBA → ARGB
            val argbPixels = IntArray(pixelCount)
            convertRGBAtoARGB(glReadBuffer, argbPixels, width, height)
            
            // Flip vertical (GL renders upside-down)
            flipVertical(argbPixels, width, height)
            
            // Create Bitmap
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.setPixels(argbPixels, 0, width, 0, 0, width, height)
            
            ThermalLog.d(TAG, "GL framebuffer captured: ${width}x${height}")
            return bitmap
            
        } catch (e: Exception) {
            ThermalLog.e(TAG, "Failed to capture GL framebuffer: ${e.message}")
            return null
        }
    }
    
    /**
     * Capture Compose UI using PixelCopy (synchronously).
     * Returns UI elements as Bitmap, or null if failed.
     */
    private fun captureComposeUI(activity: Activity, targetWidth: Int, targetHeight: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            ThermalLog.w(TAG, "PixelCopy requires Android 8.0+, skipping UI capture")
            return null
        }
        
        try {
            val window = activity.window
            val decorView = window.decorView
            
            val captureWidth = decorView.width
            val captureHeight = decorView.height
            
            if (captureWidth <= 0 || captureHeight <= 0) {
                ThermalLog.w(TAG, "Invalid window size: ${captureWidth}x${captureHeight}")
                return null
            }
            
            // Create bitmap
            val bitmap = Bitmap.createBitmap(captureWidth, captureHeight, Bitmap.Config.ARGB_8888)
            
            // Use CountDownLatch for synchronous operation
            val latch = CountDownLatch(1)
            var pixelCopyResult = -1
            
            // Post to Main thread
            Handler(Looper.getMainLooper()).post {
                try {
                    PixelCopy.request(
                        window,
                        Rect(0, 0, captureWidth, captureHeight),
                        bitmap,
                        { result ->
                            pixelCopyResult = result
                            latch.countDown()
                        },
                        Handler(Looper.getMainLooper())
                    )
                } catch (e: Exception) {
                    ThermalLog.e(TAG, "PixelCopy request failed: ${e.message}")
                    latch.countDown()
                }
            }
            
            // Wait for completion
            val timeout = latch.await(2, TimeUnit.SECONDS)
            
            if (!timeout || pixelCopyResult != PixelCopy.SUCCESS) {
                bitmap.recycle()
                ThermalLog.w(TAG, "PixelCopy failed or timeout, skipping UI")
                return null
            }
            
            // Resize if needed
            val finalBitmap = if (captureWidth != targetWidth || captureHeight != targetHeight) {
                Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true).also {
                    bitmap.recycle()
                }
            } else {
                bitmap
            }
            
            ThermalLog.d(TAG, "Compose UI captured: ${finalBitmap.width}x${finalBitmap.height}")
            return finalBitmap
            
        } catch (e: Exception) {
            ThermalLog.e(TAG, "Failed to capture Compose UI: ${e.message}")
            return null
        }
    }
    
    /**
     * Composite two bitmaps: thermal as background + UI on top.
     * UI pixels with low alpha are treated as transparent (to show thermal underneath).
     */
    private fun compositeBitmaps(thermalBitmap: Bitmap, uiBitmap: Bitmap): Bitmap {
        val width = thermalBitmap.width
        val height = thermalBitmap.height
        
        // Create result bitmap
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(result)
        
        // Draw thermal as background
        canvas.drawBitmap(thermalBitmap, 0f, 0f, null)
        
        // Draw UI on top (will composite automatically based on alpha)
        val paint = android.graphics.Paint()
        canvas.drawBitmap(uiBitmap, 0f, 0f, paint)
        
        ThermalLog.d(TAG, "Composited thermal + UI: ${width}x${height}")
        return result
    }
    
    /**
     * Convert RGBA buffer to ARGB int array.
     */
    private fun convertRGBAtoARGB(buffer: ByteBuffer, outPixels: IntArray, width: Int, height: Int) {
        val pixelCount = width * height
        for (i in 0 until pixelCount) {
            val r = buffer.get().toInt() and 0xFF
            val g = buffer.get().toInt() and 0xFF
            val b = buffer.get().toInt() and 0xFF
            val a = buffer.get().toInt() and 0xFF
            outPixels[i] = (a shl 24) or (r shl 16) or (g shl 8) or b
        }
    }
    
    /**
     * Flip image vertically (GL renders upside-down).
     */
    private fun flipVertical(pixels: IntArray, width: Int, height: Int) {
        val temp = IntArray(width)
        for (y in 0 until height / 2) {
            val topOffset = y * width
            val bottomOffset = (height - 1 - y) * width
            
            // Swap rows
            System.arraycopy(pixels, topOffset, temp, 0, width)
            System.arraycopy(pixels, bottomOffset, pixels, topOffset, width)
            System.arraycopy(temp, 0, pixels, bottomOffset, width)
        }
    }

    /**
     * [DEPRECATED - NOT USED ANYMORE]
     * Captures the full screen and saves it alongside the ThermalImage.
     * Use captureGLFramebufferAsOverlay() instead for synchronous capture.
     */
    @Deprecated("Use captureGLFramebufferAsOverlay() instead", ReplaceWith("captureGLFramebufferAsOverlay(width, height)"))
    fun captureAndSave(
        activity: Activity,
        thermalImage: ThermalImage,
        file: File,
        thermalWidth: Int? = null,
        thermalHeight: Int? = null,
        callback: Callback
    ) {
        // Validate API level
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            callback.onError("PixelCopy requires Android 8.0 (API 26) or higher. Current: ${Build.VERSION.SDK_INT}")
            return
        }

        val window = activity.window
        val decorView = window.decorView

        if (decorView.width == 0 || decorView.height == 0) {
            callback.onError("View not ready for capture (width=${decorView.width}, height=${decorView.height})")
            return
        }

        ThermalLog.d(TAG, "Starting screen capture: ${decorView.width}x${decorView.height}")

        val bitmap = Bitmap.createBitmap(
            decorView.width,
            decorView.height,
            Bitmap.Config.ARGB_8888
        )

        try {
            PixelCopy.request(
                window,
                Rect(0, 0, decorView.width, decorView.height),
                bitmap,
                { result ->
                    handlePixelCopyResult(
                        result,
                        bitmap,
                        thermalImage,
                        file,
                        thermalWidth,
                        thermalHeight,
                        callback
                    )
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            bitmap.recycle()
            ThermalLog.e(TAG, "PixelCopy request failed: ${e.message}")
            callback.onError("Failed to execute PixelCopy", e)
        }
    }
    
    /**
     * [DEPRECATED - NOT USED ANYMORE]
     * This method saves overlay as SEPARATE PNG file which creates 2 files total.
     * Use captureGLFramebufferAsOverlay() instead which creates 1 file with embedded overlay.
     */
    @Deprecated("Creates 2 files. Use captureGLFramebufferAsOverlay() instead", ReplaceWith("captureGLFramebufferAsOverlay(width, height)"))
    fun captureAndAddOverlay(
        activity: Activity,
        thermalFile: File,
        thermalWidth: Int? = null,
        thermalHeight: Int? = null,
        callback: Callback
    ) {
        // Validate API level
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            callback.onError("PixelCopy requires Android 8.0 (API 26) or higher. Current: ${Build.VERSION.SDK_INT}")
            return
        }

        val window = activity.window
        val decorView = window.decorView

        if (decorView.width == 0 || decorView.height == 0) {
            callback.onError("View not ready for capture (width=${decorView.width}, height=${decorView.height})")
            return
        }

        ThermalLog.d(TAG, "Starting screen capture to add overlay: ${decorView.width}x${decorView.height}")

        val bitmap = Bitmap.createBitmap(
            decorView.width,
            decorView.height,
            Bitmap.Config.ARGB_8888
        )

        try {
            PixelCopy.request(
                window,
                Rect(0, 0, decorView.width, decorView.height),
                bitmap,
                { result ->
                    handlePixelCopyForOverlay(
                        result,
                        bitmap,
                        thermalFile,
                        thermalWidth,
                        thermalHeight,
                        callback
                    )
                },
                Handler(Looper.getMainLooper())
            )
        } catch (e: Exception) {
            bitmap.recycle()
            ThermalLog.e(TAG, "PixelCopy request failed: ${e.message}")
            callback.onError("Failed to execute PixelCopy", e)
        }
    }

    private fun handlePixelCopyResult(
        result: Int,
        bitmap: Bitmap,
        thermalImage: ThermalImage,
        file: File,
        thermalWidth: Int?,
        thermalHeight: Int?,
        callback: Callback
    ) {
        if (result != PixelCopy.SUCCESS) {
            bitmap.recycle()
            val errorMsg = when (result) {
                PixelCopy.ERROR_DESTINATION_INVALID -> "Destination bitmap invalid"
                PixelCopy.ERROR_SOURCE_INVALID -> "Source window invalid"
                PixelCopy.ERROR_SOURCE_NO_DATA -> "Source has no data"
                PixelCopy.ERROR_TIMEOUT -> "Capture timeout"
                else -> "Unknown error code: $result"
            }
            ThermalLog.e(TAG, "PixelCopy failed: $errorMsg")
            callback.onError("PixelCopy failed: $errorMsg")
            return
        }

        ThermalLog.d(TAG, "PixelCopy successful, processing overlay...")

        var finalBitmap: Bitmap? = null
        try {
            // Resize if needed to match thermal resolution
            finalBitmap = resizeIfNeeded(bitmap, thermalWidth, thermalHeight)
            
            ThermalLog.d(TAG, "Creating JavaImageBuffer from ${finalBitmap.width}x${finalBitmap.height} bitmap")
            
            // Convert to ByteBuffer (heap-based as per FLIR docs)
            val buffer = bitmapToByteBuffer(finalBitmap)
            
            // Try to create JavaImageBuffer with multiple strategies
            val overlay = createJavaImageBuffer(buffer, finalBitmap.width, finalBitmap.height)
                ?: throw IllegalStateException("Failed to create JavaImageBuffer")
            
            ThermalLog.d(TAG, "Saving thermal image with overlay to: ${file.absolutePath}")
            thermalImage.saveAs(file.absolutePath, overlay)
            
            ThermalLog.i(TAG, "✓ Snapshot with overlay saved successfully")
            callback.onSuccess(file)
            
        } catch (e: Exception) {
            ThermalLog.e(TAG, "Failed to save snapshot with overlay: ${e.message}")
            callback.onError("Failed to save snapshot with overlay", e)
        } finally {
            // Clean up bitmaps to avoid memory leaks
            if (finalBitmap != bitmap) {
                finalBitmap?.recycle()
            }
            bitmap.recycle()
        }
    }
    
    /**
     * Handle PixelCopy result for saving overlay as separate PNG file.
     * Since ThermalImage.fromFile() doesn't exist and the ThermalImage reference
     * is already invalid at this point, we save the overlay as a separate file.
     */
    private fun handlePixelCopyForOverlay(
        result: Int,
        bitmap: Bitmap,
        thermalFile: File,
        thermalWidth: Int?,
        thermalHeight: Int?,
        callback: Callback
    ) {
        if (result != PixelCopy.SUCCESS) {
            bitmap.recycle()
            val errorMsg = when (result) {
                PixelCopy.ERROR_DESTINATION_INVALID -> "Destination bitmap invalid"
                PixelCopy.ERROR_SOURCE_INVALID -> "Source window invalid"
                PixelCopy.ERROR_SOURCE_NO_DATA -> "Source has no data"
                PixelCopy.ERROR_TIMEOUT -> "Capture timeout"
                else -> "Unknown error code: $result"
            }
            ThermalLog.e(TAG, "PixelCopy failed: $errorMsg")
            callback.onError("PixelCopy failed: $errorMsg")
            return
        }

        ThermalLog.d(TAG, "PixelCopy successful, saving overlay as separate PNG file...")

        var finalBitmap: Bitmap? = null
        try {
            // Resize if needed to match thermal resolution
            finalBitmap = resizeIfNeeded(bitmap, thermalWidth, thermalHeight)
            
            // Create overlay file path (same name with _overlay.png suffix)
            val overlayFile = File(
                thermalFile.parentFile,
                thermalFile.nameWithoutExtension + "_overlay.png"
            )
            
            ThermalLog.d(TAG, "Saving overlay to: ${overlayFile.absolutePath}")
            
            // Save overlay as PNG (high quality, lossless)
            overlayFile.outputStream().use { out ->
                finalBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            
            ThermalLog.i(TAG, "✓ Snapshot saved successfully!")
            ThermalLog.i(TAG, "  - Thermal data: ${thermalFile.absolutePath}")
            ThermalLog.i(TAG, "  - Visual overlay: ${overlayFile.absolutePath}")
            
            // Return the thermal file (main file)
            callback.onSuccess(thermalFile)
            
        } catch (e: Exception) {
            ThermalLog.e(TAG, "Failed to save overlay file: ${e.message}")
            e.printStackTrace()
            callback.onError("Failed to save overlay file", e)
        } finally {
            // Clean up bitmaps to avoid memory leaks
            if (finalBitmap != bitmap) {
                finalBitmap?.recycle()
            }
            bitmap.recycle()
        }
    }

    /**
     * Resize bitmap to match thermal resolution if specified.
     */
    private fun resizeIfNeeded(
        bitmap: Bitmap,
        targetWidth: Int?,
        targetHeight: Int?
    ): Bitmap {
        if (targetWidth == null || targetHeight == null) {
            ThermalLog.d(TAG, "No resize needed (target dimensions not specified)")
            return bitmap
        }

        if (bitmap.width == targetWidth && bitmap.height == targetHeight) {
            ThermalLog.d(TAG, "No resize needed (dimensions already match)")
            return bitmap
        }

        ThermalLog.d(TAG, "Resizing from ${bitmap.width}x${bitmap.height} to ${targetWidth}x${targetHeight}")
        return Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
    }

    /**
     * Convert Bitmap → ByteBuffer (heap-based).
     * Uses allocate() instead of allocateDirect() as per FLIR SDK documentation.
     */
    private fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val size = bitmap.byteCount
        val buffer = ByteBuffer.allocate(size)
        bitmap.copyPixelsToBuffer(buffer)
        buffer.rewind()
        ThermalLog.d(TAG, "Converted bitmap to ByteBuffer: $size bytes")
        return buffer
    }

    /**
     * Create JavaImageBuffer using reflection to support different SDK versions.
     * Tries multiple constructor signatures in order of preference.
     */
    private fun createJavaImageBuffer(buffer: ByteBuffer, width: Int, height: Int): com.flir.thermalsdk.image.JavaImageBuffer? {
        val pixelData = buffer.array()
        
        // Strategy 0: Try the REAL constructor signature found in the SDK
        // JavaImageBuffer(byte[], Format, int, int, int)
        val formatEnum = findFormatEnum()
        if (formatEnum != null) {
            val stride = width * 4  // ARGB8888 = 4 bytes per pixel
            
            tryDeclaredConstructor(
                listOf(ByteArray::class.java, formatEnum.javaClass, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
                listOf(pixelData, formatEnum, width, height, stride)
            )?.let {
                ThermalLog.i(TAG, "✓ Created JavaImageBuffer with Constructor(byte[], Format, int, int, int)")
                return it
            }
        }
        
        // Strategy 1: Try BitmapAndroid wrapper
        tryBitmapAndroid(pixelData, width, height)?.let {
            ThermalLog.i(TAG, "✓ Created JavaImageBuffer via BitmapAndroid")
            return it
        }
        
        // Strategy 2: Try Builder pattern
        tryBuilder(pixelData, width, height)?.let {
            ThermalLog.i(TAG, "✓ Created JavaImageBuffer via Builder")
            return it
        }
        
        // Strategy 3: Try other constructor variations (legacy support)
        if (formatEnum != null) {
            // Try: JavaImageBuffer(ByteBuffer, int, int, Format)
            tryConstructor(
                listOf(ByteBuffer::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, formatEnum.javaClass),
                listOf(ByteBuffer.wrap(pixelData), width, height, formatEnum)
            )?.let { 
                ThermalLog.i(TAG, "✓ Created JavaImageBuffer with Constructor(ByteBuffer, int, int, Format)")
                return it 
            }
            
            // Try: JavaImageBuffer(byte[], int, int, Format)
            tryConstructor(
                listOf(ByteArray::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!, formatEnum.javaClass),
                listOf(pixelData, width, height, formatEnum)
            )?.let {
                ThermalLog.i(TAG, "✓ Created JavaImageBuffer with Constructor(byte[], int, int, Format)")
                return it
            }
        }
        
        // Strategy 4: Try without format
        tryConstructor(
            listOf(ByteBuffer::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
            listOf(ByteBuffer.wrap(pixelData), width, height)
        )?.let {
            ThermalLog.i(TAG, "✓ Created JavaImageBuffer with Constructor(ByteBuffer, int, int)")
            return it
        }
        
        tryConstructor(
            listOf(ByteArray::class.java, Int::class.javaPrimitiveType!!, Int::class.javaPrimitiveType!!),
            listOf(pixelData, width, height)
        )?.let {
            ThermalLog.i(TAG, "✓ Created JavaImageBuffer with Constructor(byte[], int, int)")
            return it
        }
        
        ThermalLog.e(TAG, "❌ No compatible JavaImageBuffer constructor found")
        logAvailableConstructors()
        return null
    }
    
    /**
     * Try to use declared constructor (including package-private/protected).
     * This is needed because JavaImageBuffer has no public constructors.
     */
    private fun tryDeclaredConstructor(paramTypes: List<Class<*>>, paramValues: List<Any>): com.flir.thermalsdk.image.JavaImageBuffer? {
        try {
            val constructor = com.flir.thermalsdk.image.JavaImageBuffer::class.java.getDeclaredConstructor(*paramTypes.toTypedArray())
            constructor.isAccessible = true  // ← CRITICAL: Acessa construtor não-público!
            val result = constructor.newInstance(*paramValues.toTypedArray())
            ThermalLog.d(TAG, "Successfully invoked declared constructor")
            return result as com.flir.thermalsdk.image.JavaImageBuffer
        } catch (e: NoSuchMethodException) {
            ThermalLog.w(TAG, "Declared constructor not found: ${paramTypes.joinToString { it.simpleName }}")
            return null
        } catch (e: java.lang.reflect.InvocationTargetException) {
            ThermalLog.e(TAG, "Declared constructor invocation failed: ${e.cause?.message ?: e.message}")
            e.cause?.printStackTrace()
            return null
        } catch (e: Exception) {
            ThermalLog.w(TAG, "Declared constructor attempt failed: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            return null
        }
    }
    
    /**
     * Try to create JavaImageBuffer via BitmapAndroid wrapper.
     */
    private fun tryBitmapAndroid(pixelData: ByteArray, width: Int, height: Int): com.flir.thermalsdk.image.JavaImageBuffer? {
        try {
            // Create bitmap from pixel data
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val buffer = ByteBuffer.wrap(pixelData)
            buffer.rewind()
            bitmap.copyPixelsFromBuffer(buffer)
            
            // Try to create BitmapAndroid
            val bitmapAndroidClass = Class.forName("com.flir.thermalsdk.androidsdk.image.BitmapAndroid")
            val bitmapAndroid = bitmapAndroidClass.getConstructor(Bitmap::class.java).newInstance(bitmap)
            
            // Check if it's a JavaImageBuffer
            if (bitmapAndroid is com.flir.thermalsdk.image.JavaImageBuffer) {
                return bitmapAndroid
            }
            
            // Try to extract JavaImageBuffer from it
            try {
                val method = bitmapAndroidClass.getMethod("getImageBuffer")
                val result = method.invoke(bitmapAndroid)
                if (result is com.flir.thermalsdk.image.JavaImageBuffer) {
                    return result
                }
            } catch (e: Exception) {
                ThermalLog.w(TAG, "BitmapAndroid.getImageBuffer() failed: ${e.message}")
            }
            
            bitmap.recycle()
        } catch (e: ClassNotFoundException) {
            ThermalLog.w(TAG, "BitmapAndroid class not found")
        } catch (e: Exception) {
            ThermalLog.w(TAG, "BitmapAndroid approach failed: ${e.message}")
        }
        return null
    }
    
    /**
     * Try to create JavaImageBuffer via Builder pattern.
     */
    private fun tryBuilder(pixelData: ByteArray, width: Int, height: Int): com.flir.thermalsdk.image.JavaImageBuffer? {
        try {
            val builderClass = Class.forName("com.flir.thermalsdk.image.JavaImageBuffer\$Builder")
            val builder = builderClass.getDeclaredConstructor().newInstance()
            
            var anySet = false
            
            // Try to set dimensions
            anySet = tryInvoke(builder, "width", width) || anySet
            anySet = tryInvoke(builder, "setWidth", width) || anySet
            anySet = tryInvoke(builder, "height", height) || anySet
            anySet = tryInvoke(builder, "setHeight", height) || anySet
            
            // Try to set format
            val formatEnum = findFormatEnum()
            if (formatEnum != null) {
                anySet = tryInvoke(builder, "format", formatEnum) || anySet
                anySet = tryInvoke(builder, "setFormat", formatEnum) || anySet
            }
            
            // Try to set pixel data
            anySet = tryInvoke(builder, "pixels", pixelData) || anySet
            anySet = tryInvoke(builder, "setPixels", pixelData) || anySet
            anySet = tryInvoke(builder, "buffer", ByteBuffer.wrap(pixelData)) || anySet
            anySet = tryInvoke(builder, "setBuffer", ByteBuffer.wrap(pixelData)) || anySet
            anySet = tryInvoke(builder, "data", pixelData) || anySet
            anySet = tryInvoke(builder, "setData", pixelData) || anySet
            
            if (!anySet) {
                ThermalLog.w(TAG, "Builder exists but no setters worked")
                return null
            }
            
            // Try to build
            val buildMethod = builderClass.getMethod("build")
            val result = buildMethod.invoke(builder)
            
            if (result is com.flir.thermalsdk.image.JavaImageBuffer) {
                return result
            } else {
                ThermalLog.w(TAG, "Builder.build() returned wrong type: ${result?.javaClass?.name ?: "null"}")
            }
        } catch (e: ClassNotFoundException) {
            ThermalLog.w(TAG, "Builder class not found")
        } catch (e: Exception) {
            ThermalLog.w(TAG, "Builder approach failed: ${e.message}")
        }
        return null
    }
    
    /**
     * Try to invoke a method on an object.
     */
    private fun tryInvoke(target: Any, methodName: String, arg: Any): Boolean {
        try {
            // Try with exact type
            val method = target.javaClass.getMethod(methodName, arg.javaClass)
            method.invoke(target, arg)
            return true
        } catch (e: NoSuchMethodException) {
            // Try with primitive int if arg is Integer
            if (arg is Int) {
                try {
                    val method = target.javaClass.getMethod(methodName, Int::class.javaPrimitiveType)
                    method.invoke(target, arg)
                    return true
                } catch (e2: Exception) {
                    // Ignore
                }
            }
        } catch (e: Exception) {
            // Ignore
        }
        return false
    }

    private fun tryConstructor(paramTypes: List<Class<*>>, paramValues: List<Any>): com.flir.thermalsdk.image.JavaImageBuffer? {
        try {
            val constructor = com.flir.thermalsdk.image.JavaImageBuffer::class.java.getConstructor(*paramTypes.toTypedArray())
            val result = constructor.newInstance(*paramValues.toTypedArray())
            return result as com.flir.thermalsdk.image.JavaImageBuffer
        } catch (e: NoSuchMethodException) {
            ThermalLog.w(TAG, "Constructor not found: ${paramTypes.joinToString { it.simpleName }}")
            return null
        } catch (e: java.lang.reflect.InvocationTargetException) {
            ThermalLog.w(TAG, "Constructor invocation failed: ${e.cause?.message ?: e.message}")
            e.cause?.printStackTrace()
            return null
        } catch (e: Exception) {
            ThermalLog.w(TAG, "Constructor attempt failed: ${e.javaClass.simpleName}: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    /**
     * Find the Format enum for ARGB8888 pixel format.
     */
    private fun findFormatEnum(): Any? {
        val enumClassNames = listOf(
            "com.flir.thermalsdk.image.JavaImageBuffer\$Format",
            "com.flir.thermalsdk.image.ImageBuffer\$Format",
            "com.flir.thermalsdk.image.JavaImageBufferType",
            "com.flir.thermalsdk.image.PixelFormat",
            "com.flir.thermalsdk.image.Format"
        )
        
        for (className in enumClassNames) {
            try {
                val enumClass = Class.forName(className)
                if (!enumClass.isEnum) continue
                
                val values = enumClass.enumConstants ?: continue
                
                // Try to find ARGB8888 or similar
                for (value in values) {
                    val name = (value as Enum<*>).name
                    if (name.contains("ARGB") && name.contains("8888")) {
                        ThermalLog.d(TAG, "Found format enum: $className.$name")
                        return value
                    }
                }
                
                // Fallback: use first value
                if (values.isNotEmpty()) {
                    ThermalLog.d(TAG, "Using first format enum: $className.${(values[0] as Enum<*>).name}")
                    return values[0]
                }
            } catch (e: Exception) {
                // Try next class name
            }
        }
        
        ThermalLog.w(TAG, "Could not find format enum")
        return null
    }

    /**
     * Log all available constructors for debugging.
     */
    private fun logAvailableConstructors() {
        try {
            ThermalLog.e(TAG, "Available JavaImageBuffer constructors:")
            val constructors = com.flir.thermalsdk.image.JavaImageBuffer::class.java.constructors
            
            if (constructors.isEmpty()) {
                ThermalLog.e(TAG, "  - (no public constructors found)")
            } else {
                for (ctor in constructors) {
                    val params = ctor.parameterTypes.joinToString(", ") { it.simpleName }
                    ThermalLog.e(TAG, "  - JavaImageBuffer($params)")
                }
            }
            
            // Also try declared constructors (including private/protected)
            ThermalLog.e(TAG, "All declared constructors (including private):")
            val declaredCtors = com.flir.thermalsdk.image.JavaImageBuffer::class.java.declaredConstructors
            for (ctor in declaredCtors) {
                val params = ctor.parameterTypes.joinToString(", ") { it.simpleName }
                val modifiers = java.lang.reflect.Modifier.toString(ctor.modifiers)
                ThermalLog.e(TAG, "  - $modifiers JavaImageBuffer($params)")
            }
        } catch (e: Exception) {
            ThermalLog.e(TAG, "Failed to list constructors: ${e.message}")
        }
    }
}














