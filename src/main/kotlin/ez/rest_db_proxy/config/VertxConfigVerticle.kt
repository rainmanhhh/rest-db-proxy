package ez.rest_db_proxy.config

import io.vertx.core.VertxOptions

class VertxConfigVerticle : ConfigVerticle<VertxOptions>() {
  override val key = "vertx"
  override var configValue = VertxOptions()
  override suspend fun afterConfig() = Unit
}
