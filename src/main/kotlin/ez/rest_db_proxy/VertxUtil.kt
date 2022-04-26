package ez.rest_db_proxy

import io.vertx.core.Vertx

object VertxUtil {
  fun vertx() =
    Vertx.currentContext()?.owner() ?: throw RuntimeException("not running in vertx context")
}
