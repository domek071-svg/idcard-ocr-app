package com.idcard.ocr.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.idcard.ocr.R
import com.idcard.ocr.camera.CameraXManager
import com.idcard.ocr.camera.OverlayView
import com.idcard.ocr.ui.theme.AppColors
import kotlinx.coroutines.launch

/**
 * Camera screen for capturing front and back of ID card
 */
@Composable
fun CameraScreen(
    onNavigateToResult: (String, String) -> Unit,
    onPermissionDenied: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    // State
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    var isCapturing by remember { mutableStateOf(false) }
    var currentSide by remember { mutableStateOf(CaptureSide.FRONT) }
    var frontImageBase64 by remember { mutableStateOf<String?>(null) }
    var backImageBase64 by remember { mutableStateOf<String?>(null) }

    // Camera manager
    val cameraManager = remember { CameraXManager(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }

    // Permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            onPermissionDenied()
        }
    }

    // Initialize camera when permission is granted
    LaunchedEffect(hasCameraPermission, previewView) {
        if (hasCameraPermission && previewView != null) {
            val success = cameraManager.initializeCamera(lifecycleOwner, previewView!!)
            if (!success) {
                Toast.makeText(context, "Failed to initialize camera", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            cameraManager.release()
        }
    }

    // UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (hasCameraPermission) {
            // Camera preview
            AndroidView(
                factory = { ctx ->
                    PreviewView(ctx).also {
                        it.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        previewView = it
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Document overlay
            DocumentOverlay(
                modifier = Modifier.fillMaxSize(),
                aspectRatio = cameraManager.getDocumentAspectRatio()
            )

            // Instructions
            InstructionsOverlay(
                side = currentSide,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            )

            // Progress indicator
            if (isCapturing) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Capturing...",
                            color = Color.White,
                            fontSize = 18.sp
                        )
                    }
                }
            }

            // Capture button
            if (!isCapturing) {
                FloatingActionButton(
                    onClick = {
                        if (currentSide == CaptureSide.FRONT) {
                            scope.launch {
                                isCapturing = true
                                val base64 = cameraManager.captureImage()
                                isCapturing = false

                                if (base64 != null) {
                                    frontImageBase64 = base64
                                    currentSide = CaptureSide.BACK
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Capture failed. Please try again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        } else {
                            scope.launch {
                                isCapturing = true
                                val base64 = cameraManager.captureImage()
                                isCapturing = false

                                if (base64 != null) {
                                    backImageBase64 = base64
                                    // Navigate to results
                                    frontImageBase64?.let { front ->
                                        backImageBase64?.let { back ->
                                            onNavigateToResult(front, back)
                                        }
                                    }
                                } else {
                                    Toast.makeText(
                                        context,
                                        "Capture failed. Please try again.",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 48.dp)
                        .size(72.dp),
                    shape = CircleShape,
                    containerColor = AppColors.CaptureButton
                ) {
                    Icon(
                        imageVector = if (currentSide == CaptureSide.FRONT) {
                            Icons.Default.CameraAlt
                        } else {
                            Icons.Default.Check
                        },
                        contentDescription = "Capture",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

        } else {
            // Permission denied UI
            PermissionDeniedContent(
                onRequestPermission = {
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
            )
        }
    }
}

@Composable
private fun DocumentOverlay(
    modifier: Modifier = Modifier,
    aspectRatio: Float
) {
    Canvas(modifier = modifier) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Calculate frame dimensions
        val maxWidth = canvasWidth * 0.9f
        val maxHeight = canvasHeight * 0.75f

        val frameWidth: Float
        val frameHeight: Float
        val frameX: Float
        val frameY: Float

        if (maxWidth / aspectRatio <= maxHeight) {
            frameWidth = maxWidth
            frameHeight = frameWidth / aspectRatio
        } else {
            frameHeight = maxHeight
            frameWidth = frameHeight * aspectRatio
        }

        frameX = (canvasWidth - frameWidth) / 2
        frameY = (canvasHeight - frameHeight) / 2

        // Draw semi-transparent overlay with cutout
        drawRect(
            color = Color.Black.copy(alpha = 0.6f),
            size = size
        )

        // Clear the frame area
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(frameX, frameY),
            size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
        )

        // Draw frame border
        drawRoundRect(
            color = Color.White,
            topLeft = Offset(frameX, frameY),
            size = androidx.compose.ui.geometry.Size(frameWidth, frameHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx()),
            style = Stroke(width = 3.dp.toPx())
        )

        // Draw corner markers
        val cornerLength = 40.dp.toPx()
        val cornerStroke = 4.dp.toPx()
        val cornerColor = Color.White

        // Top-left
        drawLine(cornerColor, Offset(frameX, frameY + 20), Offset(frameX, frameY), cornerStroke)
        drawLine(cornerColor, Offset(frameX, frameY), Offset(frameX + 20, frameY), cornerStroke)

        // Top-right
        drawLine(cornerColor, Offset(frameX + frameWidth, frameY + 20), Offset(frameX + frameWidth, frameY), cornerStroke)
        drawLine(cornerColor, Offset(frameX + frameWidth - 20, frameY), Offset(frameX + frameWidth, frameY), cornerStroke)

        // Bottom-left
        drawLine(cornerColor, Offset(frameX, frameY + frameHeight - 20), Offset(frameX, frameY + frameHeight), cornerStroke)
        drawLine(cornerColor, Offset(frameX, frameY + frameHeight), Offset(frameX + 20, frameY + frameHeight), cornerStroke)

        // Bottom-right
        drawLine(cornerColor, Offset(frameX + frameWidth, frameY + frameHeight - 20), Offset(frameX + frameWidth, frameY + frameHeight), cornerStroke)
        drawLine(cornerColor, Offset(frameX + frameWidth - 20, frameY + frameHeight), Offset(frameX + frameWidth, frameY + frameHeight), cornerStroke)
    }
}

@Composable
private fun InstructionsOverlay(
    side: CaptureSide,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = Color.Black.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (side == CaptureSide.FRONT) "FRONT SIDE" else "BACK SIDE",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Align document within the frame",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun PermissionDeniedContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "Camera Permission Required",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Camera access is needed to scan ID cards",
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

private enum class CaptureSide {
    FRONT, BACK
}
