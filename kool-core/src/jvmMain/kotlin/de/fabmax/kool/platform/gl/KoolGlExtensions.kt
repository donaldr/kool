package de.fabmax.kool.platform.gl

import de.fabmax.kool.pipeline.DepthCompareOp
import de.fabmax.kool.pipeline.TexFormat
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL30


val TexFormat.glInternalFormat: Int
    get() = when(this) {
        TexFormat.R -> GL30.GL_R8
        TexFormat.RG -> GL30.GL_RG8
        TexFormat.RGB -> GL30.GL_RGB8
        TexFormat.RGBA -> GL30.GL_RGBA8

        TexFormat.R_F16 -> GL30.GL_R16F
        TexFormat.RG_F16 -> GL30.GL_RG16F
        TexFormat.RGB_F16 -> GL30.GL_RGB16F
        TexFormat.RGBA_F16 -> GL30.GL_RGBA16F

        TexFormat.R_F32 -> GL30.GL_R32F
        TexFormat.RG_F32 -> GL30.GL_RG32F
        TexFormat.RGB_F32 -> GL30.GL_RGB32F
        TexFormat.RGBA_F32 -> GL30.GL_RGBA32F
    }

val TexFormat.glType: Int
    get() = when(this) {
        TexFormat.R -> GL30.GL_UNSIGNED_BYTE
        TexFormat.RG -> GL30.GL_UNSIGNED_BYTE
        TexFormat.RGB -> GL30.GL_UNSIGNED_BYTE
        TexFormat.RGBA -> GL30.GL_UNSIGNED_BYTE

        TexFormat.R_F16 -> GL30.GL_FLOAT
        TexFormat.RG_F16 -> GL30.GL_FLOAT
        TexFormat.RGB_F16 -> GL30.GL_FLOAT
        TexFormat.RGBA_F16 -> GL30.GL_FLOAT

        TexFormat.R_F32 -> GL30.GL_FLOAT
        TexFormat.RG_F32 -> GL30.GL_FLOAT
        TexFormat.RGB_F32 -> GL30.GL_FLOAT
        TexFormat.RGBA_F32 -> GL30.GL_FLOAT
    }

val TexFormat.glFormat: Int
    get() = when(this) {
        TexFormat.R -> GL30.GL_RED
        TexFormat.RG -> GL30.GL_RG
        TexFormat.RGB -> GL30.GL_RGB
        TexFormat.RGBA -> GL30.GL_RGBA

        TexFormat.R_F16 -> GL30.GL_RED
        TexFormat.RG_F16 -> GL30.GL_RG
        TexFormat.RGB_F16 -> GL30.GL_RGB
        TexFormat.RGBA_F16 -> GL30.GL_RGBA

        TexFormat.R_F32 -> GL30.GL_RED
        TexFormat.RG_F32 -> GL30.GL_RG
        TexFormat.RGB_F32 -> GL30.GL_RGB
        TexFormat.RGBA_F32 -> GL30.GL_RGBA
    }

val TexFormat.pxSize: Int
    get() = when(this) {
        TexFormat.R -> 1
        TexFormat.RG -> 2
        TexFormat.RGB -> 3
        TexFormat.RGBA -> 4

        TexFormat.R_F16 -> 2
        TexFormat.RG_F16 -> 4
        TexFormat.RGB_F16 -> 6
        TexFormat.RGBA_F16 -> 8

        TexFormat.R_F32 -> 4
        TexFormat.RG_F32 -> 8
        TexFormat.RGB_F32 -> 12
        TexFormat.RGBA_F32 -> 16
    }

val DepthCompareOp.glOp: Int
    get() = when(this) {
        DepthCompareOp.DISABLED -> 0
        DepthCompareOp.ALWAYS -> GL_ALWAYS
        DepthCompareOp.NEVER -> GL_NEVER
        DepthCompareOp.LESS -> GL_LESS
        DepthCompareOp.LESS_EQUAL -> GL_LEQUAL
        DepthCompareOp.GREATER -> GL_GREATER
        DepthCompareOp.GREATER_EQUAL -> GL_GEQUAL
        DepthCompareOp.EQUAL -> GL_EQUAL
        DepthCompareOp.NOT_EQUAL -> GL_NOTEQUAL
    }