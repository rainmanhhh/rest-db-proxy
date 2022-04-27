package ez.rest_db_proxy.message

import ez.rest_db_proxy.VertxUtil
import ez.rest_db_proxy.message.res.SimpleRes
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

object MessageEx {
  internal val logger = LoggerFactory.getLogger(MessageEx::class.java)
}

/**
 * @param req should be an object which can be mapped to a [JsonObject]
 * @param resClass should be an object class which can be mapped from a [JsonObject]
 */
suspend fun <Req, Res : SimpleRes<*>> sendMessage(
  address: String,
  req: Req,
  resClass: Class<Res>
): Res {
  MessageEx.logger.debug(
    "send message to address: {}, req: {}, resClass: {}",
    address,
    req,
    resClass
  )
  val jsonReq = JsonObject.mapFrom(req)
  val resBody = VertxUtil.vertx().eventBus().request<JsonObject>(address, jsonReq).await().body()
  MessageEx.logger.debug("resBody: {}", resBody)
  return resBody.mapTo(resClass)
}

/**
 * @param reqClass should be an object class which can be mapped from a [JsonObject]
 */
fun <Req> CoroutineScope.receiveMessage(
  address: String,
  reqClass: Class<Req>,
  handler: suspend (req: Req) -> Any?
) {
  MessageEx.logger.debug("register message handler for address: {}", address)
  val vertx = VertxUtil.vertx()
  vertx.eventBus().consumer<JsonObject>(address) {
    if (MessageEx.logger.isDebugEnabled) {
      MessageEx.logger.debug("received message at address: {}, req: {}", address, it.body())
    }
    handleReq(it, it.body().mapTo(reqClass), handler)
  }
}

fun CoroutineScope.receiveMessage(
  address: String,
  handler: suspend (req: JsonObject) -> Any?
) {
  MessageEx.logger.debug("register message handler for address: {}", address)
  val vertx = VertxUtil.vertx()
  vertx.eventBus().consumer<JsonObject>(address) {
    MessageEx.logger.debug("received message at address: {}, req: {}", address, it.body())
    handleReq(it, it.body(), handler)
  }
}

private fun <Req> CoroutineScope.handleReq(
  message: Message<JsonObject>,
  req: Req,
  handler: suspend (req: Req) -> Any?
) {
  launch {
    try {
      val resBody = handler(req)
      message.reply(JsonObject.mapFrom(SimpleRes<Any>().apply { data = resBody }))
    } catch (e: Throwable) {
      MessageEx.logger.error("message handler error", e)
      message.reply(SimpleRes.fromError(e))
    }
  }
}
