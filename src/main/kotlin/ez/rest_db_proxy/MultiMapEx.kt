package ez.rest_db_proxy

import io.vertx.core.MultiMap
import io.vertx.core.json.Json
import io.vertx.core.json.JsonObject

fun MultiMap.toJson() = JsonObject().also {
  for (entry in entries()) {
    if (!entry.value.isNullOrBlank()) {
      it.put(entry.key, Json.decodeValue(entry.value))
    }
  }
}
