package com.pux0r3.lwjgltest

import java.nio.FloatBuffer

/**
 * Created by pux19 on 5/22/2017.
 */
class Vertex(val X: Float, val Y: Float, val Z: Float) {
    companion object {
        val ELEMENT_COUNT = 3 // number of floats per vertex
        val ELEMENT_SIZE = 4 // sizeof a float
    }

    fun put(buffer: FloatBuffer) {
        buffer.put(X).put(Y).put(Z)
    }
}