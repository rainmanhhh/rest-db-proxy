package ez.rest_db_proxy.config

class VertxConfigVerticle : ConfigVerticle<VertxConfig>() {
  override val key = "vertx"
  override var configValue = VertxConfig()
  override suspend fun afterConfig() {
    VertxConfig.value = configValue
  }
}
