package ez.rest_db_proxy.config

import io.vertx.core.VertxOptions
import io.vertx.core.eventbus.DeliveryOptions

class VertxConfig : VertxOptions() {
  /**
   * eventBus message min delivery timeout, in ms
   */
  var minMessageTimeout = DeliveryOptions.DEFAULT_TIMEOUT

  companion object {
    var value = VertxConfig()
  }
}
