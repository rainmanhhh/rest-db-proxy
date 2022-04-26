package ez.rest_db_proxy.verticle

import com.fasterxml.jackson.databind.DeserializationFeature
import ez.rest_db_proxy.config.ConfigLoaderVerticle
import ez.rest_db_proxy.config.VertxConfigVerticle
import ez.rest_db_proxy.handlers.DeployHandler
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory
import java.io.File
import java.util.*

class MainVerticle : CoroutineVerticle() {
  companion object {
    private val logger = LoggerFactory.getLogger(MainVerticle::class.java)!!

    init {
      DatabindCodec.mapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
      DatabindCodec.prettyMapper()
        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
    }
  }

  override suspend fun start() {
    // begin
    logger.info("cwd: {}", File(".").absolutePath)
    // base config(read config json value and map to VertxOptions)
    vertx.deployVerticle(ConfigLoaderVerticle()).await()
    val vertxConfigVerticle = VertxConfigVerticle()
    vertx.deployVerticle(vertxConfigVerticle).await()
    // use VertxOptions to create new Vertx instance
    val vertxOptions: VertxOptions = vertxConfigVerticle.configValue
    val newVertx = Vertx.vertx(vertxOptions)
    // auto deploy verticles
    for (verticle in ServiceLoader.load(AutoDeployVerticle::class.java)) {
      logger.info("auto deploy verticle: {}", verticle)
      newVertx.deployVerticle(verticle).await()
    }
    // http server verticle is special: use event loop pool size as instance amount
    newVertx.deployVerticle(
      HttpServerVerticle::class.java,
      DeploymentOptions()
        .setInstances(vertxOptions.eventLoopPoolSize)
    ).await()
    // deploy dynamic verticles
    DeployHandler.deployDir(newVertx, "verticles")
    // close old Vertx instance
    vertx.close {
      logger.info("launcher vertx instance closed")
    }
    // end
    logger.info("MainVerticle started")
  }
}
