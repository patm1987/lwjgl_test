package com.pux0r3.lwjgltest

import mu.KLogging
import org.joml.AxisAngle4f
import org.joml.Quaternionf
import org.joml.Vector3f
import org.lwjgl.Version
import org.lwjgl.glfw.Callbacks.glfwFreeCallbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.glfw.GLFWErrorCallback
import org.lwjgl.glfw.GLFWVidMode
import org.lwjgl.opengl.GL
import org.lwjgl.opengl.GL11.*
import org.lwjgl.opengl.GL14.GL_DECR_WRAP
import org.lwjgl.opengl.GL14.GL_INCR_WRAP
import org.lwjgl.opengl.GL20.glStencilOpSeparate
import org.lwjgl.system.MemoryStack.stackPush
import org.lwjgl.system.MemoryUtil.NULL
import java.nio.IntBuffer

/**
 * Created by pux19 on 5/20/2017.
 */
class Game(private var width: Int, private var height: Int) {
    companion object : KLogging()

    private var window: Long = NULL
    private var shipShader: ShaderProgram? = null
    private var outlineShader: OutlineShader? = null
    private var shadowShader: ShadowShader? = null
    private var unlitShader: UnlitShader? = null
    private var models = mutableListOf<HalfEdgeModel>()

    private val camera = LookAtPerspectiveCamera(
            Math.toRadians(45.0).toFloat(),
            1f,
            .01f,
            100f,
            Vector3f())
    private var pendingWidth = 0

    private var pendingHeight = 0
    // cache the ship model for some hacky fun
    private var ship: HalfEdgeModel? = null

    private var shipMaterial: Material? = null

    init {
        camera.transform.setPosition(Vector3f(0f, .5f, -10f))
    }

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
        glEnable(GL_CULL_FACE)
        glCullFace(GL_BACK)
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f)

        var cameraPositionRadians = 0f
        val cameraDistance = 10f
        val cameraHeight = 10f

        val shipMinHeight = 0f
        val shipMaxHeight = 2f
        val shipMinBob = Math.toRadians(-15.0).toFloat()
        val shipMaxBob = Math.toRadians(15.0).toFloat()
        val shipPeriod = 5f
        var shipT = 0f

        var lastTime = glfwGetTime()
        val cameraPosition = Vector3f(0f, cameraHeight, cameraDistance)
        while (!glfwWindowShouldClose(window)) {
            // game stuff
            val currentTime = glfwGetTime()
            val deltaTime = (currentTime - lastTime).toFloat()
            lastTime = currentTime

            // rotate the camera
            cameraPositionRadians += deltaTime
            cameraPositionRadians %= 2f * Math.PI.toFloat()
            cameraPosition.x = Math.cos(cameraPositionRadians.toDouble()).toFloat() * cameraDistance
            cameraPosition.z = Math.sin(cameraPositionRadians.toDouble()).toFloat() * cameraDistance
            camera.transform.setPosition(cameraPosition)

            // bob the ship
            shipT += deltaTime
            shipT %= (shipPeriod * 2f)
            val shipTNorm = (Math.sin((shipT / shipPeriod).toDouble() * 2.0 * Math.PI).toFloat() + 1f) / 2f
            val shipPosition = Vector3f()
            ship?.transform?.getPosition(shipPosition)
            shipPosition.y = shipMinHeight + shipTNorm * (shipMaxHeight - shipMinHeight)
            ship?.transform?.setPosition(shipPosition)

            // bob forward/back half as fast as up/down
            val shipTBobNorm = (Math.sin((shipT / shipPeriod * .5f).toDouble() * 2.0 * Math.PI).toFloat() + 1f) / 2f
            val shipRotation = Quaternionf(
                    AxisAngle4f(shipMinBob + shipTBobNorm * (shipMaxBob - shipMinBob), 1f, 0f, 0f))
            ship?.transform?.setRotation(shipRotation)

            // render
            if (pendingWidth != width || pendingHeight != height) {
                width = pendingWidth
                height = pendingHeight
                glViewport(0, 0, width, height)
            }
            glClear(GL_COLOR_BUFFER_BIT or GL_DEPTH_BUFFER_BIT or GL_STENCIL_BUFFER_BIT)

            unlitShader?.use {
                // call use to reduce redundant state sets
                models.forEach {
                    renderModel(it)
                }
            }

//            outlineShader?.use {
//                models.forEach {
//                    renderModel(it)
//                }
//            }

            glEnable(GL_STENCIL_TEST)
            glStencilFunc(GL_ALWAYS, 0, -1)
            glColorMask(false, false, false, false)
            glDepthMask(false)
            glDisable(GL_CULL_FACE)
            glStencilOpSeparate(GL_FRONT, GL_KEEP, GL_KEEP, GL_INCR_WRAP)
            glStencilOpSeparate(GL_BACK, GL_KEEP, GL_KEEP, GL_DECR_WRAP)
            shadowShader?.use {
                models.forEach {
                    renderModel(it)
                }
            }
            glStencilOp(GL_KEEP, GL_KEEP, GL_KEEP)
            glEnable(GL_CULL_FACE)
            glDepthMask(true)
            glColorMask(true, true, true, true)

            glClear(GL_DEPTH_BUFFER_BIT)
            glStencilFunc(GL_EQUAL, 0, -1)
            shipShader?.use {
                // call use to reduce redundant state sets
                models.forEach {
                    renderModel(it, shipMaterial!!)
                }
            }
            glStencilFunc(GL_ALWAYS, 0, -1)
            glDisable(GL_STENCIL_TEST)

            glfwSwapBuffers(window)
            glfwPollEvents()
            Utils.checkGlError()
        }

        // cleanup
        freeModels()
        freeShaders()
    }

    private fun createShaders() {
        camera.setResolution(width, height)
        shipShader = shader {
            vertexSource = "/shaders/basic.vert"
            fragmentSource = "/shaders/basic.frag"
            attributes {
                position = "position"
                normal = "normal"
            }
            uniforms {
                viewProjectionMatrix = "ViewProjectionMatrix"
                modelMatrix = "ModelMatrix"
                worldAmbientColor = "WorldAmbient"
                worldLightDirection = "WorldLightDirection"
                worldLightColor = "WorldLightColor"
                modelAmbientColor = "ModelAmbient"
            }
        }
        shipShader?.camera = camera

        outlineShader = OutlineShader(
                Resources.loadAssetAsString("/shaders/outline.vert"),
                Resources.loadAssetAsString("/shaders/outline.geom"),
                Resources.loadAssetAsString("/shaders/outline.frag"))
        outlineShader?.camera = camera
        outlineShader?.edgeThickness = 0.15f

        shadowShader = ShadowShader(
                Resources.loadAssetAsString("/shaders/shadow.vert"),
                Resources.loadAssetAsString("/shaders/shadow.geom"),
                Resources.loadAssetAsString("/shaders/shadow.frag"))
        shadowShader?.camera = camera

        unlitShader = UnlitShader(
                Resources.loadAssetAsString("/shaders/unlit.vert"),
                Resources.loadAssetAsString("/shaders/unlit.frag"))
        unlitShader?.camera = camera
    }

    private fun freeShaders() {
        shipShader?.free()
        shipShader = null
        outlineShader?.free()
        outlineShader = null
    }

    private fun createModels() {
        ship = ObjImporter.importFile("/models/ship.obj")
        shipMaterial = material {
            src { "/models/ship.mtl" }
        }
        ship?.let { models.add(it) }

        val halfEdgeGround = halfEdgeModel {
            vertex {
                position = Vector3f(-1f, 0f, -1f)
                normal = Vector3f(0f, 1f, 0f)
            }
            vertex {
                position = Vector3f(-1f, 0f, 1f)
                normal = Vector3f(0f, 1f, 0f)
            }
            vertex {
                position = Vector3f(1f, 0f, 1f)
                normal = Vector3f(0f, 1f, 0f)
            }
            vertex {
                position = Vector3f(1f, 0f, -1f)
                normal = Vector3f(0f, 1f, 0f)
            }
            face(0, 1, 2)
            face(0, 2, 3)
        }
        halfEdgeGround.transform.setPosition(Vector3f(0f, -2f, 0f))
        halfEdgeGround.transform.setScale(Vector3f(10f, 10f, 10f))
        models.add(halfEdgeGround)
    }

    private fun freeModels() {
        models.forEach { it.free() }
    }
}