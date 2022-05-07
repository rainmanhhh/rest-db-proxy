package ez.rest_db_proxy.handlers

import ez.rest_db_proxy.db.DbClientVerticle
import ez.rest_db_proxy.err.HttpException
import ez.rest_db_proxy.message.req.SqlReq
import ez.rest_db_proxy.message.res.ListRes
import ez.rest_db_proxy.message.res.MapRes
import ez.rest_db_proxy.message.res.check
import ez.rest_db_proxy.message.sendMessage
import ez.rest_db_proxy.paramsAsJson
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import org.slf4j.LoggerFactory

/**
 * - send message to [ez.rest_db_proxy.db.GenerateSqlVerticle] to get sql template
 * - send message to [ez.rest_db_proxy.db.DbClientVerticle] to execute sql
 */
class SqlHandler(scope: CoroutineScope) : CoroutineHandler(scope) {
  companion object {
    private val logger = LoggerFactory.getLogger(SqlHandler::class.java)
  }

  override suspend fun handleAsync(ctx: RoutingContext): Boolean {
    if (ctx.response().ended()) return false
    val address = ctx.normalizedPath()
    val paramJson = ctx.paramsAsJson()
    logger.debug("req path: {}, paramJson: {}", address, paramJson)
    val mapRes = sendMessage(address, paramJson, MapRes::class.java).check()
    logger.debug("generated mapRes: {}", mapRes)
    val sqlReq = JsonObject(mapRes).mapTo(SqlReq::class.java)
    if (sqlReq.sql.isBlank()) {
      ctx.fail(HttpException.internalErr("generated sql is empty! req path:[$address]"))
    } else {
      val jsonArray = sendMessage(
        DbClientVerticle.messageExecuteSql,
        sqlReq,
        ListRes::class.java
      ).check()
      ctx.response().putHeader(
        HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8"
      ).end(Json.encode(jsonArray)).await()
    }
    return false
  }
}
