package upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271415_e_05

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.atan2

class ArmController {

    // ✅ 1. Add a smoothing factor. Lower values are smoother but have more delay.
    private val smoothingFactor = 0.3f
    private var smoothedShoulder: Float? = null
    private var smoothedElbow: Float? = null

    // Helper to select which arm's landmarks to use. Prefers right arm.
    private fun getArmLandmarks(landmarks: List<NormalizedLandmark>): Map<Int, NormalizedLandmark>? {
        val rightShoulder = landmarks.getOrNull(12)
        val leftShoulder = landmarks.getOrNull(11)

        val useRightArm = (rightShoulder?.visibility()?.orElse(0f) ?: 0f) > 0.5f
        val useLeftArm = (leftShoulder?.visibility()?.orElse(0f) ?: 0f) > 0.5f

        return when {
            useRightArm -> mapOf(12 to landmarks[12], 14 to landmarks[14], 16 to landmarks[16])
            useLeftArm -> mapOf(11 to landmarks[11], 13 to landmarks[13], 15 to landmarks[15])
            else -> null
        }
    }

    fun getJointAngles(result: PoseLandmarkerResult): ArmAngles? {
        val landmarks = result.landmarks().firstOrNull() ?: return null
        val armLandmarks = getArmLandmarks(landmarks) ?: return null

        val shoulder = armLandmarks.values.elementAt(0)
        val elbow = armLandmarks.values.elementAt(1)
        val wrist = armLandmarks.values.elementAt(2)

        // Calculate raw angles for this frame
        val rawShoulderAngle = calculateShoulderElevation(shoulder, elbow).toFloat()
        val rawElbowAngle = calculateElbowAngle(wrist, elbow, shoulder).toFloat()

        // ✅ 2. Apply smoothing to the raw angles
        smoothedShoulder = applySmoothing(smoothedShoulder, rawShoulderAngle)
        smoothedElbow = applySmoothing(smoothedElbow, rawElbowAngle)

        return ArmAngles(smoothedShoulder!!, smoothedElbow!!)
    }

    // Applies an exponential moving average
    private fun applySmoothing(previous: Float?, current: Float): Float {
        return if (previous != null) {
            previous * (1 - smoothingFactor) + current * smoothingFactor
        } else {
            current // No previous value, so start with the current one
        }
    }

    private fun calculateShoulderElevation(shoulder: NormalizedLandmark, elbow: NormalizedLandmark): Double {
        val torso = NormalizedLandmark.create(shoulder.x(), shoulder.y() + 1f, 0f)
        return calculateAngle(elbow, shoulder, torso)
    }

    private fun calculateElbowAngle(p1: NormalizedLandmark, p2: NormalizedLandmark, p3: NormalizedLandmark): Double {
        val angle1 = atan2(p1.y() - p2.y(), p1.x() - p2.x())
        val angle2 = atan2(p3.y() - p2.y(), p3.x() - p2.x())
        var angle = Math.toDegrees((angle1 - angle2).toDouble())
        if (angle < 0) angle += 360
        // Flip angle for the right arm to be consistent
        if (p2.x() > p3.x()) { // Adjusted for mirrored view
            return 360 - angle
        }
        return angle
    }

    // ✅ 3. Removed the duplicate calculateAngle function. This is the only one needed.
    private fun calculateAngle(p1: NormalizedLandmark, p2: NormalizedLandmark, p3: NormalizedLandmark): Double {
        val angle1 = atan2(p1.y() - p2.y(), p1.x() - p2.x())
        val angle2 = atan2(p3.y() - p2.y(), p3.x() - p2.x())
        val angle = Math.toDegrees((angle1 - angle2).toDouble())
        return if (angle < 0) angle + 360 else angle
    }
}

data class ArmAngles(val shoulderElevation: Float, val elbowAngle: Float)