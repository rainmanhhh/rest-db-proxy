package ez.rest_db_proxy.verticle

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import ez.rest_db_proxy.Config
import ez.rest_db_proxy.handlers.DeployHandler
import io.vertx.config.ConfigRetriever
import io.vertx.config.ConfigRetrieverOptions
import io.vertx.config.ConfigStoreOptions
import io.vertx.core.DeploymentOptions
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.core.json.jackson.DatabindCodec
import io.vertx.kotlin.core.json.jsonArrayOf
import io.vertx.kotlin.core.json.jsonObjectOf
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory
import java.io.File

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
    logger.info("cwd: {}", File(".").absolutePath)
    readConfig()
    val cfg = Config.instance
    val newVertx = Vertx.vertx(cfg.vertx)
    newVertx.deployVerticle(
      HttpServerVerticle::class.java,
      DeploymentOptions()
        .setInstances(cfg.vertx.eventLoopPoolSize)
    ).await()
    newVertx.deployVerticle(ManualSqlVerticle()).await()
    DeployHandler.deployDir(newVertx, cfg.verticleRoot)
    vertx.close {
      logger.info("launcher vertx instance closed")
    }
    logger.info("MainVerticle started")
  }

  private suspend fun readConfig() {
    logger.info("reading config...")
    val configDir = "conf"
    generateConfigSchema(configDir, "config.schema.json")
    val configJson = ConfigRetriever.create(
      vertx,
      ConfigRetrieverOptions().addStore(
        ConfigStoreOptions()
          .setType("directory")
          .setConfig(
            jsonObjectOf(
              "path" to configDir,
              "filesets" to jsonArrayOf(
                configFileAttrs("yaml"),
                configFileAttrs("json")
              )
            )
          )
      )
    ).config.await()
    logger.debug("config: {}", configJson.encodePrettily())
    Config.instance = configJson.mapTo(Config::class.java)
  }

  private fun configFileAttrs(format: String) = JsonObject()
    .put("format", format)
    .put("pattern", "*.$format")

  private suspend fun generateConfigSchema(configDir: String, fileName: String) {
    val configBuilder =
      SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_2019_09, OptionPreset.PLAIN_JSON)
    val config = configBuilder.build()
    val generator = SchemaGenerator(config)
    val jsonSchema: JsonNode = generator.generateSchema(Config::class.java)
    val configSchemaStr = jsonSchema.toPrettyString()
    val schemaDir = "$configDir/schema"
    vertx.fileSystem().mkdirs(schemaDir)
    vertx.fileSystem().writeFile("$schemaDir/$fileName", Buffer.buffer(configSchemaStr)).await()
  }
}
