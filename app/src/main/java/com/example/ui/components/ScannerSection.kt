package com.example.ui.components

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import android.Manifest
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.BrandGoGreenDark
import com.example.ui.theme.BrandGoGreenLight
import com.example.ui.theme.BrandLaserRedDark
import com.example.ui.theme.BrandLaserRedDarkSoft
import com.example.ui.theme.BrandLaserRedLight
import com.example.ui.theme.BrandWarnDark
import com.example.ui.theme.TillInk
import com.example.ui.theme.TillMuted
import com.example.ui.theme.TillViewBgLight
import com.example.util.BarcodeAnalyzer
import java.util.concurrent.Executors

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ScannerSection(
    isCameraActive: Boolean,
    isTorchOn: Boolean,
    zoomRatio: Float,
    isRemoveMode: Boolean,
    manualCodeInput: String,
    onManualCodeChange: (String) -> Unit,
    onSubmitManualCode: () -> Unit,
    onToggleCamera: () -> Unit,
    onToggleTorch: () -> Unit,
    onToggleZoom: () -> Unit,
    onToggleRemoveMode: () -> Unit,
    onBarcodeDetected: (String) -> Unit,
    onImageSelected: (Uri) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    // Permission state
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (isGranted) {
            onToggleCamera()
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            onImageSelected(uri)
        }
    }

    var cameraRef by remember { mutableStateOf<Camera?>(null) }

    LaunchedEffect(isTorchOn, cameraRef) {
        cameraRef?.cameraControl?.enableTorch(isTorchOn)
    }

    LaunchedEffect(zoomRatio, cameraRef) {
        cameraRef?.cameraControl?.setZoomRatio(zoomRatio)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
            .testTag("scanner_section"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Viewfinder box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 10f)
                .clip(RoundedCornerShape(12.dp))
                .background(TillViewBgLight)
                .then(
                    if (isRemoveMode) Modifier.border(3.dp, BrandLaserRedLight, RoundedCornerShape(12.dp))
                    else Modifier
                )
                .testTag("scanner_viewfinder"),
            contentAlignment = Alignment.Center
        ) {
            if (isCameraActive && hasCameraPermission) {
                // Live camera preview
                val cameraExecutor = remember { Executors.newSingleThreadExecutor() }

                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx)
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

                        cameraProviderFuture.addListener({
                            val cameraProvider = cameraProviderFuture.get()
                            val preview = Preview.Builder().build().also {
                                it.setSurfaceProvider(previewView.surfaceProvider)
                            }

                            val imageAnalyzer = ImageAnalysis.Builder()
                                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                .build()
                                .also {
                                    it.setAnalyzer(
                                        cameraExecutor,
                                        BarcodeAnalyzer { rawBarcode ->
                                            onBarcodeDetected(rawBarcode)
                                        }
                                    )
                                }

                            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                            try {
                                cameraProvider.unbindAll()
                                cameraRef = cameraProvider.bindToLifecycle(
                                    lifecycleOwner,
                                    cameraSelector,
                                    preview,
                                    imageAnalyzer
                                )
                            } catch (e: Exception) {
                                cameraRef = null
                            }
                        }, ContextCompat.getMainExecutor(ctx))

                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )

                DisposableEffect(Unit) {
                    onDispose {
                        cameraExecutor.shutdown()
                    }
                }

                // Viewfinder frame reticle & laser animation
                val infiniteTransition = rememberInfiniteTransition(label = "laser")
                val laserOffset by infiniteTransition.animateFloat(
                    initialValue = 0.15f,
                    targetValue = 0.85f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1400, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "laserPos"
                )

                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Draw corner reticle brackets
                    val left = w * 0.1f
                    val right = w * 0.9f
                    val top = h * 0.15f
                    val bottom = h * 0.85f
                    val cornerLen = 24.dp.toPx()
                    val strokeW = 3.dp.toPx()
                    val frameColor = if (isRemoveMode) BrandLaserRedDark else TillInk

                    // Top-Left
                    drawLine(frameColor, Offset(left, top), Offset(left + cornerLen, top), strokeW)
                    drawLine(frameColor, Offset(left, top), Offset(left, top + cornerLen), strokeW)
                    // Top-Right
                    drawLine(frameColor, Offset(right, top), Offset(right - cornerLen, top), strokeW)
                    drawLine(frameColor, Offset(right, top), Offset(right, top + cornerLen), strokeW)
                    // Bottom-Left
                    drawLine(frameColor, Offset(left, bottom), Offset(left + cornerLen, bottom), strokeW)
                    drawLine(frameColor, Offset(left, bottom), Offset(left, bottom - cornerLen), strokeW)
                    // Bottom-Right
                    drawLine(frameColor, Offset(right, bottom), Offset(right - cornerLen, bottom), strokeW)
                    drawLine(frameColor, Offset(right, bottom), Offset(right, bottom - cornerLen), strokeW)

                    // Draw scanning laser beam
                    val laserY = h * laserOffset
                    drawLine(
                        color = if (isRemoveMode) BrandLaserRedDark else Color(0xFFFF5C50),
                        start = Offset(left + 8.dp.toPx(), laserY),
                        end = Offset(right - 8.dp.toPx(), laserY),
                        strokeWidth = 2.5.dp.toPx()
                    )
                }

                // Remove mode top banner
                if (isRemoveMode) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(top = 10.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(BrandLaserRedLight)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "TOGLI · spara l'articolo da togliere",
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Idle view state when camera is inactive
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Stylized barcode illustration
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Canvas(modifier = Modifier.size(width = 80.dp, height = 32.dp)) {
                            val barW = 2.5.dp.toPx()
                            val spacing = 4.dp.toPx()
                            var x = 0f
                            while (x < size.width) {
                                drawLine(
                                    color = TillInk.copy(alpha = 0.85f),
                                    start = Offset(x, 0f),
                                    end = Offset(x, size.height),
                                    strokeWidth = barW
                                )
                                x += spacing + (if ((x.toInt() % 7) == 0) spacing else 0f)
                            }
                            // Red laser line across illustration
                            drawLine(
                                color = BrandLaserRedLight,
                                start = Offset(-4f, size.height / 2),
                                end = Offset(size.width + 4f, size.height / 2),
                                strokeWidth = 2.dp.toPx()
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "QR Code",
                            tint = TillInk.copy(alpha = 0.85f),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Inquadra il codice a barre o il QR code dell'etichetta: ogni lettura entra nel conto e il totale si aggiorna da solo.",
                        color = TillMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Button(
                        onClick = {
                            if (!hasCameraPermission) {
                                permissionLauncher.launch(Manifest.permission.CAMERA)
                            } else {
                                onToggleCamera()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandGoGreenLight,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(44.dp)
                            .testTag("btn_start_scanner_big")
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Avvia scanner",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Camera controls row
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Start / Stop Scanner toggle button
            OutlinedButton(
                onClick = {
                    if (!hasCameraPermission) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        onToggleCamera()
                    }
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(44.dp)
                    .testTag("btn_toggle_camera")
            ) {
                Icon(
                    imageVector = if (isCameraActive) Icons.Default.Stop else Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCameraActive) "Ferma scanner" else "Avvia scanner",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                )
            }

            // Remove mode button ("Togli un articolo")
            Button(
                onClick = onToggleRemoveMode,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRemoveMode) BrandLaserRedLight else BrandLaserRedDarkSoft,
                    contentColor = if (isRemoveMode) Color.White else BrandLaserRedLight
                ),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(44.dp)
                    .testTag("btn_remove_mode")
            ) {
                Icon(
                    imageVector = Icons.Default.Remove,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isRemoveMode) "Togli attivo" else "Togli articolo",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                )
            }

            // Photo picker button ("Foto del codice")
            OutlinedButton(
                onClick = {
                    photoPickerLauncher.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    )
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f, fill = false)
                    .height(44.dp)
                    .testTag("btn_photo_picker")
            ) {
                Icon(
                    imageVector = Icons.Default.PhotoCamera,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Foto codice",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                )
            }

            // Zoom toggle button (if camera is active)
            if (isCameraActive) {
                OutlinedButton(
                    onClick = onToggleZoom,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (zoomRatio > 1.2f) BrandGoGreenLight.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = if (zoomRatio > 1.2f) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (zoomRatio > 1.2f) "1×" else "Zoom 2×",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                // Torch toggle button
                OutlinedButton(
                    onClick = onToggleTorch,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = if (isTorchOn) BrandWarnDark.copy(alpha = 0.15f) else Color.Transparent
                    ),
                    modifier = Modifier.height(44.dp)
                ) {
                    Icon(
                        imageVector = if (isTorchOn) Icons.Default.FlashlightOff else Icons.Default.FlashOn,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Torcia",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }
            }
        }

        // Manual barcode input form
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = manualCodeInput,
                onValueChange = onManualCodeChange,
                placeholder = {
                    Text(
                        text = "Scrivi il codice o usa la pistola",
                        fontSize = 13.5.sp
                    )
                },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Ascii,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onSubmitManualCode() }
                ),
                singleLine = true,
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("input_manual_barcode")
            )

            Button(
                onClick = onSubmitManualCode,
                colors = ButtonDefaults.buttonColors(
                    containerColor = BrandGoGreenLight,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .height(52.dp)
                    .testTag("btn_submit_manual_barcode")
            ) {
                Text(
                    text = "Aggiungi",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }

        Text(
            text = "Etichetta piccola o lucida? Tieni il telefono a una spanna e inclinalo per togliere i riflessi. Per due pezzi uguali usa Zoom o premi + sulla riga.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
    }
}
