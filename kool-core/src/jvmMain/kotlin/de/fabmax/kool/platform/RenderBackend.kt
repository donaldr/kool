package de.fabmax.kool.platform

import de.fabmax.kool.math.Mat4d
import de.fabmax.kool.modules.ksl.KslShader
import de.fabmax.kool.pipeline.*
import de.fabmax.kool.util.Viewport

interface RenderBackend {

    val apiName: String
    val deviceName: String

    val glfwWindow: GlfwWindow

    val projCorrectionMatrixScreen: Mat4d
    val projCorrectionMatrixOffscreen: Mat4d
    val depthBiasMatrix: Mat4d

    fun getWindowViewport(result: Viewport)

    fun drawFrame(ctx: Lwjgl3Context)
    fun close(ctx: Lwjgl3Context)
    fun cleanup(ctx: Lwjgl3Context)

    fun uploadTextureToGpu(tex: Texture, data: TextureData)

    fun createOffscreenPass2d(parentPass: OffscreenPass2dImpl): OffscreenPass2dImpl.BackendImpl
    fun createOffscreenPassCube(parentPass: OffscreenPassCubeImpl): OffscreenPassCubeImpl.BackendImpl

    fun generateKslShader(shader: KslShader, pipelineLayout: Pipeline.Layout): ShaderCode
}