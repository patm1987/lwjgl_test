package com.pux0r3.lwjgltest

object ObjImporter {
    fun importFile(filename: String, shaderProgram: ShaderProgram): SimpleModel {
        val vertices = mutableListOf<Vertex>()
        val indices = mutableListOf<Short>()

        Resources.loadAssetAsReader(filename).use { objFile ->
            objFile.lines().forEach { line ->
                val tokens = line.split(' ')
                when (tokens[0]) {
                    "v" -> {
                        vertices.add(Vertex(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat()))
                    }
                    "vt" -> println("Found texture coordinate: $line")
                    "vn" -> println("Found list of vertex normals: $line")
                    "vp" -> println("Found a parameter space vertex: $line")
                    "f" -> {
                        val faceIndices = listOf(ObjFace.parse(tokens[1]), ObjFace.parse(tokens[2]), ObjFace.parse(tokens[3]))
                        indices.addAll(faceIndices.map { it.vertexIndex })
                    }
                    "#" -> println("Found a comment: $line")
                }
            }
        }

        return SimpleModel(vertices.toTypedArray(), indices.toTypedArray(), shaderProgram)
    }

    data class ObjFace(val vertexIndex: Short, val textureIndex: Short?, val normalIndex: Short?) {
        companion object {
            fun parse(data: String): ObjFace {
                val faceParts = data.split('/')
                val vertex = faceParts[0].toShort() - 1
                val texture = (if (faceParts.size > 1) faceParts[1].toShortOrNull() else null)?.let { it - 1 }
                val normal = (if (faceParts.size > 2) faceParts[2].toShortOrNull() else null)?.let { it - 1 }
                return ObjFace(vertex.toShort(), texture?.toShort(), normal?.toShort())
            }
        }
    }
}
