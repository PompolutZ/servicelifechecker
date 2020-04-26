package se.kry.codetest

import io.vertx.core.*
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.sql.ResultSet
import io.vertx.ext.sql.SQLClient
import org.slf4j.LoggerFactory

const val SERVICE_POLLER_DATABASE_ADDRESS = "service.poller.database"

private const val SQL_CREATE_SERVICES_TABLE = "CREATE TABLE IF NOT EXISTS services (url VARCHAR(128) NOT NULL, name VARCHAR(128), status INTEGER, added VARCHAR(128));"
private const val SQL_FETCH_ALL_SERVICES = "SELECT * FROM services;"
private const val SQL_INSERT_SERVICE = "INSERT INTO services (url, name, status, added) VALUES(?, ?, ?, ?);"
private const val SQL_UPDATE_SERVICE = "UPDATE services SET name = ?, status = ? WHERE url = ?;"
private const val SQL_DELETE_SERVICE = "DELETE FROM services WHERE url = ?;"

const val ACTION_GET_ALL_SERVICES = "GET_ALL_SERVICES"
const val ACTION_ADD_NEW_SERVICE = "ADD_NEW_SERVICE"
const val ACTION_UPDATE_SERVICE = "UPDATE_SERVICE"
const val ACTION_DELETE_SERVICE = "DELETE_SERVICE"

class DBConnector : AbstractVerticle() {
    private val logger = LoggerFactory.getLogger(DBConnector::class.java)
    private val DB_PATH = "poller.db"
    private lateinit var client: SQLClient

    override fun start(startPromise: Promise<Void>?) {
        val config = JsonObject()
                .put("url", "jdbc:sqlite:$DB_PATH")
                .put("driver_class", "org.sqlite.JDBC")
                .put("max_pool_size", 30)

        client = JDBCClient.createShared(vertx, config)

         client.query(SQL_CREATE_SERVICES_TABLE) { done ->
            if(done.succeeded()) {
                logger.info("DBConnectorVerticle started")
                vertx.eventBus().consumer<JsonObject>(SERVICE_POLLER_DATABASE_ADDRESS) { msg -> processMessage(msg) }

                startPromise?.complete()
            } else {
                startPromise?.fail(done.cause())
            }
        }
    }

    private fun processMessage(message: Message<JsonObject>) {
        if(!message.headers().contains("action")) {
            logger.error("Received a message without action header")
            message.fail(404, "No action header in the message")
            return
        }

        when(message.headers().get("action")) {
            ACTION_GET_ALL_SERVICES -> {
                client.query(SQL_FETCH_ALL_SERVICES) { ar ->
                    if(ar.succeeded()) {
                        message.reply(JsonObject().put("data", JsonArray(ar.result().rows)))
                    }
                }
            }

            ACTION_ADD_NEW_SERVICE -> {
                client.queryWithParams(
                        SQL_INSERT_SERVICE,
                        JsonArray().add(message.body().getValue("url"))
                                .add(message.body().getValue("name", ""))
                                .add(message.body().getValue("status"))
                                .add(message.body().getValue("added"))) { done ->
                    if(done.succeeded()) {
                        message.reply("OK")
                    } else {
                        message.fail(EventBusErrorCodes.AsyncResultFailed.ordinal, done.cause().message)
                    }
                }
            }

            ACTION_UPDATE_SERVICE -> {
                client.updateWithParams(
                        SQL_UPDATE_SERVICE,
                        JsonArray().add(message.body().getValue("name"))
                                .add(message.body().getValue("status"))
                                .add(message.body().getValue("url"))) { done ->
                    if(done.succeeded()) {
                        message.reply("OK")
                    } else {
                        message.fail(EventBusErrorCodes.AsyncResultFailed.ordinal, done.cause().message)
                    }
                }
            }

            ACTION_DELETE_SERVICE -> {
                client.queryWithParams(
                        SQL_DELETE_SERVICE,
                        JsonArray().add(message.body().getValue("url"))) { done ->
                    if(done.succeeded()) {
                        message.reply("OK")
                    } else {
                        message.fail(EventBusErrorCodes.AsyncResultFailed.ordinal, done.cause().message)
                    }
                }
            }
        }
    }

//    @JvmOverloads
//    fun query(query: String?, params: JsonArray? = JsonArray()): Future<ResultSet> {
//        var query = query
//        if (query == null || query.isEmpty()) {
//            return Future.failedFuture("Query is null or empty")
//        }
//        if (!query.endsWith(";")) {
//            query = "$query;"
//        }
//        val queryResultFuture = Future.future<ResultSet>()
//        client.queryWithParams(query, params) { result: AsyncResult<ResultSet> ->
//            if (result.failed()) {
//                queryResultFuture.fail(result.cause())
//            } else {
//                queryResultFuture.complete(result.result())
//            }
//        }
//        return queryResultFuture
//    }
}