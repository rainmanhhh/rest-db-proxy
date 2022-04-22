package ez.rest_db_proxy.message

import ez.rest_db_proxy.err.HttpException
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.json.JsonObject

class BusiMessage() : JsonObject() {
  var code: Int? by map

  var message: String? by map

  var data: String?
    get() = getString("data")
    set(value) {
      put("data", value)
    }

  constructor(jsonObject: JsonObject) : this() {
    code = jsonObject.getInteger("code")
    message = jsonObject.getString("message")
    data = jsonObject.getString("data")
  }

  constructor(e: Throwable) : this() {
    if (e is HttpException) {
      code = e.code
      message = e.message
    } else {
      code = HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
      message = e.message
    }
  }

  fun isSuccess(): Boolean {
    val c = code
    return (c == null || c >= HttpResponseStatus.OK.code() && c < HttpResponseStatus.BAD_REQUEST.code())
  }

  fun toError(): HttpException {
    val c = code ?: HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
    val m = message ?: HttpResponseStatus.valueOf(c).reasonPhrase()
    return HttpException(c, m)
  }
}
