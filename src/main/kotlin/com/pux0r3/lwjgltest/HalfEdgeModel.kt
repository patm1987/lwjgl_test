package com.pux0r3.lwjgltest

import org.joml.Vector3f

fun halfEdgeModel(cb: HalfEdgeModel.Builder.() -> Unit): HalfEdgeModel {
    val builder = HalfEdgeModel.Builder()
    builder.cb()
    return builder.build()
}

class HalfEdgeModel(val edges: Array<HalfEdge>, val vertices: Array<Vertex>, val faces: Array<Face>) {

    data class HalfEdge(val vertexIndex: Int, val nextEdgeIndex: Int, val oppositeEdgeIndex: Int, val faceIndex: Int)

    data class Vertex(val position: Vector3f, val normal: Vector3f, val edgeIndex: Int)

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