package ez.rest_db_proxy.verticle

import ez.rest_db_proxy.err.HttpException
import ez.rest_db_proxy.message.BusiMessage
import io.vertx.core.AbstractVerticle
import io.vertx.core.json.JsonObject
import org.slf4j.LoggerFactory

abstract class SqlVerticle : AbstractVerticle() {
  private val logger = LoggerFactory.getLogger(javaClass)

  override fun start() {
    val p = path() ?: throw NullPointerException(javaClass.name + ".path should not return null")
    if (p == "/" || p.startsWith("/_admin/"))
      throw IllegalArgumentException(
        javaClass.name + " should not use `/` or `/_admin/**/*` path which are reserved by system handlers"
      )
    vertx.eventBus().consumer<JsonObject>(p).handler {
      val req = it.body()
      try {
        val sql = generateSql(req)
        it.reply(BusiMessage().apply { data = sql })
      } catch (e: Throwable) {
        if (e !is HttpException) logger.error("sqlVerticle error", e)
        it.reply(BusiMessage(e))
      }
    }
  }

  abstract fun path(): String?

  abstract fun generateSql(params: JsonObject): String
}
