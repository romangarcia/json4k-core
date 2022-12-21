package json4k

import com.fasterxml.jackson.core.JsonParser.NumberType
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.jr.ob.JSON
import com.fasterxml.jackson.jr.stree.JacksonJrsTreeCodec
import com.fasterxml.jackson.jr.stree.JrsBoolean
import com.fasterxml.jackson.jr.stree.JrsNumber
import com.fasterxml.jackson.jr.stree.JrsString
import io.vertx.core.json.Json
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import java.io.InputStream
import java.io.InputStreamReader
import java.io.StringReader

sealed interface JsValue {

    fun asJsObject(): JsObject? = to()

    fun asJsArray(): JsArray? = to()
}

inline fun <reified T : JsValue> JsValue.to(): T? {
    return when(this) {
        is T -> this
        else -> null
    }
}

data class JsString(val value: String): JsValue
data class JsNumeric(val value: Number): JsValue
data class JsBool(val value: Boolean): JsValue
data class JsObject(val fields: List<JsField>): JsValue {
    fun get(f: String): JsValue? = fields.find { it.first == f }?.second
}
data class JsArray(val values: List<JsValue>): JsValue {
    fun get(i: Int): JsValue = values[i]
}
object JsNull: JsValue
object JsNothing: JsValue

typealias JsField = Pair<String, JsValue>

interface JsonParser {
    fun parse(input: InputStream): JsValue
}

class VertxParser : JsonParser {

    val codec = Json.load().codec()

    override fun parse(input: InputStream): JsValue {
        return input.use {
            val jsString = InputStreamReader(it).readText()
            val value = codec.fromString(jsString, Any::class.java)
            decompose(value)
        }
    }

    private fun decompose(value: Any?): JsValue {
        return when(value) {
            null -> JsNull
            is String -> JsString(value)
            is Number -> JsNumeric(value)
            is Boolean -> JsBool(value)
            is JsonArray -> JsArray(value.map { decompose(it) })
            is JsonObject -> JsObject(value.map { entry ->
                entry.key.toString() to decompose(entry.value)
            })
            else -> JsNull
        }
    }

}


class JacksonParser : JsonParser {

    private val parser: JSON = JSON.builder().treeCodec(JacksonJrsTreeCodec()).build()

    override fun parse(input: InputStream): JsValue {
        val node = parser.treeFrom<TreeNode>(input)
        return decompose(node)
    }

    private fun decompose(node: TreeNode?): JsValue {
        return when {
            node == null -> JsNothing
            node.isArray -> {
                val values = (0 until node.size()).map { i ->
                    decompose(node.get(i))
                }
                JsArray(values)
            }
            node.isMissingNode -> JsNothing
            node.isValueNode -> {
                val t = node.asToken()
                return when(node) {
                    is JrsBoolean -> JsBool(t.asString().toBoolean())
                    is JrsString -> JsString(node.value)
                    is JrsNumber -> return when(node.numberType()) {
                        NumberType.INT, NumberType.LONG -> JsNumeric(node.value.toLong())
                        NumberType.FLOAT, NumberType.DOUBLE -> JsNumeric(node.value.toDouble())
                        else -> JsNumeric(node.asBigDecimal())
                    }
                    else -> JsNull
                }
            }
            node.isObject -> {
                val fields = node.fieldNames().asSequence().map { field ->
                    JsField(field, decompose(node.get(field)))
                }
                JsObject(fields.toList())
            }
            else -> throw IllegalStateException("Unexpected Json node $node")
        }
    }

}