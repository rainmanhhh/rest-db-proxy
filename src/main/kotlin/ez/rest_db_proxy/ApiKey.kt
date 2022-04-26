package ez.rest_db_proxy

import ez.rest_db_proxy.config.ErrorConfig
import ez.rest_db_proxy.config.HttpServerConfig
import ez.rest_db_proxy.err.HttpException
import io.vertx.core.json.JsonObject

class ApiKey {
  companion object {
    const val paramName = "_apiKey"
    private val httpServerConfig: HttpServerConfig
      get() = HttpServerConfig.value
    private val errorConfig: ErrorConfig
      get() = ErrorConfig.value

    @JvmStatic
    fun check(params: JsonObject) {
      check(params.getString(paramName) ?: throw HttpException.require(paramName))
    }

    @JvmStatic
    fun check(input: String?) {
      if (httpServerConfig.apiKey == "")
        throw HttpException.internalErr(errorConfig.message.serverApiKeyNotSet)
      if (input != httpServerConfig.apiKey)
        throw HttpException.forbidden(errorConfig.message.requestApiKeyIncorrect)
    }
  }
}
