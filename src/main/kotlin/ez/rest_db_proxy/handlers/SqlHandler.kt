package ez.rest_db_proxy.handlers

import ez.rest_db_proxy.err.HttpException
import ez.rest_db_proxy.message.BusiMessage
import ez.rest_db_proxy.toJson
import io.vertx.core.http.HttpHeaders
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Pool
import io.vertx.sqlclient.templates.SqlTemplate
import kotlinx.coroutines.CoroutineScope

class SqlHandler(
  scope: CoroutineScope,
  private val dbClient: Pool
) : CoroutineHandler(scope) {
  override suspend fun handleAsync(ctx: RoutingContext): Boolean {
    if (ctx.response().ended()) return false
    val eb = ctx.vertx().eventBus()
    val address = ctx.normalizedPath()
    val paramJson = ctx.queryParams().toJson()
    val resBody = eb.request<JsonObject>(address, paramJson).await().body()
    val resMessage = BusiMessage(resBody)
    if (!resMessage.isSuccess()) {
      ctx.fail(resMessage.toError())
    } else {
      val sql = resMessage.data
      if (sql == null) {
        ctx.fail(HttpException.internalErr("generated sql is null"))
      } else {
        val resultList = executeSql(ctx, sql)
        ctx.response().putHeader(
          HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8"
        ).end(Json.encode(resultList)).await()
      }
    }
    return false
  }

  private fun isQuery(s: String): Boolean {
    val first = s.lines().first {
      !it.startsWith("--") && it.isNotBlank()
    }
    return first.trimStart().lowercase().startsWith("select ")
  }

  private suspend fun executeSql(ctx: RoutingContext, sql: String) =
    if (isQuery(sql)) {
      val rowSet =
        SqlTemplate.forQuery(dbClient, sql).execute(ctx.queryParams().toJson().map).await()
      rowSet.map { it.toJson() }
    } else {
      SqlTemplate.forUpdate(dbClient, sql).execute(ctx.queryParams().toJson().map).await()
      emptyList()
    }
}
