package com.pux0r3.lwjgltest

import mu.KLogging
import org.joml.Vector3f
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
class Game(private var width: Int, private var height: Int) {
    companion object : KLogging()

    private var window: Long = NULL
    private var shader: ShaderProgram? = null
    private var models = mutableListOf<SimpleModel>()
    private val camera = LookAtPerspectiveCamera(
            Math.toRadians(45.0).toFloat(),
            1f,
            .01f,
            100f,
            Vector3f(0f, .5f, -10f),
            Vector3f())

    private var pendingWidth = 0
    private var pendingHeight = 0

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

        glfwSetWindowSizeCallback(window, { window, width, height ->
            camera.setResolution(width, height)
            pendingWidth = width
            pendingHeight = height
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

        glEnable(GL_DEPTH_TEST)
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        var cameraPositionRadians = 0f
        val cameraDistance = 10f
        val cameraHeight = 10f

        var lastTime = glfwGetTime()
        val cameraPosition = Vector3f(0f, cameraHeight, cameraDistance)
        while (!glfwWindowShouldClose(window)) {
            // game stuff
            val currentTime = glfwGetTime()
            val deltaTime = (currentTime - lastTime).toFloat()
            lastTime = currentTime

            cameraPositionRadians += deltaTime
            cameraPositionRadians %= 2f * Math.PI.toFloat()
            cameraPosition.x = Math.cos(cameraPositionRadians.toDouble()).toFloat() * cameraDistance
            cameraPosition.z = Math.sin(cameraPositionRadians.toDouble()).toFloat() * cameraDistance
            camera.setPosition(cameraPosition)

            // render
            if (pendingWidth != width || pendingHeight != height) {
                width = pendingWidth
                height = pendingHeight
                glViewport(0, 0, width, height)
            }
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT)

            shader?.use {
                // TODO: make shader handling smarter. the model has a reference to the shader, but we don't want it to
                // call use to reduce redundant state sets
                models.forEach { it.draw() }
            }

            glfwSwapBuffers(window)
            glfwPollEvents()
        }

        // cleanup
        freeModels()
        freeShaders()
    }

    private fun createShaders() {
        camera.setResolution(width, height)
        shader = ShaderProgram(
                Resources.loadAssetAsString("/shaders/basic.vert"),
                Resources.loadAssetAsString("/shaders/basic.frag"),
                camera)
    }

    private fun freeShaders() {
        shader?.free()
        shader = null
    }

    private fun createModels() {
        val shader = shader ?: throw RuntimeException("Shader was null!")
        val ship = ObjImporter.importFile("/models/ship.obj", shader)
        models.add(ship)
    }

    private fun freeModels() {
        models.forEach { it.free() }
    }
}