package com.pux0r3.lwjgltest

import mu.KLogging
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.nio.IntBuffer

/**
 * Created by pux19 on 5/20/2017.
 */
class Game(val width: Int, val height: Int) {
    companion object : KLogging()

    private var window: Long = NULL
    private var shader: ShaderProgram? = null
    private var model: SimpleModel? = null

    fun run() {
        logger.info { "Starting Game with ${Version.getVersion()}!" }

        init()
        loop()

        glfwFreeCallbacks(window)
        glfwDestroyWindow(window)

        glfwTerminate()
        glfwSetErrorCallback(null).free()
    }

    fun init() {
        GLFWErrorCallback.createPrint(System.err).set()

        if (!glfwInit()) {
            logger.error("Unable to initialize GLFW!")
        }

        // create an invisible resizable window
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE)

        window = glfwCreateWindow(width, height, "Hello World", NULL, NULL)
        if (window == NULL) {
            logger.error { "Unable to create GLFW window" }
        }

        glfwSetKeyCallback(window, { window, key, scancode, action, mods ->
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                glfwSetWindowShouldClose(window, true)
            }
        })

        stackPush().use {
            val pWidth: IntBuffer = it.mallocInt(1)
            val pHeight: IntBuffer = it.mallocInt(1)

            // get the window size
            glfwGetWindowSize(window, pWidth, pHeight)

            // get the resolution of the primary monitor
            val vidMode: GLFWVidMode = glfwGetVideoMode(glfwGetPrimaryMonitor())

            // center the window
            glfwSetWindowPos(window, (vidMode.width() - pWidth.get(0)) / 2, (vidMode.height() - pHeight.get(0)) / 2)
        }

        // activate the gl context
        glfwMakeContextCurrent(window)

        // enable vsync
        glfwSwapInterval(1)

        // show the window
        glfwShowWindow(window)
    }

    fun loop() {
        // GL can do stuff after this line
        GL.createCapabilities()

        createShaders()
        createModels()

        glClearColor(1.0f, 0.0f, 0.0f, 0.0f)

        while (!glfwWindowShouldClose(window)) {
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)
            glfwSwapBuffers(window)
            glfwPollEvents()
        }

        // cleanup
        freeModels()
        freeShaders()
    }

    private fun createShaders() {
        shader = ShaderProgram(
                """
#version 330

layout (location=0) in vec3 position;

void main() {
    gl_Position = vec4(position, 1.0);
}
""",
                """
#version 330

out vec4 fragColor;

void main() {
    fragColor = vec4(0.0, 0.5, 0.5, 1.0);
}
"""
        )
    }

    private fun freeShaders() {
        shader?.free()
        shader = null
    }

    private fun createModels() {
        model = SimpleModel(
                arrayOf(Vertex(-1f, -1f, 0f), Vertex(0f, 1f, 0f), Vertex(1f, -1f, 0f)),
                arrayOf(0, 1, 2),
                shader!!)
    }

    private fun freeModels() {
        model?.free()
        model = null
    }
}