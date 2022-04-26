package ez.rest_db_proxy.message.req

import io.vertx.core.json.JsonObject

class SqlReq(
  var sql: String,
  var params: JsonObject
) {
  constructor(): this("", JsonObject())
}
