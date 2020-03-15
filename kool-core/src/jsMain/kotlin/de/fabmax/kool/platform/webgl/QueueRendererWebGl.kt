package de.fabmax.kool.platform.webgl

import de.fabmax.kool.drawqueue.DrawQueue
import de.fabmax.kool.pipeline.CullMethod
import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.pipeline.Pipeline
import de.fabmax.kool.platform.JsContext
import de.fabmax.kool.platform.WebGL2RenderingContext
import org.khronos.webgl.WebGLRenderingContext.Companion.ALWAYS
import org.khronos.webgl.WebGLRenderingContext.Companion.BACK
import org.khronos.webgl.WebGLRenderingContext.Companion.CULL_FACE
import org.khronos.webgl.WebGLRenderingContext.Companion.DEPTH_TEST
import org.khronos.webgl.WebGLRenderingContext.Companion.FRONT
import org.khronos.webgl.WebGLRenderingContext.Companion.GEQUAL
import org.khronos.webgl.WebGLRenderingContext.Companion.GREATER
import org.khronos.webgl.WebGLRenderingContext.Companion.LEQUAL
import org.khronos.webgl.WebGLRenderingContext.Companion.LESS

class QueueRendererWebGl(val ctx: JsContext) {

    private val gl: WebGL2RenderingContext
        get() = ctx.gl

    private val glAttribs = GlAttribs()
    private val shaderMgr = ShaderManager(ctx)

    fun disposePipelines(pipelines: List<Pipeline>) {
        pipelines.forEach {
            shaderMgr.deleteShader(it)
        }
    }

    fun renderQueue(queue: DrawQueue) {
        for (cmd in queue.commands) {
            cmd.pipeline?.let { pipeline ->
                glAttribs.setupPipelineAttribs(pipeline)

                if (cmd.mesh.geometry.numIndices > 0) {
                    shaderMgr.setupShader(cmd)?.let {
                        if (it.primitiveType != 0 && it.indexType != 0) {
                            val insts = cmd.mesh.instances
                            if (insts == null) {
                                gl.drawElements(it.primitiveType, it.numIndices, it.indexType, 0)
                                ctx.engineStats.addPrimitiveCount(cmd.mesh.geometry.numPrimitives)
                            } else {
                                gl.drawElementsInstanced(it.primitiveType, it.numIndices, it.indexType, 0, insts.numInstances)
                                ctx.engineStats.addPrimitiveCount(cmd.mesh.geometry.numPrimitives * insts.numInstances)
                            }
                            ctx.engineStats.addDrawCommandCount(1)
                        }
                    }
                }
            }
        }
    }

    private inner class GlAttribs {
        var depthTest: DepthCompareOp? = null
        var cullMethod: CullMethod? = null
        var lineWidth = 0f

        fun setupPipelineAttribs(pipeline: Pipeline) {
            setDepthTest(pipeline.depthCompareOp)
            setCullMethod(pipeline.cullMethod)
            if (lineWidth != pipeline.lineWidth) {
                lineWidth = pipeline.lineWidth
                gl.lineWidth(pipeline.lineWidth)
            }
        }

        private fun setCullMethod(cullMethod: CullMethod) {
            if (this.cullMethod != cullMethod) {
                this.cullMethod = cullMethod
                when (cullMethod) {
                    CullMethod.DEFAULT -> {
                        gl.enable(CULL_FACE)
                        gl.cullFace(BACK)
                    }
                    CullMethod.CULL_BACK_FACES -> {
                        gl.enable(CULL_FACE)
                        gl.cullFace(BACK)
                    }
                    CullMethod.CULL_FRONT_FACES -> {
                        gl.enable(CULL_FACE)
                        gl.cullFace(FRONT)
                    }
                    CullMethod.NO_CULLING -> gl.disable(CULL_FACE)
                }
            }
        }

        private fun setDepthTest(depthCompareOp: DepthCompareOp) {
            if (depthTest != depthCompareOp) {
                depthTest = depthCompareOp
                when (depthCompareOp) {
                    DepthCompareOp.DISABLED -> gl.disable(DEPTH_TEST)
                    DepthCompareOp.ALWAYS -> {
                        gl.enable(DEPTH_TEST)
                        gl.depthFunc(ALWAYS)
                    }
                    DepthCompareOp.LESS -> {
                        gl.enable(DEPTH_TEST)
                        gl.depthFunc(LESS)
                    }
                    DepthCompareOp.LESS_EQUAL -> {
                        gl.enable(DEPTH_TEST)
                        gl.depthFunc(LEQUAL)
                    }
                    DepthCompareOp.GREATER -> {
                        gl.enable(DEPTH_TEST)
                        gl.depthFunc(GREATER)
                    }
                    DepthCompareOp.GREATER_EQUAL -> {
                        gl.enable(DEPTH_TEST)
                        gl.depthFunc(GEQUAL)
                    }
                }
            }
        }
    }
}