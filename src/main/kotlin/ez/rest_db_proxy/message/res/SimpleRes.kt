package ez.rest_db_proxy.message.res

import ez.rest_db_proxy.err.HttpException
import io.netty.handler.codec.http.HttpResponseStatus

open class SimpleRes<Data>() {
  var code: Int? = null

  var message: String? = null

  open var data: Data? = null

  constructor(e: Throwable) : this() {
    if (e is HttpException) {
      code = e.code
      message = e.message
    } else {
      code = HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
      message = e.message
    }
  }

  constructor(code: Int?, message: String?) : this() {
    this.code = code
    this.message = message
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

  companion object {
    fun fromError(e: Throwable) = SimpleRes<Any>(e)
  }
}

fun <Data> SimpleRes<Data>.check() = if (isSuccess()) data else throw toError()
