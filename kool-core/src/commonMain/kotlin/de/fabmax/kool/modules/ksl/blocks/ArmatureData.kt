package de.fabmax.kool.modules.ksl.blocks

import de.fabmax.kool.KoolContext
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.modules.ksl.KslShaderListener
import de.fabmax.kool.modules.ksl.lang.KslDataBlock
import de.fabmax.kool.modules.ksl.lang.KslProgram
import de.fabmax.kool.pipeline.Pipeline
import de.fabmax.kool.pipeline.UniformMat4fv
import de.fabmax.kool.pipeline.drawqueue.DrawCommand
import kotlin.math.min

class ArmatureData(maxBones: Int, program: KslProgram) : KslDataBlock, KslShaderListener {
    override val name = NAME

    val boneTransforms = program.uniformMat4Array("uJointTransform", maxBones)
    private var uBoneTransforms: UniformMat4fv? = null

    init {
        if (maxBones > 0) {
            program.shaderListeners += this
        }
    }

    override fun onShaderCreated(shader: KslShader, pipeline: Pipeline, ctx: KoolContext) {
        uBoneTransforms = shader.uniforms["uJointTransform"] as? UniformMat4fv
    }

    override fun onUpdate(cmd: DrawCommand) {
        uBoneTransforms?.let { mats ->
            cmd.mesh.skin?.let {
                for (i in 0 until min(it.nodes.size, mats.length)) {
                    val nd = it.nodes[i]
                    mats.value[i].set(nd.jointTransform)
                }
            }
        }
    }

    companion object {
        const val NAME = "ArmatureData"
    }
}