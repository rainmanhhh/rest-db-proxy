package ez.rest_db_proxy.db

import ez.rest_db_proxy.message.req.SqlReqBody
import ez.vertx.core.AutoDeployVerticle
import ez.vertx.core.busi.BusiVerticle
import ez.vertx.core.err.HttpException
import ez.vertx.core.message.receiveMessage
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.SqlClient
import io.vertx.sqlclient.templates.SqlTemplate

abstract class DbClientVerticle : AutoDeployVerticle, BusiVerticle<List<Any?>>() {
  private lateinit var dbClient: SqlClient

  abstract suspend fun createDbClient(): SqlClient

  override suspend fun start() {
    super.start()
    dbClient = createDbClient()
    receiveMessage(messageExecuteSql, SqlReqBody::class.java) {
      executeSql(it.body)
    }
  }

  private fun isQuery(s: String): Boolean {
    val first = s.trim().apply {
      if (this.isEmpty()) throw HttpException.badRequest("sql is empty")
    }.lines().first {
      val trimmed = it.trim()
      !trimmed.startsWith("--") && trimmed.isNotEmpty()
    }
    return first.trimStart().lowercase().startsWith("select")
  }

  private suspend fun executeSql(req: SqlReqBody) =
    if (isQuery(req.sql)) {
      SqlTemplate.forQuery(dbClient, req.sql).execute(req.params)
        .await()
        .map { it.toJson() }
    } else {
      SqlTemplate.forUpdate(dbClient, req.sql).execute(req.params).await()
      emptyList()
    }

  companion object {
    val messageExecuteSql = DbClientVerticle::class.java.name + ".executeSql"
  }
}
