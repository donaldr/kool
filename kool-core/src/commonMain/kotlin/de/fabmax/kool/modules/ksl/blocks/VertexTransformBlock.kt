package de.fabmax.kool.modules.ksl.blocks

import de.fabmax.kool.modules.ksl.BasicVertexConfig
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.Attribute

fun KslScopeBuilder.vertexTransformBlock(cfg: BasicVertexConfig, block: VertexTransformBlock.() -> Unit): VertexTransformBlock {
    val vertexBlock = VertexTransformBlock(cfg, parentStage.program.nextName("vertexBlock"), this)
    vertexBlock.block()
    ops += vertexBlock
    return vertexBlock
}

class VertexTransformBlock(cfg: BasicVertexConfig, name: String, parentScope: KslScopeBuilder) : KslBlock(name, parentScope) {
    val inModelMat = inMat4()
    val inLocalPos = inFloat3()
    val inLocalNormal = inFloat3(defaultValue = KslValueFloat3(0f, 0f, 0f))
    val inLocalTangent = inFloat4(defaultValue = KslValueFloat4(0f, 0f, 0f, 0f))

    val outModelMat = outMat4()
    val outWorldPos = outFloat3()
    val outWorldNormal = outFloat3()
    val outWorldTangent = outFloat4()

    init {
        body.apply {
            val stage = parentStage as? KslVertexStage ?: throw IllegalStateException("VertexTransformBlock is only allowed in vertex stage")

            outModelMat set inModelMat
            val localPos = float3Var(inLocalPos)
            val localNormal = float3Var(inLocalNormal)
            val localTangent = float4Var(inLocalTangent)

            if (cfg.isInstanced) {
                val instanceModelMat = stage.instanceAttribMat4(Attribute.INSTANCE_MODEL_MAT.name)
                outModelMat *= instanceModelMat
            }

            if (cfg.isArmature) {
                val armatureBlock = armatureBlock(cfg.maxNumberOfBones)
                armatureBlock.inBoneWeights(stage.vertexAttribFloat4(Attribute.WEIGHTS.name))
                armatureBlock.inBoneIndices(stage.vertexAttribInt4(Attribute.JOINTS.name))
                outModelMat *= armatureBlock.outBoneTransform
            }

            if (cfg.isMorphing) {
                val morphData = stage.program.morphWeightData()
                cfg.morphAttributes.forEachIndexed { i, morphAttrib ->
                    val weight = getMorphWeightComponent(i, morphData)
                    when {
                        morphAttrib.name.startsWith(Attribute.POSITIONS.name) -> {
                            localPos += stage.vertexAttribFloat3(morphAttrib.name) * weight
                        }
                        morphAttrib.name.startsWith(Attribute.NORMALS.name) -> {
                            localNormal += stage.vertexAttribFloat3(morphAttrib.name) * weight
                        }
                        morphAttrib.name.startsWith(Attribute.TANGENTS.name) -> {
                            localTangent.xyz += stage.vertexAttribFloat3(morphAttrib.name) * weight
                        }
                    }
                }
            }

            if (!cfg.displacementCfg.isEmptyOrConst(0f)) {
                val displacement = vertexDisplacementBlock(cfg.displacementCfg).outProperty
                localPos += normalize(localNormal) * displacement
            }

            outWorldPos set (outModelMat * float4Value(localPos, 1f)).xyz
            outWorldNormal set normalize((outModelMat * float4Value(localNormal, 0f)).xyz)
            outWorldTangent set float4Value(float4Value((outModelMat * localTangent).xyz, 0f).xyz, inLocalTangent.w)
        }
    }

    private fun getMorphWeightComponent(iMorphAttrib: Int, morphData: MorphWeightData): KslExprFloat1 {
        val component = when (iMorphAttrib % 4) {
            0 -> "x"
            1 -> "y"
            2 -> "z"
            else -> "w"
        }
        val weights = if (iMorphAttrib < 4) morphData.weightsA else morphData.weightsB
        return weights.float1(component)
    }
}