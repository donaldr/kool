package de.fabmax.kool.pipeline.deferred

import de.fabmax.kool.math.Mat4f
import de.fabmax.kool.math.MutableVec3f
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.scene.Light
import de.fabmax.kool.scene.Mesh
import de.fabmax.kool.scene.MeshInstanceList
import de.fabmax.kool.util.Color
import de.fabmax.kool.util.MutableColor


class DeferredPointLights(var isDynamic: Boolean) {
    val lightInstances = mutableListOf<PointLight>()

    private val lightInstanceData = MeshInstanceList(listOf(
        Attribute.INSTANCE_MODEL_MAT,
        DeferredLightShader.LIGHT_POS,
        Attribute.COLORS
    ), 10000)

    private val modelMat = Mat4f()
    private val encodedLightData = FloatArray(8)

    //val lightShader = DeferredLightShader(Light.Type.POINT)
    val lightShader = DeferredLightShader(Light.Type.POINT)

    val mesh = Mesh(listOf(Attribute.POSITIONS)).apply {
        isFrustumChecked = false
        instances = lightInstanceData

        generate {
            icoSphere {
                steps = 0
                radius = 1.176f     // required radius to fully include unit sphere at 0 subdivisions
            }
        }

        shader = lightShader

        onUpdate += {
            if (isDynamic) {
                updateLightData()
            }
        }
    }

    fun updateLightData() {
        lightInstanceData.clear()
        lightInstanceData.addInstances(lightInstances.size) { buf ->
            for (i in 0 until lightInstances.size) {
                encodeLight(lightInstances[i])
                buf.put(modelMat.array)
                buf.put(encodedLightData)
            }
        }
    }

    private fun encodeLight(light: PointLight) {
        modelMat.setIdentity()
        modelMat.translate(light.position)
        modelMat.scale(light.radius, light.radius, light.radius)

        encodedLightData[0] = light.position.x
        encodedLightData[1] = light.position.y
        encodedLightData[2] = light.position.z
        encodedLightData[3] = Light.Type.POINT.encoded

        encodedLightData[4] = light.color.r * light.intensity
        encodedLightData[5] = light.color.g * light.intensity
        encodedLightData[6] = light.color.b * light.intensity
        encodedLightData[7] = 1f
    }

    fun addPointLight(pointLight: PointLight) {
        lightInstances += pointLight
    }

    inline fun addPointLight(block: PointLight.() -> Unit): PointLight {
        val light = PointLight()
        light.block()
        addPointLight(light)
        return light
    }

    fun removePointLight(light: PointLight) {
        lightInstances -= light
    }

    class PointLight {
        val position = MutableVec3f()
        val color = MutableColor(Color.WHITE)
        var radius = 1f
        var intensity = 1f
    }
}