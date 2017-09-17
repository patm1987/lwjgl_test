package com.pux0r3.lwjgltest

import java.io.BufferedReader
import java.io.InputStreamReader

object Resources {
    fun loadAssetAsString(path: String): String {
        var body: String? = null
        loadAssetAsReader(path).use {
            body = it.readLines().joinToString("\n")
        }
        return body ?: ""
    }

    fun loadAssetAsReader(path: String): BufferedReader {
        val inputStream = this.javaClass.getResourceAsStream(path)
        return BufferedReader(InputStreamReader(inputStream))
    }
}