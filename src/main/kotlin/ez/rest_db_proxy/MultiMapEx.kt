package ez.rest_db_proxy

import io.vertx.core.MultiMap
import io.vertx.core.json.JsonObject

/**
 * all param values will be treated as string, so the result is a `Map<String, String>` json object
 */
fun MultiMap.toJson() = JsonObject().also {
  for (entry in entries()) {
    if (!entry.value.isNullOrBlank()) {
      it.put(entry.key, entry.value)
    }
  }
}
