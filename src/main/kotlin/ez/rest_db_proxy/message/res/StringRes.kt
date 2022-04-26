package ez.rest_db_proxy.message.res

class StringRes() : SimpleRes<String>() {
  override var data: String? = null

  constructor(data: String?) : this() {
    this.data = data
  }
}
