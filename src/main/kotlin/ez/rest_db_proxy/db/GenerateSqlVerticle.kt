package ez.rest_db_proxy.db

import ez.rest_db_proxy.message.receiveMessage
import ez.rest_db_proxy.message.req.SqlReq
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle

abstract class GenerateSqlVerticle : CoroutineVerticle() {
  override suspend fun start() {
    val p = path() ?: throw NullPointerException(javaClass.name + ".path should not return null")
    if (p == "/" || p.startsWith("/_admin/"))
      throw IllegalArgumentException(
        javaClass.name + " should not use `/` or `/_admin/**/*` path which are reserved by system handlers"
      )
    receiveMessage(p) {
      SqlReq(
        generateSql(it).trim(),
        it.map
      )
    }
  }

  /**
   * should return the request path(start with `/`). eg: `/getUserList`
   */
  abstract fun path(): String?

  /**
   * - should return the sql statement. eg: `select t.a from t where t.b = #{paramB}`
   * - you can add/update/delete params in this function
   */
  abstract fun generateSql(params: JsonObject): String
}
