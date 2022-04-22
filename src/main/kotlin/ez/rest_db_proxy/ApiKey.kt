package ez.rest_db_proxy

import ez.rest_db_proxy.err.HttpException
import io.vertx.core.json.JsonObject

class ApiKey {
  companion object {
    const val paramName = "_apiKey"

    @JvmStatic
    fun check(params: JsonObject) {
      check(params.getString(paramName) ?: throw HttpException.require(paramName))
    }

    @JvmStatic
    fun check(input: String?) {
      if (Config.instance.apiKey == "")
        throw HttpException.internalErr(Config.instance.error.serverApiKeyNotSet)
      if (input != Config.instance.apiKey)
        throw HttpException.forbidden(Config.instance.error.requestApiKeyIncorrect)
    }
  }
}
