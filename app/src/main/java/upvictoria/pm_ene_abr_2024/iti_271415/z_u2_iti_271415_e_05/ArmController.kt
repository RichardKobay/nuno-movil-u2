// Yo fregandome to el ArmController xd

package upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271415_e_05

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.atan2
import kotlin.math.sqrt
import kotlin.math.acos
import kotlin.math.pow

class ArmController(private val selectedArm: Int = 12){
    // Todas las variables como Float para consistencia
    private val smoothingFactor = 0.3f
    private var smoothedShoulder: Float? = null
    private var smoothedElbow: Float? = null
    private var smoothedForearmRotation: Float? = null
    private var smoothedForearmElevation: Float? = null

    private fun getArmLandmarks(landmarks: List<NormalizedLandmark>): Map<String, NormalizedLandmark>? {
        // Elige explícitamente qué brazo quieres detectar (cambia a 11 para izquierdo)
        val selectedShoulderIndex = 12 // 12 = derecho, 11 = izquierdo

        val shoulder = landmarks.getOrNull(selectedShoulderIndex) ?: return null
        val elbowIndex = if (selectedShoulderIndex == 12) 14 else 13
        val wristIndex = if (selectedShoulderIndex == 12) 16 else 15

        val elbow = landmarks.getOrNull(elbowIndex) ?: return null
        val wrist = landmarks.getOrNull(wristIndex) ?: return null

        // Solo devuelve landmarks si tienen suficiente visibilidad
        return if (shoulder.visibility().orElse(0f) > 0.5f &&
            elbow.visibility().orElse(0f) > 0.5f &&
            wrist.visibility().orElse(0f) > 0.5f) {
            mapOf(
                "shoulder" to shoulder,
                "elbow" to elbow,
                "wrist" to wrist
            )
        } else {
            null
        }
    }

    fun getJointAngles(result: PoseLandmarkerResult): ArmAngles? {
        val landmarks = result.landmarks().firstOrNull() ?: return null
        val armLandmarks = getArmLandmarks(landmarks) ?: return null

        val shoulder = armLandmarks["shoulder"]!!
        val elbow = armLandmarks["elbow"]!!
        val wrist = armLandmarks["wrist"]!!

        // Cálculos convertidos explícitamente a Float
        val rawShoulderAngle = calculateShoulderElevation(shoulder, elbow).toFloat()
        val rawElbowAngle = calculateElbowAngle(wrist, elbow, shoulder).toFloat()
        val rawForearmRotation = calculateForearmRotation(elbow, wrist).toFloat()
        val rawForearmElevation = calculateForearmElevation(shoulder, elbow, wrist).toFloat()

        // Suavizado con valores Float
        smoothedShoulder = smoothValue(smoothedShoulder, rawShoulderAngle)
        smoothedElbow = smoothValue(smoothedElbow, rawElbowAngle)
        smoothedForearmRotation = smoothValue(smoothedForearmRotation, rawForearmRotation)
        smoothedForearmElevation = smoothValue(smoothedForearmElevation, rawForearmElevation)

        return ArmAngles(
            shoulderElevation = smoothedShoulder!!,
            elbowAngle = smoothedElbow!!,
            forearmRotation = smoothedForearmRotation!!,
            forearmElevation = smoothedForearmElevation!!
        )
    }

    private fun smoothValue(current: Float?, newValue: Float): Float {
        return current?.let { it * (1 - smoothingFactor) + newValue * smoothingFactor } ?: newValue
    }

    // Funciones de cálculo que devuelven Double (para precisión)
    private fun calculateShoulderElevation(shoulder: NormalizedLandmark, elbow: NormalizedLandmark): Double {
        val torso = NormalizedLandmark.create(
            shoulder.x().toFloat(),
            (shoulder.y() + 1.0).toFloat(),
            0.0f
        )
        return calculateAngle(elbow, shoulder, torso)
    }

    private fun calculateElbowAngle(wrist: NormalizedLandmark, elbow: NormalizedLandmark, shoulder: NormalizedLandmark): Double {
        return calculateAngle(wrist, elbow, shoulder)
    }

    private fun calculateForearmRotation(elbow: NormalizedLandmark, wrist: NormalizedLandmark): Double {
        val dx = wrist.x() - elbow.x()
        val dy = wrist.y() - elbow.y()
        return Math.toDegrees(atan2(dy.toDouble(), dx.toDouble()))
    }

    private fun calculateForearmElevation(
        shoulder: NormalizedLandmark,
        elbow: NormalizedLandmark,
        wrist: NormalizedLandmark
    ): Double {
        val upperArmX = elbow.x() - shoulder.x()
        val upperArmY = elbow.y() - shoulder.y()
        val forearmX = wrist.x() - elbow.x()
        val forearmY = wrist.y() - elbow.y()

        val dot = upperArmX * forearmX + upperArmY * forearmY
        val magUpper = sqrt(upperArmX.pow(2) + upperArmY.pow(2))
        val magForearm = sqrt(forearmX.pow(2) + forearmY.pow(2))

        val angleRad = acos((dot / (magUpper * magForearm)).toDouble())
        return Math.toDegrees(angleRad)
    }

    private fun calculateAngle(p1: NormalizedLandmark, p2: NormalizedLandmark, p3: NormalizedLandmark): Double {
        val angle1 = atan2(
            (p1.y() - p2.y()).toDouble(),
            (p1.x() - p2.x()).toDouble()
        )
        val angle2 = atan2(
            (p3.y() - p2.y()).toDouble(),
            (p3.x() - p2.x()).toDouble()
        )
        var angle = Math.toDegrees(angle1 - angle2)
        if (angle < 0) angle += 360.0
        return angle
    }
}

data class ArmAngles(
    val shoulderElevation: Float,
    val elbowAngle: Float,
    val forearmRotation: Float,
    val forearmElevation: Float
)