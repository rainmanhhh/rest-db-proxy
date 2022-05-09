package ez.rest_db_proxy.db

import ez.rest_db_proxy.message.receiveMessage
import ez.rest_db_proxy.message.req.SqlReqBody
import io.vertx.core.http.HttpMethod
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
      val httpMethod = it.headers["httpMethod"]?.let(HttpMethod::valueOf)
      val sql = generateSql(httpMethod, it.body).trim()
      SqlReqBody(
        sql,
        it.body.map
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
  open fun generateSql(params: JsonObject): String = ""

  private fun generateSql(httpMethod: HttpMethod?, params: JsonObject): String =
    when (httpMethod) {
      HttpMethod.GET -> get(params)
      HttpMethod.POST -> post(params)
      HttpMethod.DELETE -> delete(params)
      HttpMethod.PUT -> put(params)
      else -> generateSql(params)
    }

  /**
   * like [generateSql] but only deal with [HttpMethod.GET]
   */
  open fun get(params: JsonObject): String = generateSql(params)

  /**
   * like [generateSql] but only deal with [HttpMethod.POST]
   */
  open fun post(params: JsonObject): String = generateSql(params)

  /**
   * like [generateSql] but only deal with [HttpMethod.DELETE]
   */
  open fun delete(params: JsonObject): String = generateSql(params)

  /**
   * like [generateSql] but only deal with [HttpMethod.PUT]
   */
  open fun put(params: JsonObject): String = generateSql(params)
}
