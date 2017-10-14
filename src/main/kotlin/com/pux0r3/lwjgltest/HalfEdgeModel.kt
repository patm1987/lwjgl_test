package com.pux0r3.lwjgltest

import org.joml.Vector3f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.NativeResource

fun halfEdgeModel(cb: HalfEdgeModel.Builder.() -> Unit): HalfEdgeModel {
    val builder = HalfEdgeModel.Builder()
    builder.cb()
    return builder.build()
}

class HalfEdgeModel(val edges: Array<HalfEdge>, val vertices: Array<Vertex>, val faces: Array<Face>): NativeResource {

    val vertexBufferObject: Int = glGenBuffers()
    val indexBufferObject: Int = glGenBuffers()
    // TODO: I'll probably want to the faces in the GPU at some point, I just don't have a reason to yet

    val transform = Transform()

    /**
     * This object allows a shader (or any caller) to render this model. It is made private so that you MUST invoke [use]
     * to bind the proper vertex attributes
     */
    private val activeModel = ActiveModel()

    init {
        stackPush().use {
            // write each edge into an index buffer
            // TODO: use GL_TRIANGLES_ADJACENCY instead for adjacency info
            val indexBuffer = it.mallocShort(edges.size)
            edges.forEach { halfEdge ->
                indexBuffer.put(halfEdge.vertexIndex.toShort())
            }
            indexBuffer.flip()

            // write each triangle into an attribute buffer
            val vertexBuffer = it.malloc(Vertex.VERTEX_SIZE * vertices.size)
            vertices.forEachIndexed { index, vertex ->
                val startIndex = index * Vertex.VERTEX_SIZE
                vertex.position.get(startIndex, vertexBuffer)
                vertex.normal.get(startIndex + Vertex.VECTOR_3_SIZE, vertexBuffer)
                vertexBuffer.putInt(startIndex + 2 * Vertex.VECTOR_3_SIZE, vertex.edgeIndex)
            }

            use {
                glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW)
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, indexBuffer, GL_STATIC_DRAW)
            }
        }
    }

    fun use(callback: HalfEdgeModel.ActiveModel.() -> Unit) {
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferObject)
        activeModel.callback()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    override fun free() {
        glDeleteBuffers(vertexBufferObject)
        glDeleteBuffers(indexBufferObject)
    }

    /**
     * Class to aid in rendering a model. Even though the model has some number of attributes, a shader might not want
     * to use all of them. When you call [use], you are operating as a function in this class to guarantee that the
     * buffers have been properly bound
     */
    inner class ActiveModel {
        fun loadPositions(positionAttributeLocation: Int) {
            GL20.glVertexAttribPointer(positionAttributeLocation, 3, GL11.GL_FLOAT, false, Vertex.VERTEX_SIZE, 0)
        }

        fun loadNormals(normalAttributeLocation: Int) {
            GL20.glVertexAttribPointer(normalAttributeLocation, 3, GL11.GL_FLOAT, true, Vertex.VERTEX_SIZE, Vertex.VECTOR_3_SIZE.toLong())
        }

        fun drawElements() {
            GL11.glDrawElements(GL11.GL_TRIANGLES, edges.size, GL11.GL_UNSIGNED_SHORT, 0)
        }
    }

    data class HalfEdge(val vertexIndex: Int, val nextEdgeIndex: Int, val oppositeEdgeIndex: Int, val faceIndex: Int)

    data class Vertex(val position: Vector3f, val normal: Vector3f, val edgeIndex: Int) {
        companion object {
            const val FLOAT_SIZE = 4
            const val INT_SIZE = 4
            const val VECTOR_3_SIZE = 3 * FLOAT_SIZE
            const val VERTEX_SIZE = 2 * VECTOR_3_SIZE + INT_SIZE
        }
    }

    data class Face(val halfEdgeIndex: Int)

    class Builder {
        val vertices = mutableListOf<VertexBuilder>()
        val faces = mutableListOf<FaceBuilder>()

        fun vertex(cb: VertexBuilder.() -> Unit): Builder {
            val builder = VertexBuilder()
            builder.cb()
            vertices.add(builder)
            return this
        }

        fun face(i0: Int, i1: Int, i2: Int): Builder {
            val builder = FaceBuilder(i0, i1, i2)
            faces.add(builder)
            return this
        }

        fun build(): HalfEdgeModel {
            val edges = mutableListOf<EdgeBuilder>()

            // create the face array, also generating the edges
            val faceArray: Array<Face> = faces.mapIndexed { index, faceBuilder ->
                val startIndex = edges.size
                val e0 = EdgeBuilder(faceBuilder.v0, index, startIndex + 1)
                val e1 = EdgeBuilder(faceBuilder.v1, index, startIndex + 2)
                val e2 = EdgeBuilder(faceBuilder.v2, index, startIndex)
                edges.add(e0)
                edges.add(e1)
                edges.add(e2)
                Face(startIndex)
            }.toTypedArray()

            // edges need to be hooked up to vertices
            edges.forEachIndexed { index, edgeBuilder ->
                if (vertices[edgeBuilder.vertexIndex].edge == null) {
                    // if the position doesn't have an edge, set it
                    vertices[edgeBuilder.vertexIndex].edge = index
                } else {
                    // otherwise set that position edge's opposite edge to this one and vice versa
                    edgeBuilder.oppositeEdgeIndex = vertices[edgeBuilder.vertexIndex].edge!!
                    edges[vertices[edgeBuilder.vertexIndex].edge!!].oppositeEdgeIndex = index
                }
            }

            // generate edges
            val edgeArray: Array<HalfEdge> = edges.map { edgeBuilder -> HalfEdge(edgeBuilder.vertexIndex, edgeBuilder.nextEdgeIndex, edgeBuilder.oppositeEdgeIndex, edgeBuilder.faceIndex) }.toTypedArray()

            // vertices should be ready for generation
            val vertexArray: Array<Vertex> = vertices.map { vertexBuilder -> Vertex(vertexBuilder.position, vertexBuilder.normal, vertexBuilder.edge!!) }.toTypedArray()

            return HalfEdgeModel(edgeArray, vertexArray, faceArray)
        }
    }

    class VertexBuilder {
        var position: Vector3f = Vector3f()
        var normal: Vector3f = Vector3f()
        var edge: Int? = null
    }

    class FaceBuilder(val v0: Int, val v1: Int, val v2: Int)

    class EdgeBuilder(val vertexIndex: Int, val faceIndex: Int, val nextEdgeIndex: Int) {
        var oppositeEdgeIndex: Int = INVALID_EDGE_INDEX // the opposite edge is optional
    }

    companion object {
        const val INVALID_EDGE_INDEX = -1
        const val INVALID_VERTEX_INDEX = -1
    }
}