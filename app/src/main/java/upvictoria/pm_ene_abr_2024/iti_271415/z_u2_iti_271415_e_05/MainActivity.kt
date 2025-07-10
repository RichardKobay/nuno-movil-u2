package upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271415_e_05

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271415_e_05.ui.theme.Z_U2_iti271415_E_05Theme
import upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271515_e_05.CameraView
import upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271515_e_05.LandmarkOverlay
import androidx.compose.ui.draw.scale

// ✅ --- NEW: Helper function to map a value from one range to another ---
fun mapRange(value: Double, fromMin: Double, fromMax: Double, toMin: Double, toMax: Double): Double {
    return toMin + (value - fromMin) * (toMax - toMin) / (fromMax - fromMin)
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Permission granted, you can now launch the camera
            } else {
                // Handle permission denial
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request camera permission
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) -> {
                // You can use the API that requires the permission.
            }
            else -> {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }

        setContent {
            Z_U2_iti271415_E_05Theme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    RoboticArmVisionScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun RoboticArmVisionScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    var poseResult by remember { mutableStateOf<PoseLandmarkerResult?>(null) }

    val armController = remember { ArmController() }
    val renderer = remember { RenderObj(context) }
    val poseDetector = remember {
        PoseDetector(
            context = context,
            onResults = {
                poseResult = it.result
            }
        )
    }

    // This effect block will now run every time a new poseResult is detected
    LaunchedEffect(poseResult) {
        poseResult?.let { result ->
            val angles = armController.getJointAngles(result)
            if (angles != null) {

                // ✅ --- FIX: Use mapRange for accurate and tunable control ---

                // Map shoulder elevation to the robot's lower arm rotation.
                // Human arm down (180deg) to horizontal (90deg) -> Robot arm low (-10deg) to high (50deg)
                val armLowAngle = mapRange(
                    value = angles.shoulderElevation.toDouble(),
                    fromMin = 180.0,
                    fromMax = 90.0,
                    toMin = -10.0,
                    toMax = 50.0
                )

                // Map elbow angle to the robot's upper arm rotation.
                // Human arm bent (~60deg) to straight (180deg) -> Robot arm high (-40deg) to low (20deg)
                val armHighAngle = mapRange(
                    value = angles.elbowAngle.toDouble(),
                    fromMin = 60.0,
                    fromMax = 180.0,
                    toMin = -40.0,
                    toMax = 20.0
                )

                renderer.setArmLowRotation(armLowAngle)
                renderer.setArmHighRotation(armHighAngle)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // --- Top Half: Camera and Pose Detection ---
        Box(modifier = Modifier.weight(1f)) {
            CameraView(
                poseDetector = poseDetector,
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scaleX = -1f, scaleY = 1f)
            )
            poseResult?.let {
                LandmarkOverlay(
                    result = it,
                    modifier = Modifier
                        .fillMaxSize()
                        .scale(scaleX = -1f, scaleY = 1f)
                )
            }
        }

        // --- Bottom Half: 3D Robotic Arm View ---
        Box(modifier = Modifier.weight(1f)) {
            AndroidView(
                factory = { ctx ->
                    org.rajawali3d.view.SurfaceView(ctx).apply {
                        setSurfaceRenderer(renderer)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
