package se.kry.codetest

import io.vertx.core.*

enum class EventBusErrorCodes {
    AsyncResultFailed,
    Exception,
}

class MainVerticle : AbstractVerticle() {
    //TODO use this
    private val poller = BackgroundPollerVerticle()

    override fun start(startPromise: Promise<Void>?) {
        CompositeFuture.all(deployDatabaseVerticle(), deployServiceStatusCheckerVerticle())
                .compose { CompositeFuture.all(deployHttpServerVerticle(), deployBackgroundPoller()) }
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

    private fun deployBackgroundPoller(): Future<String> {
        val promise = Promise.promise<String>()
        vertx.deployVerticle(BackgroundPollerVerticle(), promise)

        return promise.future()
    }

    private fun deployHttpServerVerticle(): Future<String> {
        val promise = Promise.promise<String>()

        vertx.deployVerticle(HttpServerVerticle(), promise)

        return promise.future()
    }
}