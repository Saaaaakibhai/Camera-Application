package com.example.cameraapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.Size
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.cameraapplication.ui.OverlayFrameView
import com.example.cameraapplication.ui.theme.PhotoTypeSelector
import com.example.cameraapplication.ui.theme.CameraApplicationTheme
import com.example.cameraapplication.utils.ImageSaver.AspectRatioOption
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CameraApplicationTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val needsStoragePermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q

    var hasStoragePermission by remember {
        mutableStateOf(
            if (needsStoragePermission) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        )
    }

    var showCameraScreen by remember { mutableStateOf(false) }
    var imageCapture: ImageCapture? by remember { mutableStateOf(null) }
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    var selectedAspectRatio by remember { mutableStateOf(AspectRatioOption.RATIO_4_3) }

    val storagePermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasStoragePermission = granted
        if (!granted) {
            Toast.makeText(context, "Storage permission is needed to save photos", Toast.LENGTH_SHORT).show()
        } else if (hasCameraPermission) {
            showCameraScreen = true
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            Toast.makeText(context, "Camera permission is required", Toast.LENGTH_SHORT).show()
        } else if (needsStoragePermission && !hasStoragePermission) {
            storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        } else {
            showCameraScreen = true
        }
    }

    if (!showCameraScreen) {
        WelcomeScreen(
            onRequestPermission = {
                if (!hasCameraPermission) {
                    cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                } else if (needsStoragePermission && !hasStoragePermission) {
                    storagePermissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                } else {
                    showCameraScreen = true
                }
            }
        )
    } else {
        Column(Modifier.fillMaxSize()) {
            PhotoTypeSelector(
                selectedAspectRatio = selectedAspectRatio,
                onAspectRatioSelected = { newRatio ->
                    selectedAspectRatio = newRatio
                }
            )

            CameraScreen(
                selectedAspectRatio = selectedAspectRatio,
                onCapture = {
                    imageCapture?.let { capture ->
                        takePhoto(context, capture, cameraExecutor)
                    } ?: Toast.makeText(context, "Camera is not ready", Toast.LENGTH_SHORT).show()
                },
                onImageCaptureReady = { newImageCapture ->
                    imageCapture = newImageCapture
                },
                onPreviewViewReady = { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val previewBuilder = Preview.Builder()
                        val imageCaptureBuilder = ImageCapture.Builder()

                        when (selectedAspectRatio) {
                            AspectRatioOption.RATIO_4_3 -> {
                                val resolution = Size(1280, 960)
                                previewBuilder.setTargetResolution(resolution)
                                imageCaptureBuilder.setTargetResolution(resolution)
                            }
                            AspectRatioOption.RATIO_16_9 -> {
                                val resolution = Size(1920, 1080)
                                previewBuilder.setTargetResolution(resolution)
                                imageCaptureBuilder.setTargetResolution(resolution)
                            }
                            AspectRatioOption.RATIO_3_2 -> {
                                val resolution = Size(1440, 960)
                                previewBuilder.setTargetResolution(resolution)
                                imageCaptureBuilder.setTargetResolution(resolution)
                            }
                        }

                        val preview = previewBuilder.build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        imageCapture = imageCaptureBuilder.build()

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageCapture
                            )
                        } catch (exc: Exception) {
                            Log.e("CameraApp", "Failed to bind camera use cases", exc)
                        }
                    }, ContextCompat.getMainExecutor(context))
                }
            )
        }
    }
}

@Composable
fun WelcomeScreen(onRequestPermission: () -> Unit) {
    val color1 = Color(0xFF1976D2)
    val color2 = Color(0xFF0D47A1)
    var toggle by remember { mutableStateOf(false) }
    val animatedColor by animateColorAsState(targetValue = if (toggle) color1 else color2)

    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(1000)
            toggle = !toggle
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, fontSize = 28.sp, color = Color(0xFF0D47A1))) {
                    append("Welcome To\n\n")
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, color = Color(0xFF1976D2))) {
                    append("Q-Soft Precise Assistance\n")
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Medium, fontSize = 20.sp, color = Color.Gray)) {
                    append("Camera Application\n")
                }
            },
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onRequestPermission,
            colors = ButtonDefaults.buttonColors(
                containerColor = animatedColor,
                contentColor = Color.White
            )
        ) {
            Text("Start Camera")
        }
    }
}

@Composable
fun CameraScreen(
    selectedAspectRatio: AspectRatioOption,
    onCapture: () -> Unit,
    onImageCaptureReady: (ImageCapture) -> Unit,
    onPreviewViewReady: (PreviewView) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    var isAligned by remember { mutableStateOf(false) }
    var cameraSelector by remember { mutableStateOf(CameraSelector.DEFAULT_BACK_CAMERA) }

    // Face detector configuration
    val faceDetector = remember {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .build()
        FaceDetection.getClient(options)
    }

    val imageAnalysis = remember {
        ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { analyzer ->
                analyzer.setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    processImageForFaceDetection(imageProxy, faceDetector) { rect ->
                        // Update the overlay with detection rectangle
                        isAligned = rect != null // Simple alignment check - you might want more complex logic
                    }
                    imageProxy.close()
                }
            }
    }

    LaunchedEffect(selectedAspectRatio, cameraSelector) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val previewBuilder = Preview.Builder()
            val imageCaptureBuilder = ImageCapture.Builder()
            val imageAnalysisBuilder = ImageAnalysis.Builder() // Create a builder for ImageAnalysis

            when (selectedAspectRatio) {
                AspectRatioOption.RATIO_4_3 -> {
                    val resolution = Size(1280, 960)
                    previewBuilder.setTargetResolution(resolution)
                    imageCaptureBuilder.setTargetResolution(resolution)
                    imageAnalysisBuilder.setTargetResolution(resolution) // Set resolution for analysis
                }
                AspectRatioOption.RATIO_16_9 -> {
                    val resolution = Size(1920, 1080)
                    previewBuilder.setTargetResolution(resolution)
                    imageCaptureBuilder.setTargetResolution(resolution)
                    imageAnalysisBuilder.setTargetResolution(resolution)
                }
                AspectRatioOption.RATIO_3_2 -> {
                    val resolution = Size(1440, 960)
                    previewBuilder.setTargetResolution(resolution)
                    imageCaptureBuilder.setTargetResolution(resolution)
                    imageAnalysisBuilder.setTargetResolution(resolution)
                }
            }

            val preview = previewBuilder.build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageCapture = imageCaptureBuilder.build()
            val imageAnalysis = imageAnalysisBuilder.build().apply {
                setAnalyzer(ContextCompat.getMainExecutor(context)) { imageProxy ->
                    processImageForFaceDetection(imageProxy, faceDetector) { rect ->
                        isAligned = rect != null
                    }
                    imageProxy.close()
                }
            }

            onImageCaptureReady(imageCapture)

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    cameraSelector,
                    preview,
                    imageCapture,
                    imageAnalysis
                )
            } catch (exc: Exception) {
                Log.e("CameraApp", "Failed to bind camera use cases", exc)
            }
        }, ContextCompat.getMainExecutor(context))
    }
    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { previewView }
        )

        // Overlay Frame View
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { OverlayFrameView(context) },
            update = { overlay ->
                overlay.isAligned = isAligned
                overlay.showDetectionRect = true
            }
        )

        // Instructions Text
        Text(
            text = if (isAligned) "Aligned! Capture now." else "Place the subject within the frame.",
            color = if (isAligned) Color.Green else Color.White,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.5f))
        )

        // Camera flip button (top-right corner)
        IconButton(
            onClick = {
                cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                } else {
                    CameraSelector.DEFAULT_BACK_CAMERA
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
                .background(Color.Black.copy(alpha = 0.3f), CircleShape)
                .size(48.dp),
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = Color.White
            )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.cameraswitch),
                contentDescription = "Switch camera",
                modifier = Modifier.size(32.dp)
            )
        }

        // Capture button (bottom center)
        Button(
            onClick = onCapture,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 90.dp)
                .size(80.dp)
                .border(
                    width = 3.dp,
                    color = Color.White,
                    shape = CircleShape
                ),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF1976D2),
                contentColor = Color.White
            ),
            shape = CircleShape
        ) {
            Icon(
                painter = painterResource(id = R.drawable.camera),
                contentDescription = "Capture photo",
                modifier = Modifier.size(36.dp)
            )
        }
    }
}

private fun processImageForFaceDetection(
    imageProxy: ImageProxy,
    faceDetector: FaceDetector,
    onFaceDetected: (Rect?) -> Unit
) {
    val mediaImage = imageProxy.image
    if (mediaImage != null) {
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

        faceDetector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isNotEmpty()) {
                    val boundingBox = faces[0].boundingBox
                    onFaceDetected(boundingBox)
                } else {
                    onFaceDetected(null)
                }
            }
            .addOnFailureListener { e ->
                Log.e("CameraApp", "Face detection failed", e)
                onFaceDetected(null)
            }
    }
}

private fun takePhoto(context: Context, imageCapture: ImageCapture, executor: ExecutorService) {
    val photoFile = createImageFile(context)
    val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

    imageCapture.takePicture(
        outputOptions,
        executor,
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Photo Saved Successfully", Toast.LENGTH_LONG).show()
                }
                Log.d("CameraApp", "Photo saved to: ${photoFile.absolutePath}")
            }

            override fun onError(exception: ImageCaptureException) {
                Handler(Looper.getMainLooper()).post {
                    Toast.makeText(context, "Error saving photo: ${exception.message}", Toast.LENGTH_LONG).show()
                }
                Log.e("CameraApp", "Photo capture failed", exception)
            }
        }
    )
}

private fun createImageFile(context: Context): File {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
    return File(storageDir, "JPEG_${timeStamp}.jpg")
}