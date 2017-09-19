/**
 * Created by pux19 on 5/20/2017.
 */
package com.pux0r3.lwjgltest

import kotlin.concurrent.thread

fun main (args: Array<String>) {
    println("hello world")
    val game = Game(1600, 900)
    thread {
        game.run()
    }
}
