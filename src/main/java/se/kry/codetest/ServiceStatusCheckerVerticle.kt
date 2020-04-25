package se.kry.codetest

import io.vertx.core.AbstractVerticle
import io.vertx.core.Promise
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.slf4j.LoggerFactory

const val SERVICE_STATUS_CHECKER_ADDRESS = "service.status.checker"

class ServiceStatusCheckerVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(ServiceStatusCheckerVerticle::class.java)
    private lateinit var webClient: WebClient

    override fun start(startPromise: Promise<Void>?) {
        webClient = WebClient.create(vertx);
        vertx.eventBus().consumer<String>(SERVICE_STATUS_CHECKER_ADDRESS) { msg -> processMessage(msg) }

        logger.info("Starting Service Status Checker")

        startPromise?.complete()
    }

    private fun processMessage(message: Message<String>) {
        WebClient.create(vertx).getAbs(message.body()).send { ar ->
            if(ar.succeeded()) {
                message.reply(ar.result().statusCode().toString())
            } else {
                message.fail(EventBusErrorCodes.AsyncResultFailed.ordinal, ar.cause().message)
            }
        }
    }
}