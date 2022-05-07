package ez.rest_db_proxy.message.res

class MapRes(): SimpleRes<Map<String, Any?>>() {
  override var data: Map<String, Any?>? = null

  constructor(map: Map<String, Any?>): this() {
    data = map
  }
}
