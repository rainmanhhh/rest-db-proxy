package ez.rest_db_proxy.err

import ez.rest_db_proxy.config.ErrorConfig
import io.netty.handler.codec.http.HttpResponseStatus

class HttpException(val code: Int, message: String) : Exception(message) {
  override fun fillInStackTrace(): Throwable {
    return this
  }

  companion object {
    private val config: ErrorConfig
      get() = ErrorConfig.value

    fun badRequest(message: String) = HttpResponseStatus.BAD_REQUEST.err(message)
    fun forbidden(message: String) = HttpResponseStatus.FORBIDDEN.err(message)
    fun internalErr(message: String) = HttpResponseStatus.INTERNAL_SERVER_ERROR.err(message)
    fun require(name: String) = badRequest(config.message.paramRequired + name)
    fun formatError(name: String) = badRequest(config.message.paramFormatError + name)
  }
}

fun HttpResponseStatus.err(message: String) = HttpException(code(), message)
