package com.glassesscan.app.ui.visual

import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.compose.ui.res.stringResource
import com.glassesscan.app.R
import com.glassesscan.app.util.AdMobHelper

import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.text.style.TextAlign

import androidx.camera.core.Camera
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashOff

import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import java.io.File
import android.content.ContentValues
import android.provider.MediaStore
import android.graphics.*
import androidx.compose.material.icons.filled.Camera
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualAIScreen(onBack: () -> Unit, isPremium: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    
    var showInstructions by remember { mutableStateOf(true) }
    var isTorchEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    var faceDetected by remember { mutableStateOf(false) }
    
    val imageCapture = remember { ImageCapture.Builder().build() }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.nav_visual), style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBackIosNew, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { 
                        isTorchEnabled = !isTorchEnabled
                        camera?.cameraControl?.enableTorch(isTorchEnabled)
                    }) {
                        Icon(
                            imageVector = if (isTorchEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = stringResource(R.string.torch),
                            tint = if (isTorchEnabled) Color(0xFFFFCC00) else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showInstructions = !showInstructions }) {
                        Icon(Icons.Default.Info, contentDescription = stringResource(R.string.instructions_desc), tint = if (showInstructions) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface)
                    }
                }
            )
        },
        bottomBar = {
            AdMobHelper.BannerAdView(isPremium = isPremium)
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        
                        // ML Kit Face Detection
                        val faceDetector = FaceDetection.getClient(
                            FaceDetectorOptions.Builder()
                                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                                .build()
                        )
                        
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        
                        imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                faceDetector.process(image)
                                    .addOnSuccessListener { faces ->
                                        faceDetected = faces.isNotEmpty()
                                    }
                                    .addOnCompleteListener {
                                        imageProxy.close()
                                    }
                            } else {
                                imageProxy.close()
                            }
                        }

                        try {
                            cameraProvider.unbindAll()
                            camera = cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture,
                                imageAnalysis
                            )
                        } catch (e: Exception) {}
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )
            
            // Status overlay for LED flicker detection
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (showInstructions) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)),
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                stringResource(R.string.ai_scan_guide_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                stringResource(R.string.ai_scan_how_to),
                                style = MaterialTheme.typography.bodySmall,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (faceDetected) Color(0xFFFF3B30).copy(alpha = 0.8f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Text(
                        if (faceDetected) "WAJAH TERDETEKSI - MENGANALISIS KACAMATA..." else stringResource(R.string.ai_scanning_label),
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Camera Shutter Style Capture Button
                IconButton(
                    onClick = { 
                        takeEvidencePhoto(context, imageCapture)
                    },
                    modifier = Modifier
                        .size(72.dp)
                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                        .padding(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Camera,
                            contentDescription = stringResource(R.string.capture_evidence),
                            tint = Color(0xFF007AFF),
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun takeEvidencePhoto(context: android.content.Context, imageCapture: ImageCapture) {
    val name = "Glasses_Evidence_${System.currentTimeMillis()}.jpg"
    val contentValues = ContentValues().apply {
        put(MediaStore.MediaColumns.DISPLAY_NAME, name)
        put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
        if (android.os.Build.VERSION.SDK_INT > android.os.Build.VERSION_CODES.P) {
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/GlassesScanner")
        }
    }

    val outputOptions = ImageCapture.OutputFileOptions.Builder(
        context.contentResolver,
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        contentValues
    ).build()

    val photoFailedMsg = context.getString(R.string.photo_failed)
    val evidenceSavedMsg = context.getString(R.string.evidence_saved)

    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                android.widget.Toast.makeText(context, photoFailedMsg.format(exc.message), android.widget.Toast.LENGTH_SHORT).show()
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                android.widget.Toast.makeText(context, evidenceSavedMsg, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    )
}
