package ez.rest_db_proxy.db

import ez.rest_db_proxy.message.receiveMessage
import ez.rest_db_proxy.message.res.StringRes
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

abstract class GenerateSqlVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val p = path() ?: throw NullPointerException(javaClass.name + ".path should not return null")
    if (p == "/" || p.startsWith("/_admin/"))
      throw IllegalArgumentException(
        javaClass.name + " should not use `/` or `/_admin/**/*` path which are reserved by system handlers"
      )
    receiveMessage(p) { req, message ->
      val sql = generateSql(req)
      val r = StringRes(sql)
      message.reply(JsonObject.mapFrom(r))
    }
  }

  abstract fun path(): String?

  abstract fun generateSql(params: JsonObject): String
}
