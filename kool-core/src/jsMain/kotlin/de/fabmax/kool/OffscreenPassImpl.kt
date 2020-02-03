package de.fabmax.kool

import de.fabmax.kool.pipeline.CubeMapTexture
import de.fabmax.kool.pipeline.LoadedTexture
import de.fabmax.kool.pipeline.Texture
import de.fabmax.kool.platform.JsContext
import de.fabmax.kool.platform.WebGL2RenderingContext.Companion.DEPTH_COMPONENT24
import de.fabmax.kool.platform.WebGL2RenderingContext.Companion.TEXTURE_WRAP_R
import de.fabmax.kool.platform.glInternalFormat
import de.fabmax.kool.platform.pxSize
import org.khronos.webgl.WebGLFramebuffer
import org.khronos.webgl.WebGLRenderbuffer
import org.khronos.webgl.WebGLRenderingContext.Companion.CLAMP_TO_EDGE
import org.khronos.webgl.WebGLRenderingContext.Companion.COLOR_ATTACHMENT0
import org.khronos.webgl.WebGLRenderingContext.Companion.COLOR_BUFFER_BIT
import org.khronos.webgl.WebGLRenderingContext.Companion.DEPTH_ATTACHMENT
import org.khronos.webgl.WebGLRenderingContext.Companion.DEPTH_BUFFER_BIT
import org.khronos.webgl.WebGLRenderingContext.Companion.FRAMEBUFFER
import org.khronos.webgl.WebGLRenderingContext.Companion.LINEAR
import org.khronos.webgl.WebGLRenderingContext.Companion.LINEAR_MIPMAP_LINEAR
import org.khronos.webgl.WebGLRenderingContext.Companion.RENDERBUFFER
import org.khronos.webgl.WebGLRenderingContext.Companion.TEXTURE_2D
import org.khronos.webgl.WebGLRenderingContext.Companion.TEXTURE_CUBE_MAP
import org.khronos.webgl.WebGLRenderingContext.Companion.TEXTURE_CUBE_MAP_POSITIVE_X
import org.khronos.webgl.WebGLRenderingContext.Companion.TEXTURE_MAG_FILTER
import org.khronos.webgl.WebGLRenderingContext.Companion.TEXTURE_MIN_FILTER
import org.khronos.webgl.WebGLRenderingContext.Companion.TEXTURE_WRAP_S
import org.khronos.webgl.WebGLRenderingContext.Companion.TEXTURE_WRAP_T
import org.khronos.webgl.WebGLTexture

actual class OffscreenPass2dImpl actual constructor(val offscreenPass: OffscreenPass2d) {
    actual val texture: Texture = OffscreenTexture()

    private var fbos: List<WebGLFramebuffer?> = mutableListOf()
    private var rbos: List<WebGLRenderbuffer?> = mutableListOf()

    private var isSetup = false

    private fun setup(ctx: JsContext) {
        val gl = ctx.gl

        texture as OffscreenTexture
        texture.create(ctx)

        for (i in 0 until offscreenPass.mipLevels) {
            val fbo = gl.createFramebuffer()
            val rbo = gl.createRenderbuffer()

            gl.bindFramebuffer(FRAMEBUFFER, fbo)
            gl.bindRenderbuffer(RENDERBUFFER, rbo)
            gl.renderbufferStorage(RENDERBUFFER, DEPTH_COMPONENT24, offscreenPass.texWidth shr i, offscreenPass.texHeight shr i)
            gl.framebufferRenderbuffer(FRAMEBUFFER, DEPTH_ATTACHMENT, RENDERBUFFER, rbo)
            gl.framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0, TEXTURE_2D, texture.offscreenTex, i)

            fbos += fbo
            rbos += rbo
        }

        isSetup = true
    }

    fun draw(ctx: JsContext) {
        if (!isSetup) {
            setup(ctx)
        }
        texture as OffscreenTexture

        val mipLevel = offscreenPass.targetMipLevel
        val width = offscreenPass.mipWidth(mipLevel)
        val height = offscreenPass.mipHeight(mipLevel)
        val clearColor = offscreenPass.clearColor
        val fboIdx = if (mipLevel < 0) 0 else mipLevel

        ctx.gl.bindFramebuffer(FRAMEBUFFER, fbos[fboIdx])
        ctx.gl.viewport(0, 0, width, height)
        ctx.gl.clearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a)
        ctx.gl.clear(COLOR_BUFFER_BIT or DEPTH_BUFFER_BIT)
        ctx.queueRenderer.renderQueue(offscreenPass.drawQueues[0])
        ctx.gl.bindFramebuffer(FRAMEBUFFER, null)
    }

    private inner class OffscreenTexture : Texture(loader = null) {
        var offscreenTex: WebGLTexture? = null

        fun create(ctx: JsContext) {
            val gl = ctx.gl

            val intFormat = offscreenPass.colorFormat.glInternalFormat
            val width = offscreenPass.texWidth
            val height = offscreenPass.texHeight

            offscreenTex = gl.createTexture()
            gl.bindTexture(TEXTURE_2D, offscreenTex)
            gl.texStorage2D(TEXTURE_2D, offscreenPass.mipLevels, intFormat, width, height)

            gl.texParameteri(TEXTURE_2D, TEXTURE_WRAP_S, CLAMP_TO_EDGE)
            gl.texParameteri(TEXTURE_2D, TEXTURE_WRAP_T, CLAMP_TO_EDGE)
            gl.texParameteri(TEXTURE_2D, TEXTURE_MIN_FILTER, LINEAR_MIPMAP_LINEAR)
            gl.texParameteri(TEXTURE_2D, TEXTURE_MAG_FILTER, LINEAR)

            val estSize = estimatedTexSize(width, height, offscreenPass.colorFormat.pxSize, 1, offscreenPass.mipLevels)
            loadedTexture = LoadedTexture(ctx, offscreenTex, estSize)
            loadingState = LoadingState.LOADED
        }
    }
}

actual class OffscreenPassCubeImpl actual constructor(val offscreenPass: OffscreenPassCube) {
    actual val texture: CubeMapTexture = OffscreenTextureCube()

    private var fbos: List<WebGLFramebuffer?> = mutableListOf()
    private var rbos: List<WebGLRenderbuffer?> = mutableListOf()

    private var isSetup = false

    private fun setup(ctx: JsContext) {
        val gl = ctx.gl

        texture as OffscreenTextureCube
        texture.create(ctx)

        for (i in 0 until offscreenPass.mipLevels) {
            val fbo = gl.createFramebuffer()
            val rbo = gl.createRenderbuffer()

            gl.bindFramebuffer(FRAMEBUFFER, fbo)
            gl.bindRenderbuffer(RENDERBUFFER, rbo)
            gl.renderbufferStorage(RENDERBUFFER, DEPTH_COMPONENT24, offscreenPass.texWidth shr i, offscreenPass.texHeight shr i)
            gl.framebufferRenderbuffer(FRAMEBUFFER, DEPTH_ATTACHMENT, RENDERBUFFER, rbo)

            fbos += fbo
            rbos += rbo
        }

        isSetup = true
    }

    fun draw(ctx: JsContext) {
        if (!isSetup) {
            setup(ctx)
        }
        texture as OffscreenTextureCube

        val mipLevel = offscreenPass.targetMipLevel
        val width = offscreenPass.mipWidth(mipLevel)
        val height = offscreenPass.mipHeight(mipLevel)
        val clearColor = offscreenPass.clearColor
        val fboIdx = if (mipLevel < 0) 0 else mipLevel

        ctx.gl.bindFramebuffer(FRAMEBUFFER, fbos[fboIdx])
        ctx.gl.viewport(0, 0, width, height)
        ctx.gl.clearColor(clearColor.r, clearColor.g, clearColor.b, clearColor.a)
        for (i in 0 until 6) {
            ctx.gl.framebufferTexture2D(FRAMEBUFFER, COLOR_ATTACHMENT0, TEXTURE_CUBE_MAP_POSITIVE_X + i, texture.offscreenTex, fboIdx)
            ctx.gl.clear(COLOR_BUFFER_BIT or DEPTH_BUFFER_BIT)

            val view = VIEWS[i]
            val queue = offscreenPass.drawQueues[view.index]
            ctx.queueRenderer.renderQueue(queue)
        }

        ctx.gl.bindFramebuffer(FRAMEBUFFER, null)
    }

    companion object {
        private val VIEWS = Array(6) { i ->
            when (i) {
                0 -> OffscreenPassCube.ViewDirection.RIGHT
                1 -> OffscreenPassCube.ViewDirection.LEFT
                2 -> OffscreenPassCube.ViewDirection.UP
                3 -> OffscreenPassCube.ViewDirection.DOWN
                4 -> OffscreenPassCube.ViewDirection.FRONT
                5 -> OffscreenPassCube.ViewDirection.BACK
                else -> throw IllegalStateException()
            }
        }
    }

    private inner class OffscreenTextureCube : CubeMapTexture(loader = null) {
        var offscreenTex: WebGLTexture? = null

        fun create(ctx: JsContext) {
            val gl = ctx.gl

            val intFormat = offscreenPass.colorFormat.glInternalFormat
            val width = offscreenPass.texWidth
            val height = offscreenPass.texHeight

            offscreenTex = gl.createTexture()
            gl.bindTexture(TEXTURE_CUBE_MAP, offscreenTex)
            gl.texStorage2D(TEXTURE_CUBE_MAP, offscreenPass.mipLevels, intFormat, width, height)

            gl.texParameteri(TEXTURE_CUBE_MAP, TEXTURE_WRAP_S, CLAMP_TO_EDGE)
            gl.texParameteri(TEXTURE_CUBE_MAP, TEXTURE_WRAP_T, CLAMP_TO_EDGE)
            gl.texParameteri(TEXTURE_CUBE_MAP, TEXTURE_WRAP_R, CLAMP_TO_EDGE)
            gl.texParameteri(TEXTURE_CUBE_MAP, TEXTURE_MIN_FILTER, LINEAR_MIPMAP_LINEAR)
            gl.texParameteri(TEXTURE_CUBE_MAP, TEXTURE_MAG_FILTER, LINEAR)

            val estSize = estimatedTexSize(width, height, offscreenPass.colorFormat.pxSize, 6, offscreenPass.mipLevels)
            loadedTexture = LoadedTexture(ctx, offscreenTex, estSize)
            loadingState = LoadingState.LOADED
        }
    }
}