package ez.rest_db_proxy.handlers

import io.vertx.core.Handler
import io.vertx.ext.web.RoutingContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

abstract class CoroutineHandler(protected val scope: CoroutineScope) : Handler<RoutingContext> {

  override fun handle(ctx: RoutingContext) {
    scope.launch {
      try {
        val next = handleAsync(ctx)
        if (next) ctx.next()
      } catch (e: Throwable) {
        ctx.fail(e)
      }
    }
  }

  /**
   * @return true - continue to next handler; false - response#end already called
   */
  abstract suspend fun handleAsync(ctx: RoutingContext): Boolean
}
