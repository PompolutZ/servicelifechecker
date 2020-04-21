package se.kry.codetest

import io.vertx.core.Vertx

object Start {
    @JvmStatic
    fun main(args: Array<String>) {
        Vertx.vertx().deployVerticle(MainVerticle())
    }
}