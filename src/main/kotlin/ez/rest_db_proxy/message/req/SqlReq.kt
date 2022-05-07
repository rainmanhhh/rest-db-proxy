package ez.rest_db_proxy.message.req

class SqlReq(
  val sql: String,
  val params: Map<String, Any?>
) {
  constructor() : this("", emptyMap())
}
