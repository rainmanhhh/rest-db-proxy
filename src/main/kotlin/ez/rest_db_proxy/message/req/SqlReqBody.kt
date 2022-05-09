package ez.rest_db_proxy.message.req

data class SqlReqBody(
  val sql: String,
  val params: Map<String, Any?>
) {
  constructor() : this("", emptyMap())
}
