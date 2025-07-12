package upvictoria.pm_ene_abr_2024.iti_271415.z_u2_iti_271415_e_05

import android.content.Context
import android.util.Log
import android.view.MotionEvent
import org.rajawali3d.Object3D
import org.rajawali3d.lights.DirectionalLight
import org.rajawali3d.loader.LoaderOBJ
import org.rajawali3d.loader.ParsingException
import org.rajawali3d.materials.Material
import org.rajawali3d.materials.methods.DiffuseMethod
import org.rajawali3d.materials.methods.SpecularMethod
import org.rajawali3d.materials.textures.ATexture.TextureException
import org.rajawali3d.materials.textures.Texture
import org.rajawali3d.math.vector.Vector3
import org.rajawali3d.renderer.Renderer

class RenderObj(context: Context) : Renderer(context) {

    private val TAG = "Renderer"
    private val scaleFactor = 1.0
    private val objects: HashMap<Int, Object3D?> = HashMap()
    val ANGLE_STEP = 3.0f
    var cameraPosition: Vector3 = Vector3(0.0, 4.0, 10.0)

    init {
        frameRate = 60.0
        // Every imported object should be added into this map
        objects[R.raw.base_1] = null
        objects[R.raw.base_2] = null
        objects[R.raw.arm_1] = null
        objects[R.raw.arm_2] = null
        objects[R.raw.wrist_1] = null
        objects[R.raw.wrist_2] = null
        objects[R.raw.gear_1] = null
        objects[R.raw.gear_2] = null
        objects[R.raw.link_1] = null
        objects[R.raw.link_2] = null
        objects[R.raw.gripper_1] = null
        objects[R.raw.gripper_2] = null
    }

    override fun initScene() {
        val key = DirectionalLight(-3.0, -4.0, -5.0)
        key.power = 2f
        currentScene.addLight(key)

        val mBlue = Material().apply {
            enableLighting(true)
            diffuseMethod = DiffuseMethod.Lambert()
            colorInfluence = 0f
        }
        val mSilver = Material().apply {
            enableLighting(true)
            diffuseMethod = DiffuseMethod.Lambert()
            colorInfluence = 0f
            specularMethod = SpecularMethod.Phong()
        }

        try {
            val blueTexture = Texture("Blue", R.drawable.blue_texture)
            val silverTexture = Texture("Silver", R.drawable.silver_texture)
            mBlue.addTexture(blueTexture)
            mSilver.addTexture(silverTexture)

            val keys = ArrayList(objects.keys)
            for (keyId in keys) {
                val loader = LoaderOBJ(context.resources, textureManager, keyId)
                loader.parse()
                objects[keyId] = loader.parsedObject
                currentScene.addChild(objects[keyId])
            }

            // Add children relationships
            objects[R.raw.base_1]?.addChild(objects[R.raw.base_2])
            objects[R.raw.base_2]?.addChild(objects[R.raw.arm_1])
            objects[R.raw.arm_1]?.addChild(objects[R.raw.arm_2])
            objects[R.raw.arm_2]?.addChild(objects[R.raw.wrist_1])
            objects[R.raw.wrist_1]?.addChild(objects[R.raw.wrist_2])
            objects[R.raw.wrist_2]?.addChild(objects[R.raw.gear_1])
            objects[R.raw.wrist_2]?.addChild(objects[R.raw.gear_2])
            objects[R.raw.wrist_2]?.addChild(objects[R.raw.link_1])
            objects[R.raw.wrist_2]?.addChild(objects[R.raw.link_2])
            objects[R.raw.gear_1]?.addChild(objects[R.raw.gripper_1])
            objects[R.raw.gear_2]?.addChild(objects[R.raw.gripper_2])

            // Initial setup
            setupObjects(mBlue, mSilver)

            currentScene.setSkybox(
                R.drawable.posx, R.drawable.negx, R.drawable.posy,
                R.drawable.negy, R.drawable.posz, R.drawable.negz
            )
        } catch (e: ParsingException) {
            Log.d("$TAG.initScene", "Couldn't parse file\n$e")
        } catch (e: TextureException) {
            Log.d("$TAG.initScene", e.toString())
        } catch (e: NullPointerException) {
            Log.d("$TAG.initScene", "An object file hasn't been initialized correctly")
        }

        currentCamera.position = cameraPosition
        currentCamera.setLookAt(0.0, 2.0, 0.0)
    }

    fun setArmLowRotation(degrees: Double) {
        val arm1 = objects[R.raw.arm_1] ?: return
        // Clamp the angle to prevent impossible poses (-10 to 50 degrees)
        val clampedDegrees = degrees.coerceIn(-10.0, 50.0)
        arm1.rotZ = clampedDegrees
    }

    fun setArmHighRotation(degrees: Double) {
        val arm2 = objects[R.raw.arm_2] ?: return
        // Clamp the angle (-40 to 20 degrees)
        val clampedDegrees = degrees.coerceIn(-40.0, 20.0)
        arm2.rotZ = clampedDegrees
    }

    // We can also add one for the base
    fun setBaseRotation(degrees: Double) {
        val base2 = objects[R.raw.base_2] ?: return
        base2.rotY = degrees
    }

    private fun setupObjects(mBlue: Material, mSilver: Material) {
        objects[R.raw.base_1]?.apply { setScale(scaleFactor); material = mSilver }
        objects[R.raw.base_2]?.apply { setScale(scaleFactor); material = mBlue }
        objects[R.raw.arm_1]?.apply { setScale(scaleFactor); material = mBlue; moveUp(1.25); moveRight(0.170); moveForward(-0.245) }
        objects[R.raw.arm_2]?.apply { setScale(scaleFactor); material = mBlue; moveUp(1.4); moveRight(-0.50); moveForward(0.12) }
        objects[R.raw.wrist_1]?.apply { setScale(scaleFactor); material = mBlue; moveRight(1.344); moveForward(-0.021) }
        objects[R.raw.wrist_2]?.apply { setScale(scaleFactor); material = mSilver; moveForward(0.125); moveRight(0.415) }
        objects[R.raw.gear_1]?.apply { setScale(scaleFactor); material = mSilver; moveRight(0.595); moveForward(0.226) }
        objects[R.raw.link_1]?.apply { setScale(scaleFactor); material = mSilver; moveRight(0.911); moveForward(0.102) }
        objects[R.raw.gripper_1]?.apply { setScale(scaleFactor); material = mSilver; moveRight(0.446); moveForward(0.183) }
        objects[R.raw.gear_2]?.apply { setScale(scaleFactor); material = mSilver; moveRight(0.595); moveForward(-0.195) }
        objects[R.raw.link_2]?.apply { setScale(scaleFactor); material = mSilver; moveRight(0.911); moveForward(-0.056) }
        objects[R.raw.gripper_2]?.apply { setScale(scaleFactor); material = mSilver; moveRight(0.446); moveForward(-0.172) }
    }

    override fun onOffsetsChanged(xOffset: Float, yOffset: Float, xOffsetStep: Float, yOffsetStep: Float, xPixelOffset: Int, yPixelOffset: Int) {}
    override fun onTouchEvent(event: MotionEvent?) {}

    override fun onRender(elapsedTime: Long, deltaTime: Double) {
        super.onRender(elapsedTime, deltaTime)
    }

    fun rotateBase(isPositive: Boolean) {
        objects[R.raw.base_2]?.rotate(Vector3.Axis.Y, if (isPositive) ANGLE_STEP.toDouble() else -ANGLE_STEP.toDouble())
    }

    fun rotateArmLow(isPositive: Boolean) {
        objects[R.raw.arm_1]?.let {
            if ((isPositive && it.rotZ < 51) || (!isPositive && it.rotZ > -10.2)) {
                it.rotate(Vector3.Axis.Z, if (isPositive) ANGLE_STEP.toDouble() else -ANGLE_STEP.toDouble())
            }
        }
        objects[R.raw.base_1]?.rotate(Vector3.Axis.Z, 0.0) // Flickering fix
    }

    fun rotateArmHigh(isPositive: Boolean) {
        objects[R.raw.arm_2]?.let {
            if ((isPositive && it.rotZ < 21.21) || (!isPositive && it.rotZ > -40.8)) {
                it.rotate(Vector3.Axis.Z, if (isPositive) ANGLE_STEP.toDouble() else -ANGLE_STEP.toDouble())
            }
        }
        objects[R.raw.base_1]?.rotate(Vector3.Axis.Z, 0.0) // Flickering fix
    }

    fun rotateArmWristAround(isPositive: Boolean) {
        objects[R.raw.wrist_1]?.rotate(Vector3.Axis.X, if (isPositive) ANGLE_STEP.toDouble() else -ANGLE_STEP.toDouble())
        objects[R.raw.base_1]?.rotate(Vector3.Axis.Z, 0.0) // Flickering fix
    }

    fun rotateArmWrist(isPositive: Boolean) {
        objects[R.raw.wrist_2]?.let {
            if ((isPositive && it.rotZ < 48.96) || (!isPositive && it.rotZ > -48.96)) {
                it.rotate(Vector3.Axis.Z, if (isPositive) ANGLE_STEP.toDouble() else -ANGLE_STEP.toDouble())
            }
        }
        objects[R.raw.base_1]?.rotate(Vector3.Axis.Z, 0.0) // Flickering fix
    }

    fun openHand(isPositive: Boolean) {
        objects[R.raw.gear_1]?.let {
            if ((isPositive && it.rotY < 36.72) || (!isPositive && it.rotY > -10.2)) {
                val gear1Angle = if (isPositive) ANGLE_STEP.toDouble() else -ANGLE_STEP.toDouble()
                val gear2Angle = if (isPositive) -ANGLE_STEP.toDouble() else ANGLE_STEP.toDouble()

                objects[R.raw.gear_1]?.rotate(Vector3.Axis.Y, gear1Angle)
                objects[R.raw.gear_2]?.rotate(Vector3.Axis.Y, gear2Angle)
                objects[R.raw.link_1]?.rotate(Vector3.Axis.Y, gear1Angle)
                objects[R.raw.link_2]?.rotate(Vector3.Axis.Y, gear2Angle)
                objects[R.raw.gripper_1]?.rotate(Vector3.Axis.Y, gear2Angle)
                objects[R.raw.gripper_2]?.rotate(Vector3.Axis.Y, gear1Angle)

                objects[R.raw.base_1]?.rotate(Vector3.Axis.Z, 0.0) // Flickering fix
            }
        }
    }

    // En RenderObj.kt
    fun setWristRotation(degrees: Double) {
        val wrist = objects[R.raw.wrist_2] ?: return
        wrist.rotZ = degrees.coerceIn(-45.0, 45.0)
    }

    fun setWristElevation(degrees: Double) {
        val forearm = objects[R.raw.arm_2] ?: return
        forearm.rotY = degrees.coerceIn(0.0, 60.0)
    }


}