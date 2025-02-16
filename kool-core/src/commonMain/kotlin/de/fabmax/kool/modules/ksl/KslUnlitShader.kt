package de.fabmax.kool.modules.ksl

import de.fabmax.kool.math.Vec4f
import de.fabmax.kool.modules.ksl.blocks.*
import de.fabmax.kool.modules.ksl.lang.*
import de.fabmax.kool.pipeline.Attribute
import de.fabmax.kool.pipeline.BlendMode
import de.fabmax.kool.pipeline.Texture2d
import de.fabmax.kool.pipeline.shading.AlphaMode
import de.fabmax.kool.util.copy

open class KslUnlitShader(cfg: UnlitShaderConfig, model: KslProgram = Model(cfg)) : KslShader(model, cfg.pipelineCfg) {

    constructor(block: UnlitShaderConfig.() -> Unit) : this(UnlitShaderConfig().apply(block))

    var color: Vec4f by colorUniform(cfg.colorCfg)
    var colorMap: Texture2d? by colorTexture(cfg.colorCfg)

    val colorCfg = ColorBlockConfig(cfg.colorCfg.colorName, cfg.colorCfg.colorSources.copy().toMutableList())

    open class UnlitShaderConfig {
        val vertexCfg = BasicVertexConfig()
        val colorCfg = ColorBlockConfig("baseColor")
        val pipelineCfg = PipelineConfig()

        var colorSpaceConversion = ColorSpaceConversion.AS_IS
        var alphaMode: AlphaMode = AlphaMode.Blend

        var modelCustomizer: (KslProgram.() -> Unit)? = null

        fun vertices(block: BasicVertexConfig.() -> Unit) {
            vertexCfg.block()
        }

        fun color(block: ColorBlockConfig.() -> Unit) {
            colorCfg.apply(block)
        }

        fun pipeline(block: PipelineConfig.() -> Unit) {
            pipelineCfg.apply(block)
        }
    }

    class Model(cfg: UnlitShaderConfig) : KslProgram("Unlit Shader") {
        init {
            vertexStage {
                main {
                    val viewProj = mat4Var(cameraData().viewProjMat)
                    val vertexBlock = vertexTransformBlock(cfg.vertexCfg) {
                        inModelMat(modelMatrix().matrix)
                        inLocalPos(vertexAttribFloat3(Attribute.POSITIONS.name))
                    }
                    outPosition set viewProj * float4Value(vertexBlock.outWorldPos, 1f)
                }
            }
            fragmentStage {
                main {
                    val colorBlock = fragmentColorBlock(cfg.colorCfg)
                    val baseColorPort = float4Port("baseColor", colorBlock.outColor)

                    val baseColor = float4Var(baseColorPort)
                    when (val alphaMode = cfg.alphaMode) {
                        is AlphaMode.Blend -> { }
                        is AlphaMode.Opaque -> baseColor.a set 1f.const
                        is AlphaMode.Mask -> {
                            `if`(baseColorPort.a lt alphaMode.cutOff.const) {
                                discard()
                            }
                        }
                    }

                    val outRgb = float3Var(baseColor.rgb)
                    if (cfg.pipelineCfg.blendMode == BlendMode.BLEND_PREMULTIPLIED_ALPHA) {
                        outRgb set outRgb * baseColor.a
                    }
                    outRgb set convertColorSpace(outRgb, cfg.colorSpaceConversion)
                    colorOutput(outRgb, baseColor.a)
                }
            }
            cfg.modelCustomizer?.invoke(this)
        }
    }
}