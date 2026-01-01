package com.example.pantrypal.ui

import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.max

private val VIEWFINDER_CORNER_RADIUS = 16.dp
private val VIEWFINDER_STROKE_WIDTH = 4.dp
private val VIEWFINDER_PADDING = 32.dp
private val VIEWFINDER_ASPECT_RATIO = 0.6f

// Helper class to pass data to analyzer
private data class ScannerConfig(
    val viewSize: Size = Size.Zero,
    val viewfinderRect: Rect = Rect.Zero
)

@Composable
fun BarcodeScanner(
    modifier: Modifier = Modifier,
    onBarcodeDetected: (String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val scannerConfig = remember { AtomicReference(ScannerConfig()) }

    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                cameraProviderFuture.addListener({
                    val cameraProvider = cameraProviderFuture.get()
                    val preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    val options = BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(Barcode.FORMAT_ALL_FORMATS)
                        .build()
                    val scanner = BarcodeScanning.getClient(options)

                    val imageAnalysis = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also {
                            it.setAnalyzer(cameraExecutor) { imageProxy ->
                                processImageProxy(scanner, imageProxy, onBarcodeDetected, scannerConfig.get())
                            }
                        }

                    try {
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageAnalysis
                        )
                    } catch (exc: Exception) {
                        android.util.Log.e("BarcodeScanner", "Use case binding failed", exc)
                    }
                }, ContextCompat.getMainExecutor(ctx))
                previewView
            }
        )

        // Viewfinder overlay
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Calculate viewfinder rect (wider and rectangular)
            val padding = VIEWFINDER_PADDING.toPx()
            val rectWidth = width - (padding * 2)
            val rectHeight = rectWidth * VIEWFINDER_ASPECT_RATIO // Aspect ratio example
            val left = (width - rectWidth) / 2
            val top = (height - rectHeight) / 2
            val rect = Rect(left, top, left + rectWidth, top + rectHeight)

            // Update config for analyzer
            scannerConfig.set(ScannerConfig(Size(width, height), rect))

            // Draw dimmed background
            val path = Path().apply {
                addRect(Rect(0f, 0f, width, height))
            }
            val holePath = Path().apply {
                addRoundRect(RoundRect(rect, CornerRadius(VIEWFINDER_CORNER_RADIUS.toPx())))
            }
            val difference = Path.combine(PathOperation.Difference, path, holePath)

            drawPath(difference, Color.Black.copy(alpha = 0.5f))

            drawRoundRect(
                color = Color.White,
                topLeft = Offset(left, top),
                size = Size(rectWidth, rectHeight),
                cornerRadius = CornerRadius(VIEWFINDER_CORNER_RADIUS.toPx()),
                style = Stroke(width = VIEWFINDER_STROKE_WIDTH.toPx())
            )
        }
    }
}

private fun processImageProxy(
    scanner: com.google.mlkit.vision.barcode.BarcodeScanner,
    imageProxy: ImageProxy,
    onBarcodeDetected: (String) -> Unit,
    config: ScannerConfig
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        scanner.process(image)
            .addOnSuccessListener { barcodes ->
                for (barcode in barcodes) {
                    if (isBarcodeInViewfinder(barcode, imageProxy, config)) {
                        barcode.rawValue?.let {
                            onBarcodeDetected(it)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("BarcodeScanner", "Barcode processing failed", e)
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    } else {
        imageProxy.close()
    }
}

private fun isBarcodeInViewfinder(barcode: Barcode, imageProxy: ImageProxy, config: ScannerConfig): Boolean {
    val box = barcode.boundingBox ?: return false
    if (config.viewSize == Size.Zero) return true // default allow if UI not ready

    // Map box to View coordinates
    // 1. Get rotated image dimensions
    val rotation = imageProxy.imageInfo.rotationDegrees
    val imgW = imageProxy.width.toFloat()
    val imgH = imageProxy.height.toFloat()
    val (rotatedW, rotatedH) = if (rotation == 90 || rotation == 270) {
        imgH to imgW
    } else {
        imgW to imgH
    }

    // 2. Calculate Scale and Offset for FILL_VIEWPORT (CenterCrop)
    val viewW = config.viewSize.width
    val viewH = config.viewSize.height
    val scale = max(viewW / rotatedW, viewH / rotatedH)
    val scaledW = rotatedW * scale
    val scaledH = rotatedH * scale
    val offsetX = (viewW - scaledW) / 2
    val offsetY = (viewH - scaledH) / 2

    // 3. Map the barcode bounding box to view coordinates
    val transformPoint: (Float, Float) -> Offset = { x, y ->
        val (rotatedX, rotatedY) = rotatePoint(x, y, rotation, imgW, imgH)
        Offset(
            x = rotatedX * scale + offsetX,
            y = rotatedY * scale + offsetY
        )
    }

    val corners = barcode.cornerPoints
    if (corners.isNullOrEmpty()) {
        // Fallback to center point check if corner points are not available
        val centerInView = transformPoint(box.centerX().toFloat(), box.centerY().toFloat())
        return config.viewfinderRect.contains(centerInView)
    }

    val mappedCorners = corners.map { point ->
        transformPoint(point.x.toFloat(), point.y.toFloat())
    }

    val mappedBoundingBox = Rect(
        left = mappedCorners.minOf { it.x },
        top = mappedCorners.minOf { it.y },
        right = mappedCorners.maxOf { it.x },
        bottom = mappedCorners.maxOf { it.y }
    )

    // 4. Check if the mapped bounding box overlaps with the viewfinder
    return config.viewfinderRect.overlaps(mappedBoundingBox)
}

private fun rotatePoint(x: Float, y: Float, rotation: Int, width: Float, height: Float): Pair<Float, Float> {
    return when (rotation) {
        90 -> (height - y) to x
        180 -> (width - x) to (height - y)
        270 -> y to (width - x)
        else -> x to y
    }
}
