package upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271415_e_05

import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import java.util.concurrent.Executors

// ✅ --- FIX 1: Define landmarks for BOTH arms ---
private object LandmarkConnections {
    // Left Arm
    /* const val LEFT_SHOULDER = 11
    const val LEFT_ELBOW = 13
    const val LEFT_WRIST = 15
    const val LEFT_PINKY = 17
    const val LEFT_INDEX = 19
    const val LEFT_THUMB = 21

    // Right Arm
    const val RIGHT_SHOULDER = 12
    const val RIGHT_ELBOW = 14
    const val RIGHT_WRIST = 16
    const val RIGHT_PINKY = 18
    const val RIGHT_INDEX = 20
    const val RIGHT_THUMB = 22

    // Define which points to connect for drawing
    val connections = listOf(
        // Left Arm
        LEFT_SHOULDER to LEFT_ELBOW,
        LEFT_ELBOW to LEFT_WRIST,
        // Left Hand (Palm)
        LEFT_WRIST to LEFT_THUMB,
        LEFT_WRIST to LEFT_PINKY,
        LEFT_PINKY to LEFT_INDEX,

        // Right Arm
        RIGHT_SHOULDER to RIGHT_ELBOW,
        RIGHT_ELBOW to RIGHT_WRIST,
        // Right Hand (Palm)
        RIGHT_WRIST to RIGHT_THUMB,
        RIGHT_WRIST to RIGHT_PINKY,
        RIGHT_PINKY to RIGHT_INDEX
    )

    // Define all the points we care about drawing
    val allPoints = connections.flatMap { listOf(it.first, it.second) }.toSet() */

    // Solo mantener las conexiones para el brazo que estás detectando
    const val SHOULDER = 12 // Cambiar a 11 para izquierdo
    const val ELBOW = 14    // Cambiar a 13 para izquierdo
    const val WRIST = 16    // Cambiar a 15 para izquierdo

    val connections = listOf(
        SHOULDER to ELBOW,
        ELBOW to WRIST
    )

    val allPoints = connections.flatMap { listOf(it.first, it.second) }.toSet()

}


@Composable
fun CameraView(
    poseDetector: PoseDetector,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    LaunchedEffect(cameraProviderFuture) {
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val imageAnalyzer = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also {
                it.setAnalyzer(Executors.newSingleThreadExecutor()) { imageProxy ->
                    poseDetector.detectLiveStream(imageProxy)
                    imageProxy.close()
                }
            }

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_FRONT_CAMERA, // Use front camera
                preview,
                imageAnalyzer
            )
        } catch (exc: Exception) {
            Log.e("CameraView", "Use case binding failed", exc)
        }
    }

    AndroidView({ previewView }, modifier = modifier)
}

@Composable
fun LandmarkOverlay(
    result: PoseLandmarkerResult,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val landmarks = result.landmarks().firstOrNull() ?: return@Canvas

        // Draw connections
        LandmarkConnections.connections.forEach { (startIdx, endIdx) ->
            val startLandmark = landmarks[startIdx]
            val endLandmark = landmarks[endIdx]
            if (startLandmark.visibility().orElse(0f) > 0.5f && endLandmark.visibility().orElse(0f) > 0.5f) {
                drawLine(
                    color = Color.Blue,
                    start = Offset(startLandmark.x() * size.width, startLandmark.y() * size.height),
                    end = Offset(endLandmark.x() * size.width, endLandmark.y() * size.height),
                    strokeWidth = 8f
                )
            }
        }

        // ✅ --- FIX 2: Draw ONLY the dots for the arms and hands ---
        // Iterate through our defined points instead of all landmarks
        LandmarkConnections.allPoints.forEach { index ->
            val landmark = landmarks[index]
            if (landmark.visibility().orElse(0f) > 0.5f) { // Only draw visible landmarks
                drawCircle(
                    color = Color.Yellow,
                    radius = 15f,
                    center = Offset(landmark.x() * size.width, landmark.y() * size.height)
                )
            }
        }
    }
}