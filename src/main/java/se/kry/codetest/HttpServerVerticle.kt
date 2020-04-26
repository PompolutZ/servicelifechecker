package se.kry.codetest

import io.vertx.core.AbstractVerticle
import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Promise
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.client.WebClient
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.StaticHandler
import org.slf4j.LoggerFactory
import java.lang.Exception
import java.time.Instant
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.HashMap
import java.util.stream.Collectors

class HttpServerVerticle : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(HttpServerVerticle::class.java)

    override fun start(startPromise: Promise<Void>?) {
        val router = Router.router(vertx)
        router.route().handler(BodyHandler.create())
        setRoutes(router)
        vertx
            .createHttpServer()
            .requestHandler(router)
            .listen(8080) { result: AsyncResult<HttpServer?> ->
                if (result.succeeded()) {
                    logger.info("HTTPServerVerticle started")
                    startPromise?.complete()
                } else {
                    startPromise?.fail(result.cause())
                }
            }
    }

    private fun setRoutes(router: Router) {
        router.route("/*").handler(StaticHandler.create())
        router["/service"].handler { req: RoutingContext ->
            vertx.eventBus().request<JsonObject>(
                    SERVICE_POLLER_DATABASE_ADDRESS,
                    JsonObject(),
                    DeliveryOptions().addHeader("action", ACTION_GET_ALL_SERVICES)) { ar ->
                if(ar.succeeded()) {
                    req.response()
                            .putHeader("content-type", "application/json")
                            .end(ar.result().body().getJsonArray("data").encode());
                }
            }
        }
        router.post("/service").handler { req: RoutingContext ->
            val jsonBody = req.bodyAsJson
            vertx.eventBus().request<String>(SERVICE_STATUS_CHECKER_ADDRESS, jsonBody.getString("url")) { reply ->
                if(reply.failed()) {
                    req.response().setStatusCode(400).end(reply.cause().message)
                } else {
                    val options = DeliveryOptions().addHeader("action", ACTION_ADD_NEW_SERVICE)
                    vertx.eventBus().request<JsonObject>(
                            SERVICE_POLLER_DATABASE_ADDRESS,
                            JsonObject()
                                    .put("url", jsonBody.getString("url"))
                                    .put("name", jsonBody.getString("name"))
                                    .put("status", reply.result().body().toInt())
                                    .put("added", DateTimeFormatter.ISO_INSTANT.format(Instant.now()).toString()),
                            options) { dbReply ->
                        if(dbReply.failed()) {
                            req.response().setStatusCode(500).end(dbReply.cause().message)
                        } else {
                            req.response().end("OK")
                        }
                    }
                }
            }
        }

        router.delete("/service").handler { req ->
            val jsonBody = req.bodyAsJson
            vertx.eventBus().request<JsonObject>(
                    SERVICE_POLLER_DATABASE_ADDRESS,
                    JsonObject().put("url", jsonBody.getString("url")),
                    DeliveryOptions().addHeader("action", ACTION_DELETE_SERVICE)) { dbReply ->
                if(dbReply.failed()) {
                    req.response().setStatusCode(500).end(dbReply.cause().message)
                } else {
                    req.response().end("OK")
                }
            }
        }
    }
}