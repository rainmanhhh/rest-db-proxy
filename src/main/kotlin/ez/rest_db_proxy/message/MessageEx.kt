package ez.rest_db_proxy.message

import ez.rest_db_proxy.VertxUtil
import ez.rest_db_proxy.err.HttpException
import ez.rest_db_proxy.message.res.SimpleRes
import io.vertx.core.eventbus.Message
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
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
fun <Req> receiveMessage(
  address: String,
  reqClass: Class<Req>,
  handler: (req: Req, message: Message<JsonObject>) -> Unit
) {
  MessageEx.logger.debug("register message handler for address: {}", address)
  val vertx = VertxUtil.vertx()
//  coroutineScope {
  vertx.eventBus().consumer<JsonObject>(address) {
    MessageEx.logger.debug("received message at address: {}, req: {}", address, it.body())
    handleReq(it, it.body().mapTo(reqClass), handler)
  }
//  }
}

fun receiveMessage(
  address: String,
  handler: (req: JsonObject, message: Message<JsonObject>) -> Unit
) {
  MessageEx.logger.debug("register message handler for address: {}", address)
  val vertx = VertxUtil.vertx()
//  coroutineScope {
  vertx.eventBus().consumer<JsonObject>(address) {
    MessageEx.logger.debug("received message at address: {}, req: {}", address, it.body())
    handleReq(it, it.body(), handler)
  }
//  }
}

private fun <Req> handleReq(
  message: Message<JsonObject>,
  req: Req,
  handler: (req: Req, message: Message<JsonObject>) -> Unit
) {
//  launch {
  try {
    handler(req, message)
  } catch (e: Throwable) {
    if (e !is HttpException) MessageEx.logger.error("message handler error", e)
    message.reply(SimpleRes<Any>(e))
  }
//  }
}
