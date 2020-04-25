package se.kry.codetest

import io.vertx.core.AbstractVerticle
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.client.WebClient
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.HashMap

class BackgroundPollerVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(BackgroundPollerVerticle::class.java)

    override fun start(startPromise: Promise<Void>?) {
        logger.info("Starting BackgroundPoller")
        vertx.setPeriodic(1000 * 10L) { pollServices() }
        startPromise?.complete()
    }

    private fun pollServices() {
        vertx.eventBus().request<JsonObject>(
                SERVICE_POLLER_DATABASE_ADDRESS,
                JsonObject(),
                DeliveryOptions().addHeader("action", ACTION_GET_ALL_SERVICES)) { ar ->
            if(ar.succeeded()) {
                var array = ar.result().body().getJsonArray("data")
                array.list.forEach {
                    var json = it as? JsonObject
                    var urlToCheck = json?.getString("url")
                    val options = DeliveryOptions().addHeader("action", ACTION_UPDATE_SERVICE)

                    vertx.eventBus().request<String>(SERVICE_STATUS_CHECKER_ADDRESS, urlToCheck) { reply ->
                    if(reply.succeeded()) {
                        vertx.eventBus().send(
                                SERVICE_POLLER_DATABASE_ADDRESS,
                                JsonObject()
                                        .put("url", urlToCheck)
                                        .put("status", reply.result().body().toInt())
                                        .put("name", json?.getValue("name")),
                                options)
                    } else {
                        logger.error("Failed to check url $urlToCheck : ${reply.cause()}")
                        vertx.eventBus().send(
                                SERVICE_POLLER_DATABASE_ADDRESS,
                                JsonObject()
                                        .put("url", urlToCheck)
                                        .put("status", -1)
                                        .put("name", json?.getValue("name")),
                                options)
                    }
                }
                }
            } else {
                logger.error("Failed to read from database: ${ar.cause()}")
            }
        }
    }
}