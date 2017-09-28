package com.pux0r3.lwjgltest

import mu.KLogging
import org.joml.Vector3f

@DslMarker
annotation class MaterialTagMarker

/**
 * Use this function to create a Material using a pseudo-DSL
 */
fun material(cb: Material.Builder.() -> Unit): Material {
    val builder = Material.Builder()
    builder.cb()
    return builder.build()
}

class Material private constructor(
        val name: String,
        val ambient: Vector3f,
        val diffuse: Vector3f,
        val specular: Vector3f,
        val specularExponent: Float) {

    companion object : KLogging() {

        fun parseColor(tokens: List<String>, startIndex: Int, outColor: Vector3f) {
            outColor.x = tokens[startIndex].toFloat()
            outColor.y = tokens[startIndex + 1].toFloat()
            outColor.z = tokens[startIndex + 2].toFloat()
        }
    }

    @MaterialTagMarker
    class Builder {
        var name: String = ""
        private val ambient: Vector3f = Vector3f()
        private val diffuse: Vector3f = Vector3f()
        private val specular: Vector3f = Vector3f()
        private var specularExponent: Float = 0f
        private var shader: ShaderProgram? = null
        private var materialFile: String? = null

        fun setAmbient(color: Vector3f): Builder {
            this.ambient.set(color)
            return this
        }

        fun setDiffuse(color: Vector3f): Builder {
            this.diffuse.set(color)
            return this
        }

        fun setSpecular(color: Vector3f): Builder {
            this.specular.set(color)
            return this
        }

        fun setSpecularExponent(exponent: Float): Builder {
            specularExponent = exponent
            return this
        }

        fun src(cb: ()->String) {
            this.materialFile = cb()
        }

        private fun loadFromFile(filename: String) {
            Resources.loadAssetAsReader(filename).use { materialFile ->
                materialFile.lines().forEach { line ->
                    val tokens = line.split(' ')
                    when (tokens[0]) {
                        "Ka" -> {
                            val color = Vector3f()
                            parseColor(tokens, 1, color)
                            setAmbient(color)
                        }
                        "Kd" -> {
                            val color = Vector3f()
                            parseColor(tokens, 1, color)
                            setDiffuse(color)
                        }
                        "Ks" -> {
                            val color = Vector3f()
                            parseColor(tokens, 1, color)
                            setSpecular(color)
                        }
                        "Ns" -> {
                            setSpecularExponent(tokens[1].toFloat())
                        }
                        "newmtl" -> {
                            val nameParts = tokens.slice(IntRange(1, tokens.size - 1))
                            name = nameParts.joinToString(" ")
                        }

                    }
                }
            }
        }

        fun build(): Material {
            materialFile?.let { loadFromFile(it) }

            // throw an error on bad shader, not a typo
            return Material(name, ambient, diffuse, specular, specularExponent)
        }
    }
}
