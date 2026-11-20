package com.Lia.assistant.action

import org.json.JSONArray
import org.json.JSONObject

/**
 * Builds a Gemini function declaration object.
 *
 * ALL parameter types are declared as STRING — the Live API only supports STRING
 * parameters in function declarations as of the current BidiGenerateContent schema.
 *
 * @param name        Snake_case function name (e.g. "open_app").
 * @param description One sentence the model uses to decide when to call this function.
 * @param params      List of (paramName, paramDescription) pairs.
 * @param required    Names of required parameters (subset of [params] keys).
 */
fun functionDecl(
    name       : String,
    description: String,
    params     : List<Pair<String, String>> = emptyList(),
    required   : List<String>              = emptyList(),
): JSONObject {
    val properties = JSONObject()
    params.forEach { (paramName, paramDesc) ->
        properties.put(
            paramName,
            JSONObject()
                .put("type",        "string")
                .put("description", paramDesc),
        )
    }

    val parametersObj = JSONObject()
        .put("type",       "object")
        .put("properties", properties)

    if (required.isNotEmpty()) {
        parametersObj.put("required", JSONArray(required))
    }

    return JSONObject()
        .put("name",        name)
        .put("description", description)
        .put("parameters",  parametersObj)
}
