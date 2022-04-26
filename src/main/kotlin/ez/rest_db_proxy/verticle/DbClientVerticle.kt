package ez.rest_db_proxy.verticle

import ez.rest_db_proxy.message.receiveMessage
import ez.rest_db_proxy.message.req.SqlReq
import ez.rest_db_proxy.message.res.ListRes
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate
import kotlinx.coroutines.launch

abstract class DbClientVerticle : AutoDeployVerticle, CoroutineVerticle() {
  private lateinit var dbClient: SqlClient

  abstract suspend fun createDbClient(): SqlClient

  override suspend fun start() {
    dbClient = createDbClient()
    receiveMessage(messageExecuteSql, SqlReq::class.java) { req, message ->
      launch {
        val list = executeSql(req)
        val r = ListRes(list)
        message.reply(JsonObject.mapFrom(r))
      }
    }
  }

  private fun isQuery(s: String): Boolean {
    val first = s.lines().first {
      !it.startsWith("--") && it.isNotBlank()
    }
    return first.trimStart().lowercase().startsWith("select")
  }

  private suspend fun executeSql(req: SqlReq) =
    if (isQuery(req.sql)) {
      SqlTemplate.forQuery(dbClient, req.sql).execute(req.params.map)
        .await()
        .map { it.toJson() }
    } else {
      SqlTemplate.forUpdate(dbClient, req.sql).execute(req.params.map).await()
      emptyList()
    }

  companion object {
    val messageExecuteSql = DbClientVerticle::class.java.name + ".executeSql"
  }
}
