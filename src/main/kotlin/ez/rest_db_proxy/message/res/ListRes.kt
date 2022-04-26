package ez.rest_db_proxy.message.res

class ListRes() : SimpleRes<List<*>>() {
  override var data: List<*>? = null

  constructor(list: List<*>) : this() {
    data = list
  }
}
