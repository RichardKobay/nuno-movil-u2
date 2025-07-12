package upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271415_e_05

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271415_e_05.ui.theme.Z_U2_iti271415_E_05Theme

// Helper function to map a value from one range to another
fun mapRange(value: Double, fromMin: Double, fromMax: Double, toMin: Double, toMax: Double): Double {
    return toMin + (value - fromMin) * (toMax - toMin) / (fromMax - fromMin)
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) { /* Permission granted */ } else { /* Handle denial */ }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }

        setContent {
            Z_U2_iti271415_E_05Theme {
                CombinedControlScreen()
            }
        }
    }
}

@Composable
fun CombinedControlScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current

    // State for MediaPipe results
    var poseResult by remember { mutableStateOf<PoseLandmarkerResult?>(null) }

    // Controllers
    val renderer = remember { RenderObj(context) }
    val armController = remember { ArmController() } // For calculating angles
    val poseDetector = remember {
        PoseDetector(
            context = context,
            onResults = { poseResult = it.result }
        )
    }

    // ✅ --- This effect block connects the vision to the robot ---
    LaunchedEffect(poseResult) {
        poseResult?.let { result ->
            val angles = armController.getJointAngles(result)
            if (angles != null) {
                // --- GREEN PART MAPPING ---
                // Map your shoulder elevation to the robot's lower arm rotation.
                val armLowAngle = mapRange(
                    value = angles.shoulderElevation.toDouble(),
                    fromMin = 180.0, // Your arm hanging down
                    fromMax = 30.0,  // Your arm raised high
                    toMin = -10.0, // Robot arm lowest point
                    toMax = 50.0   // Robot arm highest point
                )

                // --- PINK PART MAPPING ---
                // Map your elbow angle to the robot's upper arm rotation.
                val armHighAngle = mapRange(
                    value = angles.elbowAngle.toDouble(),
                    fromMin = 70.0,  // Your arm bent sharply
                    fromMax = 180.0, // Your arm fully straight
                    toMin = -40.0, // Robot arm most bent
                    toMax = 20.0   // Robot arm straightest
                )

                // ✅ 1. Rotación horizontal (A2+/-)
                val wristRotation = mapRange(
                    value = angles.forearmRotation.toDouble(),
                    fromMin = -90.0,
                    fromMax = 90.0,
                    toMin = -45.0,
                    toMax = 45.0
                )

                // ✅ 2. Elevación vertical (estirado vs encogido)
                val wristElevation = mapRange(
                    value = angles.forearmElevation.toDouble(),
                    fromMin = 30.0,   // Brazo doblado (ejemplo)
                    fromMax = 180.0,   // Brazo estirado
                    toMin = 0.0,       // Posición encogida del robot
                    toMax = 60.0       // Posición estirada del robot
                )

                // Update the robot's joints
                renderer.setArmLowRotation(armLowAngle)
                renderer.setArmHighRotation(armHighAngle)
                renderer.setWristRotation(wristRotation)
                renderer.setWristElevation(wristElevation)


            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            ControlButtons(renderer = renderer)
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- 3D Robotic Arm View ---
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

            // --- Camera and Pose Detection View ---
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
        }
    }
}

@Composable
fun ControlButtons(renderer: RenderObj) {
    // This composable remains unchanged
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = { renderer.rotateBase(true) }) { Text("B+") }
            Button(onClick = { renderer.rotateArmLow(true) }) { Text("A1+") }
            Button(onClick = { renderer.rotateArmHigh(true) }) { Text("A2+") }
            Button(onClick = { renderer.rotateArmWristAround(true) }) { Text("W_A+") }
            Button(onClick = { renderer.rotateArmWrist(true) }) { Text("W+") }
            Button(onClick = { renderer.openHand(true) }) { Text("Open") }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.Center
        ) {
            Button(onClick = { renderer.rotateBase(false) }) { Text("B-") }
            Button(onClick = { renderer.rotateArmLow(false) }) { Text("A1-") }
            Button(onClick = { renderer.rotateArmHigh(false) }) { Text("A2-") }
            Button(onClick = { renderer.rotateArmWristAround(false) }) { Text("W_A-") }
            Button(onClick = { renderer.rotateArmWrist(false) }) { Text("W-") }
            Button(onClick = { renderer.openHand(false) }) { Text("Close") }
        }
    }
}