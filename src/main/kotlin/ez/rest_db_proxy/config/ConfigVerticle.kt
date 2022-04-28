package ez.rest_db_proxy.config

import com.fasterxml.jackson.databind.node.ObjectNode
import com.github.victools.jsonschema.generator.OptionPreset
import com.github.victools.jsonschema.generator.SchemaGenerator
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigBuilder
import com.github.victools.jsonschema.generator.SchemaVersion
import ez.rest_db_proxy.VertxUtil
import ez.rest_db_proxy.verticle.AutoDeployVerticle
import io.vertx.core.buffer.Buffer
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.await
import org.slf4j.LoggerFactory

abstract class ConfigVerticle<ConfigType : Any> : AutoDeployVerticle, CoroutineVerticle() {
  companion object {
    private val logger = LoggerFactory.getLogger(ConfigVerticle::class.java)
    private val configMap = HashMap<String, Any>()

    @JvmStatic
    @Suppress("UNCHECKED_CAST")
    fun <C : Any> get(key: String) = configMap[key] as? C
      ?: throw RuntimeException("config not found! key: $key")
  }

  abstract val key: String

  abstract var configValue: ConfigType

  final override suspend fun start() {
    try {
      logger.debug("generate json schema for [{}]", key)
      generateConfigSchema(ConfigLoaderVerticle.configDir, "$key.schema.json")
    } catch (e: Throwable) {
      logger.warn("generate json schema for [{}] failed", key, e)
    }
    val configJson = ConfigLoaderVerticle.configJson.getJsonObject(key, JsonObject())
    configValue = configJson.mapTo(configValue.javaClass)
    configMap[key] = configValue
    afterConfig()
  }

  abstract suspend fun afterConfig()

  private fun ObjectNode.getObj(childName: String) =
    get(childName) as ObjectNode? ?: putObject(childName)

  private suspend fun generateConfigSchema(configDir: String, fileName: String) {
    val configBuilder =
      SchemaGeneratorConfigBuilder(SchemaVersion.DRAFT_7, OptionPreset.PLAIN_JSON)
    val config = configBuilder.build()
    val generator = SchemaGenerator(config)
    val jsonSchema = generator.generateSchema(configValue.javaClass)

    val defsNode = jsonSchema.getObj("definitions")
    defsNode.putObject(configValue.javaClass.name)
      .put("type", "object")
      .putPOJO("properties", jsonSchema.getObj("properties"))
    jsonSchema.putObject("properties")
      .putObject(key)
      .put("\$ref", "#/definitions/" + configValue.javaClass.name)

    val configSchemaStr = jsonSchema.toPrettyString()
    val vertx = VertxUtil.vertx()
    val schemaDir = "$configDir/schema"
    vertx.fileSystem().mkdirs(schemaDir)
    vertx.fileSystem().writeFile("$schemaDir/$fileName", Buffer.buffer(configSchemaStr)).await()
  }
}
