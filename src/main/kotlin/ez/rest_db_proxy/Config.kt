package ez.rest_db_proxy

import io.vertx.core.VertxOptions
import io.vertx.core.http.HttpServerOptions
import io.vertx.sqlclient.PoolOptions
import io.vertx.sqlclient.SqlConnectOptions

class Config {
  /**
   * vertx app options
   */
  var vertx = VertxOptions()

  /**
   * http server options, such as port
   */
  var httpServer = HttpServerOptions()

  /**
   * always use http 200 status code in response header
   */
  var alwaysUseOkStatus = false

  /**
   * max request handling time(milliseconds)
   */
  var timeout = 10000L

  /**
   * verticle file directory(relative path of cwd, without prefix `./` or suffix `/`）
   */
  var verticleRoot = "verticles"

  /**
   * you should pass this queryParam(name is `_apiKey`) when accessing admin resource or manually executing sql
   */
  var apiKey = ""

  /**
   * database connection config. see [DbConfig]
   */
  var db = DbConfig()

  /**
   * error message prefixes. see [ErrorMessageConfig]
   */
  var error = ErrorMessageConfig()

  companion object {
    @JvmStatic
    var instance = Config()
  }
}

class DbConfig {
  /**
   * connect options, such as host, port, username and password. see [SqlConnectOptions]
   */
  var connect = SqlConnectOptions()

  /**
   * pool options. see [PoolOptions]
   */
  var pool = PoolOptions()
}

/**
 * error message prefixes
 */
class ErrorMessageConfig {
  /**
   * required param not found in request
   */
  var paramRequired = "param required:"

  /**
   * param format error
   */
  var paramFormatError = "param format error:"

  /**
   * server-side apiKey config value is null
   */
  var serverApiKeyNotSet = "server api key not set"

  /**
   * apiKey in request is not match with server-side config value
   */
  var requestApiKeyIncorrect = "request api key incorrect"
}
