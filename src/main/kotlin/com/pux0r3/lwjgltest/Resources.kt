package com.pux0r3.lwjgltest

import java.io.BufferedReader
import java.io.InputStreamReader

object Resources {
    fun loadAsset(path: String): String {
        val vertexStream = this.javaClass.getResourceAsStream(path)
        val vertexReader = BufferedReader(InputStreamReader(vertexStream))
        var body: String? = null
        vertexReader.use {
            body = it.readLines().joinToString("\n")
        }
        return body ?: ""
    }
}