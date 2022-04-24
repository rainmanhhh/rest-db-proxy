package ez.rest_db_proxy

import io.vertx.core.json.JsonObject
import io.vertx.ext.web.RoutingContext

/**
 * Merge queryParams and body into a json object. All params in query or form will be treated as strings
 */
fun RoutingContext.paramsAsJson(): JsonObject {
  val params = queryParams().toJson()
  val bodyJson = when (parsedHeaders().contentType()?.subComponent()) {
    "json" -> bodyAsJson
    "x-www-form-urlencoded" -> request().formAttributes().toJson()
    else -> null
  }
  bodyJson?.let { params.mergeIn(it) }
  return params
}
