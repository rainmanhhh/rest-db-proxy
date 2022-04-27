package ez.rest_db_proxy.message.req

class SqlReq(
  var sql: String,
  var params: Map<String, Any?>
) {
  constructor() : this("", emptyMap())
}
