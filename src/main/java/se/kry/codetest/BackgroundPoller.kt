package se.kry.codetest

import io.vertx.core.Future
import java.util.HashMap

class BackgroundPoller {
    fun pollServices(services: HashMap<String, String>): Future<List<String>> {
        //TODO
        return Future.failedFuture("TODO")
    }
}