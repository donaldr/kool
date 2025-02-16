package de.fabmax.kool.modules.ksl.blocks

import de.fabmax.kool.KoolContext
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.KslShaderListener
import de.fabmax.kool.modules.ksl.lang.KslDataBlock
import de.fabmax.kool.modules.ksl.lang.KslProgram
import de.fabmax.kool.pipeline.Pipeline
import de.fabmax.kool.pipeline.Uniform4f
import de.fabmax.kool.pipeline.drawqueue.DrawCommand
import kotlin.math.min

fun KslProgram.morphWeightData(): MorphWeightData {
    return (dataBlocks.find { it is MorphWeightData } as? MorphWeightData) ?: MorphWeightData(this)
}

class MorphWeightData(program: KslProgram) : KslDataBlock, KslShaderListener {
    override val name = NAME

    val weightsA = program.uniformFloat4("uMorphWeightsA")
    val weightsB = program.uniformFloat4("uMorphWeightsB")
    private var uMorphWeightsA: Uniform4f? = null
    private var uMorphWeightsB: Uniform4f? = null

    init {
        program.shaderListeners += this
    }

    override fun onShaderCreated(shader: KslShader, pipeline: Pipeline, ctx: KoolContext) {
        uMorphWeightsA = shader.uniforms["uMorphWeightsA"] as? Uniform4f
        uMorphWeightsB = shader.uniforms["uMorphWeightsB"] as? Uniform4f
    }

    override fun onUpdate(cmd: DrawCommand) {
        cmd.mesh.morphWeights?.let { w ->
            uMorphWeightsA?.let { wA ->
                for (i in 0 until min(4, w.size)) {
                    wA.value[i] = w[i]
                }
            }
            uMorphWeightsB?.let { wB ->
                for (i in 0 until min(4, w.size - 4)) {
                    wB.value[i] = w[i + 4]
                }
            }
        }
    }

    companion object {
        const val NAME = "MorphWeightData"
    }
}