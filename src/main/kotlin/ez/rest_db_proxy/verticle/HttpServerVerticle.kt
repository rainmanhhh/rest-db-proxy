package ez.rest_db_proxy.verticle

import ez.rest_db_proxy.ApiKey
import ez.rest_db_proxy.handlers.DeployHandler
import ez.rest_db_proxy.handlers.SqlHandler
import ez.rest_db_proxy.message.res.SimpleRes
import ez.rest_db_proxy.paramsAsJson
import ez.rest_db_proxy.config.ConfigVerticle
import ez.rest_db_proxy.config.HttpServerConfig
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServer
import io.vertx.core.json.Json
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.FaviconHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.TimeoutHandler
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class HttpServerVerticle : ConfigVerticle<HttpServerConfig>() {
  companion object {
    private val logger = LoggerFactory.getLogger(HttpServerVerticle::class.java)!!
    private val adminHtml = HttpServerVerticle::class.java.getResource("/admin.html").readText()
  }

  override val key = "httpServer"

  override var configValue = HttpServerConfig()

  private lateinit var httpServer: HttpServer
  private lateinit var router: Router

  override suspend fun afterConfig() {
    HttpServerConfig.value = configValue
    createBeans()
    startHttpServer()
  }

  private fun createBeans() {
    logger.info("creating beans...")
    httpServer = vertx.createHttpServer(configValue)
    router = Router.router(vertx)
    logger.info("beans created")
  }

  /**
   * 注册所有的web handler，并启动http服务
   */
  private suspend fun startHttpServer() {
    logger.info("starting httpServer...")
    router.route().handler(TimeoutHandler.create(configValue.timeout))
    router.route().handler(LoggerHandler.create())
    router.post().handler(BodyHandler.create())
    router.get("/favicon.ico").handler(FaviconHandler.create(vertx))
    router.get("/").handler(this::handleAdminHtml)
    router.post("/_admin/deploy").handler(DeployHandler(this))
    router.post("/_admin/*").handler(this::handleAdmin)
    router.route().handler(SqlHandler(this)).failureHandler(this::handleError)
    val server = httpServer.requestHandler(router).listen().await()
    logger.info("httpServer started at {}", server.actualPort())
  }

  private fun handleAdminHtml(ctx: RoutingContext) = launch {
    ctx.response()
      .putHeader(HttpHeaders.CONTENT_TYPE, "text/html;charset=utf-8")
      .end(adminHtml)
      .await()
  }

  private fun handleAdmin(ctx: RoutingContext) = launch {
    val eb = ctx.vertx().eventBus()
    val address = ctx.normalizedPath()
    val jsonParams = ctx.paramsAsJson()
    ApiKey.check(jsonParams)
    val res = eb.request<String>(address, jsonParams).await().body()
    ctx.response().end(res)
  }

  private fun handleError(ctx: RoutingContext) = launch {
    var statusCode = ctx.statusCode()
    if (statusCode < 0) statusCode = HttpResponseStatus.INTERNAL_SERVER_ERROR.code()
    val err = ctx.failure()
    if (err != null) {
      if (err is ez.rest_db_proxy.err.HttpException) statusCode = err.code
      else logger.error(err.message, err)
    }
    val message = err?.let { it.javaClass.name + ":" + it.message }
      ?: HttpResponseStatus.valueOf(statusCode).reasonPhrase()
    ctx.response()
      .setStatusCode(
        if (configValue.alwaysUseOkStatus) HttpResponseStatus.OK.code()
        else statusCode
      )
      .putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
      .end(Json.encodeToBuffer(SimpleRes<Any>(statusCode, message)))
      .await()
  }
}
