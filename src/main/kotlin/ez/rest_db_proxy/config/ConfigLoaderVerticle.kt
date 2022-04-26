package ez.rest_db_proxy.config

import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory

class ConfigLoaderVerticle : CoroutineVerticle() {
  companion object {
    private val logger = LoggerFactory.getLogger(ConfigLoaderVerticle::class.java)
    const val configDir = "conf"
    lateinit var configJson: JsonObject
  }

  override suspend fun start() {
    logger.info("reading config...")
    configJson = ConfigRetriever.create(
      vertx,
      ConfigRetrieverOptions().addStore(
        ConfigStoreOptions()
          .setType("directory")
          .setConfig(
            jsonObjectOf(
              "path" to configDir,
              "filesets" to jsonArrayOf(
                configFileAttrs("yaml", "*.yml"),
                configFileAttrs("yaml", "*.yaml")
              )
            )
          )
      )
    ).config.await()
    logger.debug("configJson: {}", configJson.encodePrettily())
  }

  private fun configFileAttrs(format: String, pattern: String) = JsonObject()
    .put("format", format)
    .put("pattern", pattern)
}
