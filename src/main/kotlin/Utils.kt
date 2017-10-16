import com.pux0r3.lwjgltest.Game
import mu.KLogging
import org.lwjgl.opengl.GL11

object Utils: KLogging (){
    fun checkGlError() {
        val error = GL11.glGetError()
        if (error != GL11.GL_NO_ERROR) {
            Game.logger.error { "Failed because $error" }
            when (error) {
                GL11.GL_INVALID_VALUE -> Game.logger.error { "Invalid Value" }
                GL11.GL_INVALID_OPERATION -> Game.logger.error { "Invalid Operation" }
            }
        }
    }
}