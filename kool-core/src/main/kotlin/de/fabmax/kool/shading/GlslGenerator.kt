package de.fabmax.kool.shading

import de.fabmax.kool.RenderContext
import de.fabmax.kool.glslVersion
import de.fabmax.kool.scene.animation.Armature

/**
 * @author fabmax
 */
open class GlslGenerator {
    companion object {
        const val U_MVP_MATRIX = "uMvpMatrix"
        const val U_MODEL_MATRIX = "uModelMatrix"
        const val U_VIEW_MATRIX = "uViewMatrix"
        const val U_LIGHT_DIRECTION = "uLightDirection"
        const val U_LIGHT_COLOR = "uLightColor"
        const val U_SHININESS = "uShininess"
        const val U_SPECULAR_INTENSITY = "uSpecularIntensity"
        const val U_CAMERA_POSITION = "uCameraPosition"
        const val U_FOG_COLOR = "uFogColor"
        const val U_FOG_RANGE = "uFogRange"
        const val U_TEXTURE_0 = "uTexture0"
        const val U_STATIC_COLOR = "uStaticColor"
        const val U_ALPHA = "uAlpha"
        const val U_SATURATION = "uSaturation"
        const val U_BONES = "uBones"

        const val V_TEX_COORD = "vTexCoord"
        const val V_EYE_DIRECTION = "vEyeDirection_cameraspace"
        const val V_LIGHT_DIRECTION = "vLightDirection_cameraspace"
        const val V_NORMAL = "vNormal_cameraspace"
        const val V_COLOR = "vFragmentColor"
        const val V_DIFFUSE_LIGHT_COLOR = "vDiffuseLightColor"
        const val V_SPECULAR_LIGHT_COLOR = "vSpecularLightColor"
        const val V_POSITION_WORLDSPACE = "vPositionWorldspace"

        const val L_TEX_COLOR = "texColor"
        const val L_VERTEX_COLOR = "vertColor"
        const val L_STATIC_COLOR = "staticColor"
    }

    interface GlslInjector {
        fun vsHeader(text: StringBuilder) { }
        fun vsStart(shaderProps: ShaderProps, text: StringBuilder) { }
        fun vsAfterInput(shaderProps: ShaderProps, text: StringBuilder) { }
        fun vsBeforeProj(shaderProps: ShaderProps, text: StringBuilder) { }
        fun vsAfterProj(shaderProps: ShaderProps, text: StringBuilder) { }
        fun vsEnd(shaderProps: ShaderProps, text: StringBuilder) { }

        fun fsHeader(text: StringBuilder) { }
        fun fsStart(shaderProps: ShaderProps, text: StringBuilder) { }
        fun fsAfterInput(shaderProps: ShaderProps, text: StringBuilder) { }
        fun fsBeforeSampling(shaderProps: ShaderProps, text: StringBuilder) { }
        fun fsAfterSampling(shaderProps: ShaderProps, text: StringBuilder) { }
        fun fsEnd(shaderProps: ShaderProps, text: StringBuilder) { }
    }

    val injectors = mutableListOf<GlslInjector>()

    val uniformMvpMatrix: UniformMatrix4 = UniformMatrix4(U_MVP_MATRIX)
    val uniformModelMatrix: UniformMatrix4 = UniformMatrix4(U_MODEL_MATRIX)
    val uniformViewMatrix: UniformMatrix4 = UniformMatrix4(U_VIEW_MATRIX)
    val uniformLightColor: Uniform3f = Uniform3f(U_LIGHT_COLOR)
    val uniformLightDirection: Uniform3f = Uniform3f(U_LIGHT_DIRECTION)
    val uniformCameraPosition: Uniform3f = Uniform3f(U_CAMERA_POSITION)
    val uniformShininess: Uniform1f = Uniform1f(U_SHININESS)
    val uniformSpecularIntensity: Uniform1f = Uniform1f(U_SPECULAR_INTENSITY)
    val uniformStaticColor: Uniform4f = Uniform4f(U_STATIC_COLOR)
    val uniformTexture: UniformTexture2D = UniformTexture2D(U_TEXTURE_0)
    val uniformAlpha: Uniform1f = Uniform1f(U_ALPHA)
    val uniformSaturation: Uniform1f = Uniform1f(U_SATURATION)
    val uniformFogRange: Uniform1f = Uniform1f(U_FOG_RANGE)
    val uniformFogColor: Uniform4f = Uniform4f(U_FOG_COLOR)

    val customUnitforms: MutableMap<String, Uniform<*>> = mutableMapOf()

    private val vsIn = glslVersion.dialect.vsIn
    private val vsOut = glslVersion.dialect.vsOut
    private val fsIn = glslVersion.dialect.fsIn
    private val fsOut = glslVersion.dialect.fragColorHead
    private val fsOutBody = glslVersion.dialect.fragColorBody
    private val texSampler = glslVersion.dialect.texSampler

    fun addCustomUniform(uniform: Uniform<*>) {
        customUnitforms[uniform.name] = uniform
    }

    /**
     * Called when shader is loaded / compiled
     */
    fun onLoad(shader: BasicShader, ctx: RenderContext) {
        shader.enableAttribute(Attribute.POSITIONS, ctx)
        shader.enableAttribute(Attribute.NORMALS, ctx)
        shader.enableAttribute(Attribute.TEXTURE_COORDS, ctx)
        shader.enableAttribute(Attribute.COLORS, ctx)

        if (shader.props.isSkinned) {
            shader.enableAttribute(Armature.BONE_INDICES, ctx)
            shader.enableAttribute(Armature.BONE_WEIGHTS, ctx)
        }

        shader.setUniformLocation(uniformMvpMatrix, ctx)
        shader.setUniformLocation(uniformModelMatrix, ctx)
        shader.setUniformLocation(uniformViewMatrix, ctx)
        shader.setUniformLocation(uniformLightDirection, ctx)
        shader.setUniformLocation(uniformLightColor, ctx)
        shader.setUniformLocation(uniformShininess, ctx)
        shader.setUniformLocation(uniformSpecularIntensity, ctx)
        shader.setUniformLocation(uniformCameraPosition, ctx)
        shader.setUniformLocation(uniformFogColor, ctx)
        shader.setUniformLocation(uniformFogRange, ctx)
        shader.setUniformLocation(uniformTexture, ctx)
        shader.setUniformLocation(uniformStaticColor, ctx)
        shader.setUniformLocation(uniformAlpha, ctx)
        shader.setUniformLocation(uniformSaturation, ctx)

        for (uniform in customUnitforms.values) {
            shader.setUniformLocation(uniform, ctx)
        }
    }

    fun generate(shaderProps: ShaderProps): Shader.Source {
        uniformMvpMatrix.location = null
        uniformModelMatrix.location = null
        uniformViewMatrix.location = null
        uniformLightColor.location = null
        uniformLightDirection.location = null
        uniformCameraPosition.location = null
        uniformShininess.location = null
        uniformSpecularIntensity.location = null
        uniformStaticColor.location = null
        uniformTexture.location = null
        uniformAlpha.location = null
        uniformSaturation.location = null
        uniformFogRange.location = null
        uniformFogColor.location = null

        return Shader.Source(generateVertShader(shaderProps), generateFragShader(shaderProps))
    }

    private fun generateVertShader(shaderProps: ShaderProps): String {
        val text = StringBuilder("${glslVersion.versionStr}\n")

        injectors.forEach { it.vsHeader(text) }

        injectors.forEach { it.vsStart(shaderProps, text) }
        generateVertInputCode(shaderProps, text)
        injectors.forEach { it.vsAfterInput(shaderProps, text) }
        generateVertBodyCode(shaderProps, text)
        injectors.forEach { it.vsEnd(shaderProps, text) }

        return text.toString()
    }

    private fun generateFragShader(shaderProps: ShaderProps): String {
        val text = StringBuilder("${glslVersion.versionStr}\n")

        injectors.forEach { it.fsHeader(text) }

        injectors.forEach { it.fsStart(shaderProps, text) }
        generateFragInputCode(shaderProps, text)
        injectors.forEach { it.fsAfterInput(shaderProps, text) }
        generateFragBodyCode(shaderProps, text)
        injectors.forEach { it.fsEnd(shaderProps, text) }

        return text.toString()
    }

    private fun generateVertInputCode(shaderProps: ShaderProps, text: StringBuilder) {
        // MVP matrices and vertex position attribute are always needed
        text.append("$vsIn vec3 ${Attribute.POSITIONS.name};\n")
        text.append("uniform mat4 $U_MVP_MATRIX;\n")
        text.append("uniform mat4 $U_MODEL_MATRIX;\n")
        text.append("uniform mat4 $U_VIEW_MATRIX;\n")

        // add light dependent uniforms and attributes
        if (shaderProps.lightModel != LightModel.NO_LIGHTING) {
            text.append("$vsIn vec3 ${Attribute.NORMALS.name};\n")
            text.append("uniform vec3 $U_LIGHT_DIRECTION;\n")

            if (shaderProps.lightModel == LightModel.PHONG_LIGHTING) {
                // Phong model specific stuff
                text.append("$vsOut vec3 $V_EYE_DIRECTION;\n")
                text.append("$vsOut vec3 $V_LIGHT_DIRECTION;\n")
                text.append("$vsOut vec3 $V_NORMAL;\n")

            } else {
                // Gouraud model specific stuff
                text.append("uniform vec3 $U_LIGHT_COLOR;\n")
                text.append("uniform float $U_SHININESS;\n")
                text.append("uniform float $U_SPECULAR_INTENSITY;\n")
                text.append("$vsOut vec3 $V_DIFFUSE_LIGHT_COLOR;\n")
                text.append("$vsOut vec3 $V_SPECULAR_LIGHT_COLOR;\n")
            }
        }

        // add color dependent attributes
        if (shaderProps.isTextureColor) {
            // texture color
            text.append("$vsIn vec2 ${Attribute.TEXTURE_COORDS.name};\n")
            text.append("$vsOut vec2 $V_TEX_COORD;\n")

        }
        if (shaderProps.isVertexColor) {
            // vertex color
            text.append("$vsIn vec4 ${Attribute.COLORS.name};\n")
            text.append("$vsOut vec4 $V_COLOR;\n")
        }

        if (shaderProps.isSkinned) {
            text.append("$vsIn ivec4 ${Armature.BONE_INDICES.name};\n")
            text.append("$vsIn vec4 ${Armature.BONE_WEIGHTS.name};\n")
            text.append("uniform mat4 $U_BONES[${Armature.MAX_BONES}];\n")
        }

        // if fog is enabled, fragment shader needs to know the world position
        if (shaderProps.fogModel != FogModel.FOG_OFF) {
            text.append("$vsIn vec3 $V_POSITION_WORLDSPACE;\n")
        }

        for (uniform in customUnitforms.values) {
            text.append("uniform ${uniform.type} ${uniform.name};\n")
        }
    }

    private fun generateVertBodyCode(shaderProps: ShaderProps, text: StringBuilder) {
        text.append("\nvoid main() {\n")

        injectors.forEach { it.vsBeforeProj(shaderProps, text) }

        // output position of the vertex in clip space: MVP * position
        text.append("gl_Position = $U_MVP_MATRIX * vec4(${Attribute.POSITIONS.name}, 1.0);\n")

        injectors.forEach { it.vsAfterProj(shaderProps, text) }

        if (shaderProps.fogModel != FogModel.FOG_OFF) {
            text.append("$V_POSITION_WORLDSPACE = ($U_MODEL_MATRIX * vec4(${Attribute.POSITIONS.name}, 1.0)).xyz;\n")
        }

        if (shaderProps.isTextureColor) {
            // interpolate vertex texture coordinate for usage in fragment shader
            text.append("$V_TEX_COORD = ${Attribute.TEXTURE_COORDS.name};\n")

        }
        if (shaderProps.isVertexColor) {
            // interpolate vertex color for usage in fragment shader
            text.append("$V_COLOR = ${Attribute.COLORS.name};\n")
        }

        if (shaderProps.lightModel == LightModel.PHONG_LIGHTING) {
            // vector from vertex to camera, in camera space. In camera space, the camera is at the origin (0, 0, 0).
            text.append("$V_EYE_DIRECTION = -($U_VIEW_MATRIX * ($U_MODEL_MATRIX * vec4(${Attribute.POSITIONS.name}, 1.0))).xyz;\n")

            // light direction, in camera space. M is left out because light position is already in world space.
            text.append("$V_LIGHT_DIRECTION = ($U_VIEW_MATRIX * vec4($U_LIGHT_DIRECTION, 0.0)).xyz;\n")

            // normal of the the vertex, in camera space
            text.append("$V_NORMAL = ($U_VIEW_MATRIX * ($U_MODEL_MATRIX * vec4(${Attribute.NORMALS.name}, 0.0))).xyz;\n")

        } else if (shaderProps.lightModel == LightModel.GOURAUD_LIGHTING) {
            // vector from vertex to camera, in camera space. In camera space, the camera is at the origin (0, 0, 0).
            text.append("vec3 e = normalize(-($U_VIEW_MATRIX * ($U_MODEL_MATRIX * vec4(${Attribute.POSITIONS.name}, 1.0))).xyz);\n")

            // light direction, in camera space. M is left out because light position is already in world space.
            text.append("vec3 l = normalize(($U_VIEW_MATRIX * vec4($U_LIGHT_DIRECTION, 0.0)).xyz);\n")

            // normal of the the vertex, in camera space
            text.append("vec3 n = normalize(($U_VIEW_MATRIX * ($U_MODEL_MATRIX * vec4(${Attribute.NORMALS.name}, 0.0))).xyz);\n")

            // cosine of angle between surface normal and light direction
            text.append("float cosTheta = clamp(dot(n, l), 0.0, 1.0);\n")
            // direction in which the light is reflected
            text.append("vec3 r = reflect(-l, n);\n")
            // cosine of the angle between the eye vector and the reflect vector
            text.append("float cosAlpha = clamp(dot(e, r), 0.0, 1.0);\n")

            // interpolate light colors for usage in fragment shader
            text.append("$V_DIFFUSE_LIGHT_COLOR = $U_LIGHT_COLOR * cosTheta;\n")
            text.append("$V_SPECULAR_LIGHT_COLOR = $U_LIGHT_COLOR * $U_SPECULAR_INTENSITY * pow(cosAlpha, $U_SHININESS);\n")
        }
        text.append("}\n")
    }

    private fun generateFragInputCode(shaderProps: ShaderProps, text: StringBuilder) {
        text.append("precision highp float;\n")
        text.append("uniform mat4 $U_MODEL_MATRIX;\n")
        text.append("uniform mat4 $U_VIEW_MATRIX;\n")
        if (shaderProps.isAlpha) {
            text.append("uniform float $U_ALPHA;\n")
        }
        if (shaderProps.isSaturation) {
            text.append("uniform float $U_SATURATION;\n")
        }

        // add light dependent uniforms and varyings
        if (shaderProps.lightModel == LightModel.PHONG_LIGHTING) {
            text.append("uniform vec3 $U_LIGHT_COLOR;\n")
            text.append("uniform float $U_SHININESS;\n")
            text.append("uniform float $U_SPECULAR_INTENSITY;\n")
            text.append("$fsIn vec3 $V_EYE_DIRECTION;\n")
            text.append("$fsIn vec3 $V_LIGHT_DIRECTION;\n")
            text.append("$fsIn vec3 $V_NORMAL;\n")
        } else if (shaderProps.lightModel == LightModel.GOURAUD_LIGHTING) {
            text.append("$fsIn vec3 $V_DIFFUSE_LIGHT_COLOR;\n")
            text.append("$fsIn vec3 $V_SPECULAR_LIGHT_COLOR;\n")
        }

        // add color dependent uniforms and varyings
        if (shaderProps.isTextureColor) {
            // texture color
            text.append("uniform sampler2D $U_TEXTURE_0;\n")
            text.append("$fsIn vec2 $V_TEX_COORD;\n")
        }
        if (shaderProps.isVertexColor) {
            // vertex color
            text.append("$fsIn vec4 $V_COLOR;\n")
        }
        if (shaderProps.isStaticColor) {
            // static color
            text.append("uniform vec4 $U_STATIC_COLOR;\n")
        }

        // add fog uniforms
        if (shaderProps.fogModel != FogModel.FOG_OFF) {
            text.append("uniform vec3 $U_CAMERA_POSITION;\n")
            text.append("uniform vec4 $U_FOG_COLOR;\n")
            text.append("uniform float $U_FOG_RANGE;\n")
            text.append("$fsIn vec3 $V_POSITION_WORLDSPACE;\n")
        }

        for (uniform in customUnitforms.values) {
            text.append("uniform ${uniform.type} ${uniform.name};\n")
        }
        text.append(fsOut)
    }

    private fun generateFragBodyCode(shaderProps: ShaderProps, text: StringBuilder) {
        text.append("\nvoid main() {\n")
        text.append("$fsOutBody = vec4(0.0);\n")

        injectors.forEach { it.fsBeforeSampling(shaderProps, text) }

        if (shaderProps.isTextureColor) {
            // get base fragment color from texture
            text.append("vec4 $L_TEX_COLOR = $texSampler($U_TEXTURE_0, $V_TEX_COORD);\n")
            text.append("$fsOutBody = $L_TEX_COLOR;\n")
        }
        if (shaderProps.isVertexColor) {
            // get base fragment color from vertex attribute
            text.append("vec4 $L_VERTEX_COLOR = $V_COLOR;\n")
            text.append("$L_VERTEX_COLOR.rgb *= $L_VERTEX_COLOR.a;\n")
            text.append("$fsOutBody = $L_VERTEX_COLOR;\n")
        }
        if (shaderProps.isStaticColor) {
            // get base fragment color from static color uniform
            text.append("vec4 $L_STATIC_COLOR = $U_STATIC_COLOR;\n")
            text.append("$L_STATIC_COLOR.rgb *= $L_STATIC_COLOR.a;\n")
            text.append("$fsOutBody = $L_STATIC_COLOR;\n")
        }

        injectors.forEach { it.fsAfterSampling(shaderProps, text) }

        if (shaderProps.lightModel != LightModel.NO_LIGHTING) {
            if (shaderProps.lightModel == LightModel.PHONG_LIGHTING) {
                // normalize input vectors
                text.append("vec3 e = normalize($V_EYE_DIRECTION);\n")
                text.append("vec3 l = normalize($V_LIGHT_DIRECTION);\n")
                text.append("vec3 n = normalize($V_NORMAL);\n")

                // cosine of angle between surface normal and light direction
                text.append("float cosTheta = clamp(dot(n, l), 0.0, 1.0);\n")
                // direction in which the light is reflected
                text.append("vec3 r = reflect(-l, n);\n")
                // cosine of the angle between the eye vector and the reflect vector
                text.append("float cosAlpha = clamp(dot(e, r), 0.0, 1.0);\n")

                text.append("vec3 materialAmbientColor = $fsOutBody.rgb * vec3(0.42);\n")
                text.append("vec3 materialDiffuseColor = $fsOutBody.rgb * $U_LIGHT_COLOR * cosTheta;\n")
                text.append("vec3 materialSpecularColor = $U_LIGHT_COLOR * $U_SPECULAR_INTENSITY * pow(cosAlpha, $U_SHININESS) * $fsOutBody.a;\n")

            } else if (shaderProps.lightModel == LightModel.GOURAUD_LIGHTING) {
                text.append("vec3 materialAmbientColor = $fsOutBody.rgb * vec3(0.42);\n")
                text.append("vec3 materialDiffuseColor = $fsOutBody.rgb * $V_DIFFUSE_LIGHT_COLOR;\n")
                text.append("vec3 materialSpecularColor = $V_SPECULAR_LIGHT_COLOR * $fsOutBody.a;\n")
            }

            // compute output color
            text.append("$fsOutBody = vec4(materialAmbientColor + materialDiffuseColor + materialSpecularColor, $fsOutBody.a);\n")

        }

        // add fog code
        if (shaderProps.fogModel != FogModel.FOG_OFF) {
            text.append("float d = 1.0 - clamp(length($U_CAMERA_POSITION - $V_POSITION_WORLDSPACE / $U_FOG_RANGE), 0.0, 1.0);\n")
            text.append("$fsOutBody.rgb = mix($U_FOG_COLOR.rgb, $fsOutBody.rgb, d * d * $U_FOG_COLOR.a);\n")
        }

        if (shaderProps.isAlpha) {
            text.append("$fsOutBody *= $U_ALPHA;\n")
        }
        if (shaderProps.isSaturation) {
            text.append("float avgColor = ($fsOutBody.r + $fsOutBody.g + $fsOutBody.b) * 0.333;\n")
            text.append("$fsOutBody.rgb = mix(vec3(avgColor), $fsOutBody.rgb, $U_SATURATION);\n")
        }

        text.append("}\n")
    }
}
