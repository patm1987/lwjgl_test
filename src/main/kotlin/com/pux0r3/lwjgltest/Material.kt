package com.pux0r3.lwjgltest

import mu.KLogging
import org.joml.Vector3f

class Material private constructor(
        val name: String,
        val ambient: Vector3f,
        val diffuse: Vector3f,
        val specular: Vector3f,
        val specularExponent: Float) {

    companion object : KLogging() {
        fun loadFromFile(filename: String): Material {
            val builder = Builder()
            Resources.loadAssetAsReader(filename).use { materialFile ->
                materialFile.lines().forEach { line ->
                    val tokens = line.split(' ')
                    when (tokens[0]) {
                        "Ka" -> {
                            val color = Vector3f()
                            parseColor(tokens, 1, color)
                            builder.setAmbient(color)
                        }
                        "Kd" -> {
                            val color = Vector3f()
                            parseColor(tokens, 1, color)
                            builder.setDiffuse(color)
                        }
                        "Ks" -> {
                            val color = Vector3f()
                            parseColor(tokens, 1, color)
                            builder.setSpecular(color)
                        }
                        "Ns" -> {
                            builder.setSpecularExponent(tokens[1].toFloat())
                        }
                        "newmtl" -> {
                            val nameParts = tokens.slice(IntRange(1, tokens.size - 1))
                            builder.setName(nameParts.joinToString(" "))
                        }

                    }
                }
            }
            return builder.build()
        }

        fun parseColor(tokens: List<String>, startIndex: Int, outColor: Vector3f) {
            outColor.x = tokens[startIndex].toFloat()
            outColor.y = tokens[startIndex + 1].toFloat()
            outColor.z = tokens[startIndex + 2].toFloat()
        }
    }

    class Builder {
        private var name: String = ""
        private val ambient: Vector3f = Vector3f()
        private val diffuse: Vector3f = Vector3f()
        private val specular: Vector3f = Vector3f()
        private var specularExponent: Float = 0f

        fun setName(name: String): Builder {
            this.name = name
            return this
        }

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

        fun build(): Material {
            return Material(name, ambient, diffuse, specular, specularExponent)
        }
    }
}
