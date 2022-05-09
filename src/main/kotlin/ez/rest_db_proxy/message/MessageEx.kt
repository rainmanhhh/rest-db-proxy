package ez.rest_db_proxy.message

import ez.rest_db_proxy.VertxUtil
import ez.rest_db_proxy.config.VertxConfig
import ez.rest_db_proxy.message.req.Req
import ez.rest_db_proxy.message.res.SimpleRes
import io.vertx.core.eventbus.DeliveryOptions
import io.vertx.core.eventbus.Message
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject
import io.vertx.kotlin.coroutines.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.slf4j.LoggerFactory

object MessageEx {
  internal val logger = LoggerFactory.getLogger(MessageEx::class.java)
}

/**
 * @param reqBody should be an object which can be mapped to a [JsonObject]
 * @param resClass should be an object class which can be mapped from a [JsonObject]
 */
suspend fun <ReqBody, Res : SimpleRes<*>> sendMessage(
  address: String,
  reqBody: ReqBody,
  resClass: Class<Res>,
  deliveryOptions: DeliveryOptions = DeliveryOptions()
): Res {
  MessageEx.logger.debug(
    "send message to address: {}, reqBody: {}, resClass: {}",
    address,
    reqBody,
    resClass
  )
  val jsonReq = JsonObject.mapFrom(reqBody)
  val minTimeout = VertxConfig.value.minMessageTimeout
  if (deliveryOptions.sendTimeout < minTimeout) deliveryOptions.sendTimeout = minTimeout
  val resBody = VertxUtil.vertx().eventBus().request<JsonObject>(
    address, jsonReq, deliveryOptions
  ).await().body()
  MessageEx.logger.debug("resBody: {}", resBody)
  return resBody.mapTo(resClass)
}

/**
 * @param reqBodyClass should be an object class which can be mapped from a [JsonObject]
 */
fun <ReqBody> CoroutineScope.receiveMessage(
  address: String,
  reqBodyClass: Class<ReqBody>,
  handler: suspend (req: Req<ReqBody>) -> Any?
) {
  MessageEx.logger.debug("register message handler for address: {}", address)
  val vertx = VertxUtil.vertx()
  vertx.eventBus().consumer<JsonObject>(address) {
    if (MessageEx.logger.isDebugEnabled) {
      val httpMethod = it.headers()["httpMethod"]
      MessageEx.logger.debug(
        "received message at address: {}, httpMethod: {}, body: {}",
        address,
        httpMethod,
        Json.encodePrettily(it.body())
      )
    }
    handleReq(it, Req(it.headers(), it.body().mapTo(reqBodyClass)), handler)
  }
}

fun CoroutineScope.receiveMessage(
  address: String,
  handler: suspend (req: Req<JsonObject>) -> Any?
) {
  MessageEx.logger.debug("register message handler for address: {}", address)
  val vertx = VertxUtil.vertx()
  vertx.eventBus().consumer<JsonObject>(address) {
    val method = it.headers().get("method") ?: "get"
    MessageEx.logger.debug("received message at address: {}, method: {}, body: {}", address, method, it.body())
    handleReq(it, Req(it.headers(), it.body()), handler)
  }
}

private fun <ReqBody> CoroutineScope.handleReq(
  message: Message<JsonObject>,
  req: Req<ReqBody>,
  handler: suspend (req: Req<ReqBody>) -> Any?
) {
  launch {
    val res = try {
      val resBody = handler(req)
      SimpleRes<Any>().apply { data = resBody }
    } catch (e: Throwable) {
      MessageEx.logger.error("handle message error! req: {}", req, e)
      SimpleRes.fromError(e)
    }
    try {
      message.reply(JsonObject.mapFrom(res))
    } catch (e: Throwable) {
      MessageEx.logger.error("reply message error", e)
    }
  }
}
