package json4k

import kotlin.test.Test
import kotlin.test.assertEquals

class VertxTest {

    @Test
    fun `vertx parser should process medium json`() {
        val json = """
            {
              "resources": {
                "apiRest": {
                  "handlers": [
                    {
                      "name": "jutsuFunction",
                      "codeUri": "api",
                      "handler": "wabi.main.jutsu",
                      "routes": [
                        {
                          "method": "post",
                          "path": "/jutsu"
                        }
                      ],
                      "concurrency": {
                        "qa": {
                          "minDesired": 1,
                          "maxDesired": 5,
                          "autoScalingOn": 0.5
                        },
                        "prod": {
                          "minDesired": 1,
                          "maxDesired": 5,
                          "autoScalingOn": 0.5
                        }
                      }
                    }
                  ]
                }
              }
            }
        """.trimIndent()

        val tree = VertxParser().parse(json.byteInputStream())
        val firstHandler = tree.asJsObject()?.get("resources")?.asJsObject()?.get("apiRest")?.asJsObject()?.get("handlers")?.asJsArray()?.get(0)
        assertEquals(JsString("jutsuFunction"), firstHandler?.asJsObject()?.get("name"))
    }
}