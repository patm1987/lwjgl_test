package com.pux0r3.lwjgltest

import org.joml.Vector3f
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL15.*
import org.lwjgl.opengl.GL20.glVertexAttribPointer
import org.lwjgl.opengl.GL32.GL_TRIANGLES_ADJACENCY
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.NativeResource

fun halfEdgeModel(cb: HalfEdgeModel.Builder.() -> Unit): HalfEdgeModel {
    val builder = HalfEdgeModel.Builder()
    builder.cb()
    return builder.build()
}

class HalfEdgeModel(val edges: Array<HalfEdge>, val vertices: Array<Vertex>, val faces: Array<Face>) : NativeResource {

    val vertexBufferObject: Int = glGenBuffers()

    // TODO: one or the other. I don't need indices and adjacencies!
    val indexBufferObject: Int = glGenBuffers()
    val adjacencyBufferObject: Int = glGenBuffers()

    val transform = Transform()

    /**
     * This object allows a shader (or any caller) to render this model. It is made private so that you MUST invoke [use]
     * to bind the proper vertex attributes
     */
    private val activeModel = ActiveModel()

    init {
        stackPush().use {
            // write each edge into an index buffer
            val indexBuffer = it.mallocShort(edges.size)
            edges.forEach { halfEdge ->
                indexBuffer.put(halfEdge.vertexIndex.toShort())
            }
            indexBuffer.flip()

            // build an adjacency triangle list
            val adjacencyIndexBuffer = it.mallocShort(edges.size * 2)
            edges.forEach { halfEdge ->
                adjacencyIndexBuffer.put(halfEdge.vertexIndex.toShort())
                if (halfEdge.oppositeEdgeIndex != INVALID_EDGE_INDEX) {
                    val oppositeEdge = oppositeEdge(halfEdge)
                    assert(oppositeEdge(oppositeEdge) == halfEdge)
                    val outlier = nextEdge(nextEdge(oppositeEdge))
                    assert(!vertexInFace(outlier.vertexIndex, halfEdge.faceIndex))
                    adjacencyIndexBuffer.put(outlier.vertexIndex.toShort())
                } else {
                    adjacencyIndexBuffer.put(halfEdge.vertexIndex.toShort())
                }
            }
            adjacencyIndexBuffer.flip()

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
            useAdjacency {
                // TODO: I'm double binding GL_ARRAY_BUFFER, fix this!
                glBufferData(GL_ELEMENT_ARRAY_BUFFER, adjacencyIndexBuffer, GL_STATIC_DRAW)
            }
        }
    }

    fun oppositeEdge(edge: HalfEdge): HalfEdge {
        return edges[edge.oppositeEdgeIndex]
    }

    fun nextEdge(edge: HalfEdge): HalfEdge {
        return edges[edge.nextEdgeIndex]
    }

    fun vertexInFace(vertexIndex: Int, faceIndex: Int): Boolean {
        val startEdge = faces[faceIndex].halfEdgeIndex
        var currentEdge = startEdge
        do {
            if (edges[currentEdge].vertexIndex == vertexIndex) {
                return true
            }
            currentEdge = edges[currentEdge].nextEdgeIndex
        } while (currentEdge != startEdge)
        return false
    }

    fun use(callback: HalfEdgeModel.ActiveModel.() -> Unit) {
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexBufferObject)
        activeModel.callback()
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ARRAY_BUFFER, 0)
    }

    fun useAdjacency(callback: HalfEdgeModel.ActiveModel.() -> Unit) {
        glBindBuffer(GL_ARRAY_BUFFER, vertexBufferObject)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, adjacencyBufferObject)
        activeModel.callback()
        glBindBuffer(GL_ARRAY_BUFFER, 0)
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0)
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
            glVertexAttribPointer(positionAttributeLocation, 3, GL_FLOAT, false, Vertex.VERTEX_SIZE, 0)
        }

        fun loadNormals(normalAttributeLocation: Int) {
            glVertexAttribPointer(normalAttributeLocation, 3, GL_FLOAT, true, Vertex.VERTEX_SIZE, Vertex.VECTOR_3_SIZE.toLong())
        }

        fun drawElements() {
            glDrawElements(GL_TRIANGLES, edges.size, GL_UNSIGNED_SHORT, 0)
        }

        fun drawElementsAdjacency() {
            glDrawElements(GL_TRIANGLES_ADJACENCY, edges.size * 2, GL_UNSIGNED_SHORT, 0)
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

                // the index of the first edge
                val startIndex = edges.size

                // build the edges
                val e0 = EdgeBuilder(faceBuilder.v0, index, startIndex + 1)
                val e1 = EdgeBuilder(faceBuilder.v1, index, startIndex + 2)
                val e2 = EdgeBuilder(faceBuilder.v2, index, startIndex)

                // cache the edges in the edge list
                edges.add(e0)
                edges.add(e1)
                edges.add(e2)

                // add this edge to the vertex list
                vertices[faceBuilder.v0].edges.add(startIndex)
                vertices[faceBuilder.v1].edges.add(startIndex + 1)
                vertices[faceBuilder.v2].edges.add(startIndex + 2)

                // create the face
                Face(startIndex)
            }.toTypedArray()

            // generate edges
            val edgeArray: Array<HalfEdge> = edges.mapIndexed { index, edgeBuilder ->
                // the edge opposite us starts at the vertex our next edge starts at and ends at our vertex
                val oppositeStartVertexIndex = edges[edgeBuilder.nextEdgeIndex].vertexIndex
                val oppositeStartVertex = vertices[oppositeStartVertexIndex]
                val oppositeEdgeIndex = oppositeStartVertex.edges.firstOrNull { edgeIndex ->
                    val testEdge = edges[edgeIndex]
                    val testNextEdge = edges[testEdge.nextEdgeIndex]
                    testNextEdge.vertexIndex == edgeBuilder.vertexIndex
                } ?: INVALID_EDGE_INDEX

                assert(index != oppositeEdgeIndex)
                HalfEdge(edgeBuilder.vertexIndex, edgeBuilder.nextEdgeIndex, oppositeEdgeIndex, edgeBuilder.faceIndex)
            }.toTypedArray()

            // vertices should be ready for generation
            val vertexArray: Array<Vertex> = vertices.map { vertexBuilder ->
                assert(!vertexBuilder.edges.isEmpty())
                Vertex(vertexBuilder.position, vertexBuilder.normal, vertexBuilder.edges.first())
            }.toTypedArray()

            // make sure all the edges belong to this triangle and their opposites don't
            assert(faceArray.all { face ->
                val startEdgeIndex = face.halfEdgeIndex
                var edge = edgeArray[startEdgeIndex]
                while(edge.nextEdgeIndex != startEdgeIndex) {
                    if (faceArray[edge.faceIndex] != face) {
                        return@all false
                    }
                    if (edge.oppositeEdgeIndex != INVALID_EDGE_INDEX && faceArray[edgeArray[edge.oppositeEdgeIndex].faceIndex] == face) {
                        return@all false
                    }
                    edge = edgeArray[edge.nextEdgeIndex]
                }
                return@all true
            })

            return HalfEdgeModel(edgeArray, vertexArray, faceArray)
        }
    }

    class VertexBuilder {
        var position: Vector3f = Vector3f()
        var normal: Vector3f = Vector3f()
        val edges = mutableListOf<Int>()
    }

    class FaceBuilder(val v0: Int, val v1: Int, val v2: Int)

    /**
     * @param vertexIndex the index of the vertex that starts this edge
     * @param faceIndex the face that this edge belongs to
     * @param nextEdgeIndex the next edge in the list
     */
    class EdgeBuilder(val vertexIndex: Int, val faceIndex: Int, val nextEdgeIndex: Int)

    companion object {
        const val INVALID_EDGE_INDEX = -1
    }
}