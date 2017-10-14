package com.pux0r3.lwjgltest

import org.joml.Vector3f

object ObjImporter {
    fun importFile(filename: String): HalfEdgeModel {
        val vertices = mutableListOf<Vector3f>()
        val normals = mutableListOf<Vector3f>()
        val indices = mutableListOf<Int>()

        Resources.loadAssetAsReader(filename).use { objFile ->
            objFile.lines().forEach { line ->
                val tokens = line.split(' ')
                when (tokens[0]) {
                    "v" -> {
                        vertices.add(Vector3f(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat()))
                    }
                    "vt" -> println("Found texture coordinate: $line")
                    "vn" -> {
                        normals.add(Vector3f(tokens[1].toFloat(), tokens[2].toFloat(), tokens[3].toFloat()))
                    }
                    "vp" -> println("Found a parameter space vertex: $line")
                    "f" -> {
                        val faceIndices = listOf(ObjFace.parse(tokens[1]), ObjFace.parse(tokens[2]), ObjFace.parse(tokens[3]))
                        indices.addAll(faceIndices.map { it.vertexIndex })
                    }
                    "#" -> println("Found a comment: $line")
                }
            }
        }

        return halfEdgeModel {
            vertices.zip(normals).forEach { (pos, norm) ->
                vertex {
                    position = pos
                    normal = norm
                }
            }

            // I really wish I had a normal for loop...
            var faceIndex = 0
            while (faceIndex + 2 < indices.size) {
                face(indices[faceIndex], indices[faceIndex + 1], indices[faceIndex + 2])
                faceIndex += 3
            }
        }
    }

    data class ObjFace(val vertexIndex: Int, val textureIndex: Int?, val normalIndex: Int?) {
        companion object {
            fun parse(data: String): ObjFace {
                val faceParts = data.split('/')
                val vertex = faceParts[0].toShort() - 1
                val texture = (if (faceParts.size > 1) faceParts[1].toShortOrNull() else null)?.let { it - 1 }
                val normal = (if (faceParts.size > 2) faceParts[2].toShortOrNull() else null)?.let { it - 1 }
                return ObjFace(vertex, texture, normal)
            }
        }
    }
}
