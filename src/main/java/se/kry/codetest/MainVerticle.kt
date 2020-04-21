package se.kry.codetest

import io.vertx.core.*
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import java.util.*
import java.util.stream.Collectors

enum class EventBusErrorCodes {
    AsyncResultFailed,
    Exception,
}

class MainVerticle : AbstractVerticle() {
    //TODO use this
    private val poller = BackgroundPoller()

    override fun start(startPromise: Promise<Void>?) {
        CompositeFuture.all(deployDatabaseVerticle(), deployServiceStatusCheckerVerticle())
                .compose { deployHttpServerVerticle() }
                .onSuccess {
                    startPromise?.complete()
                }.onFailure {
                    reason -> startPromise?.fail(reason)
                }
    }

    private fun deployDatabaseVerticle(): Future<String> {
        val promise = Promise.promise<String>()
        vertx.deployVerticle(DBConnector(), promise)

        return promise.future()
    }

    private fun deployServiceStatusCheckerVerticle(): Future<String> {
        val promise = Promise.promise<String>()
        vertx.deployVerticle(ServiceStatusCheckerVerticle(), promise)

        return promise.future()
    }

    private fun deployHttpServerVerticle(): Future<String> {
        val promise = Promise.promise<String>()

        vertx.deployVerticle(HttpServerVerticle(), promise)

        return promise.future()
    }
}