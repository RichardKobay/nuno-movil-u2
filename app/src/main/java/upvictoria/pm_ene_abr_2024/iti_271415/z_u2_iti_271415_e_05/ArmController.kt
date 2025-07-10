package upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271415_e_05

import com.google.mediapipe.tasks.components.containers.NormalizedLandmark
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult
import kotlin.math.atan2
import kotlin.math.sqrt

class ArmController {

    // Helper to select which arm's landmarks to use. Prefers right arm.
    private fun getArmLandmarks(landmarks: List<NormalizedLandmark>): Map<Int, NormalizedLandmark>? {
        val rightShoulder = landmarks.getOrNull(12)
        val leftShoulder = landmarks.getOrNull(11)

        val useRightArm = (rightShoulder?.visibility()?.orElse(0f) ?: 0f) > 0.5f
        val useLeftArm = (leftShoulder?.visibility()?.orElse(0f) ?: 0f) > 0.5f

        return when {
            // Prioritize right arm
            useRightArm -> mapOf(
                12 to landmarks[12], 14 to landmarks[14], 16 to landmarks[16]
            )
            useLeftArm -> mapOf(
                11 to landmarks[11], 13 to landmarks[13], 15 to landmarks[15]
            )
            else -> null
        }
    }

    fun getJointAngles(result: PoseLandmarkerResult): ArmAngles? {
        val landmarks = result.landmarks().firstOrNull() ?: return null
        val armLandmarks = getArmLandmarks(landmarks) ?: return null

        val shoulder = armLandmarks.values.elementAt(0)
        val elbow = armLandmarks.values.elementAt(1)
        val wrist = armLandmarks.values.elementAt(2)

        // ✅ Calculate more stable angles
        val shoulderElevation = calculateShoulderElevation(shoulder, elbow)
        val elbowAngle = calculateElbowAngle(wrist, elbow, shoulder)

        return ArmAngles(shoulderElevation.toFloat(), elbowAngle.toFloat())
    }

    // ✅ --- NEW: Calculates shoulder elevation relative to a vertical line ---
    // This is more stable than measuring against the horizontal.
    private fun calculateShoulderElevation(shoulder: NormalizedLandmark, elbow: NormalizedLandmark): Double {
        // Create a "virtual" point straight down from the shoulder to represent the torso
        val torso = NormalizedLandmark.create(shoulder.x(), shoulder.y() + 1f, 0f)
        return calculateAngle(elbow, shoulder, torso)
    }

    // Renamed for clarity, logic is the same. Calculates the angle formed by three points (p1-p2-p3)
    private fun calculateElbowAngle(p1: NormalizedLandmark, p2: NormalizedLandmark, p3: NormalizedLandmark): Double {
        val angle1 = atan2(p1.y() - p2.y(), p1.x() - p2.x())
        val angle2 = atan2(p3.y() - p2.y(), p3.x() - p2.x())
        var angle = Math.toDegrees((angle1 - angle2).toDouble())
        if (angle < 0) angle += 360
        // Flip angle for the right arm to be consistent
        if (p2.x() < p3.x()) {
            return 360 - angle
        }
        return angle
    }

    // Calculates the angle formed by three points (p1-p2-p3)
    private fun calculateAngle(p1: NormalizedLandmark, p2: NormalizedLandmark, p3: NormalizedLandmark): Double {
        val angle1 = atan2(p1.y() - p2.y(), p1.x() - p2.x())
        val angle2 = atan2(p3.y() - p2.y(), p3.x() - p2.x())
        var angle = Math.toDegrees((angle1 - angle2).toDouble())
        if (angle < 0) angle += 360
        return angle
    }
}

// Renamed for clarity
data class ArmAngles(val shoulderElevation: Float, val elbowAngle: Float)