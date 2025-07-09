package upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271415_e_05

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.SystemClock
import androidx.camera.core.ImageProxy
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.Delegate
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.framework.image.MPImage
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarkerResult

class PoseDetector(
    val context: Context,
    val onResults: (Result) -> Unit
) {
    private var poseLandmarker: PoseLandmarker

    init {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task")
            .setDelegate(Delegate.CPU)
            .build()
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.LIVE_STREAM)
            .setResultListener(this::returnLivestreamResult)
            .setErrorListener(this::returnLivestreamError)
            .build()
        poseLandmarker = PoseLandmarker.createFromOptions(context, options)
    }

    fun detectLiveStream(imageProxy: ImageProxy) {
        val frameTime = SystemClock.uptimeMillis()
        val bitmap = imageProxy.toBitmap()
        val rotatedBitmap = bitmap.rotate(imageProxy.imageInfo.rotationDegrees.toFloat())
        val mpImage = BitmapImageBuilder(rotatedBitmap).build()

        poseLandmarker.detectAsync(mpImage, frameTime)
    }

    private fun returnLivestreamResult(result: PoseLandmarkerResult, input: MPImage) {
        val finishTimeMs = SystemClock.uptimeMillis()
        val inferenceTime = finishTimeMs - result.timestampMs()

        onResults(
            Result(
                result = result,
                inferenceTime = inferenceTime,
                inputWidth = input.width,
                inputHeight = input.height
            )
        )
    }

    private fun returnLivestreamError(error: RuntimeException) {
        // Handle the error
    }

    data class Result(
        val result: PoseLandmarkerResult,
        val inferenceTime: Long,
        val inputWidth: Int,
        val inputHeight: Int
    )

    // Helper to rotate the bitmap from camera
    private fun Bitmap.rotate(degrees: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees) }
        return Bitmap.createBitmap(this, 0, 0, width, height, matrix, true)
    }
}