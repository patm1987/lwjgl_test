package com.pux0r3.lwjgltest

// TODO: I can probably do this without virtual functions, abstract loadUniform into a scenegraph node
// TODO: at the very least, this should return a Matrixf to caller rather than set uniform directly
interface ICamera {
    fun setResolution(width: Int, height: Int)
    fun loadUniform(uniformId: Int)
}