package ez.rest_db_proxy.verticle

import ez.rest_db_proxy.ApiKey
import ez.rest_db_proxy.Config
import ez.rest_db_proxy.handlers.DeployHandler
import ez.rest_db_proxy.handlers.SqlHandler
import ez.rest_db_proxy.message.BusiMessage
import ez.rest_db_proxy.toJson
import io.netty.handler.codec.http.HttpResponseStatus
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.FaviconHandler
import io.vertx.ext.web.handler.LoggerHandler
import io.vertx.ext.web.handler.TimeoutHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import io.vertx.sqlclient.Pool
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

class HttpServerVerticle : CoroutineVerticle() {
  companion object {
    private val logger = LoggerFactory.getLogger(HttpServerVerticle::class.java)!!
    private val adminHtml = HttpServerVerticle::class.java.getResource("/admin.html").readText()
  }

  private lateinit var cfg: Config
  private lateinit var httpServer: HttpServer
  private lateinit var router: Router
  private lateinit var dbClient: Pool

  override suspend fun start() {
    cfg = Config.instance
    createBeans()
    startHttpServer()
  }

  private fun createBeans() {
    logger.info("creating beans...")
    httpServer = vertx.createHttpServer(cfg.httpServer)
    router = Router.router(vertx)
    dbClient = Pool.pool(vertx, cfg.db.connect, cfg.db.pool)
    logger.info("beans created")
  }

  /**
   * 注册所有的web handler，并启动http服务
   */
  private suspend fun startHttpServer() {
    logger.info("starting httpServer...")
    router.route().handler(TimeoutHandler.create(cfg.timeout))
    router.route().handler(LoggerHandler.create())
    router.post().handler(BodyHandler.create())
    router.get("/favicon.ico").handler(FaviconHandler.create(vertx))
    router.get("/").handler(this::handleAdminHtml)
    router.get("/_admin/deploy").handler(DeployHandler(this))
    router.get("/_admin/*").handler(this::handleAdmin)
    router.get().handler(SqlHandler(this, dbClient)).failureHandler(this::handleError)
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
    val jsonParams = ctx.queryParams().toJson()
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
        if (Config.instance.alwaysUseOkStatus) HttpResponseStatus.OK.code()
        else statusCode
      )
      .putHeader(HttpHeaders.CONTENT_TYPE, "application/json;charset=utf-8")
      .end(BusiMessage().also {
        it.code = statusCode
        it.message = message
      }.toBuffer())
      .await()
  }
}
