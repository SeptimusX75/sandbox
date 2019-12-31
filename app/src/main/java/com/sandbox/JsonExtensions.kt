package com.sandbox

import org.json.JSONObject

fun Map<String, Any>.toJson(): JSONObject = json {
  forEach { (key, value) -> put(key, value) }
}

fun json(jsonAction: JSONObject.() -> Unit): JSONObject = JSONObject().apply(jsonAction)