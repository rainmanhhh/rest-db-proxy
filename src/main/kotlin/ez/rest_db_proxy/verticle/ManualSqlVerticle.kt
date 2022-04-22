package ez.rest_db_proxy.verticle

import ez.rest_db_proxy.ApiKey
import ez.rest_db_proxy.err.HttpException
import io.vertx.core.json.JsonObject

class ManualSqlVerticle : SqlVerticle() {
  override fun path(): String = "/manualSql"

  override fun generateSql(params: JsonObject): String {
    ApiKey.check(params)
    val paramName = "sql"
    return params.getString(paramName) ?: throw HttpException.require(paramName)
  }
}
