import com.facekom.mq.ConnectionProtocol
import com.facekom.mq.QueueConfig
import com.facekom.mq.QueueMessage
import com.facekom.mq.RabbitMqOptions
import com.google.gson.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.*

class TestHelper {
    val logger: Logger = LoggerFactory.getLogger("brandNewTestLogger")

    var testConfig = QueueConfig()

    val messageOK = { data: JsonElement -> QueueMessage("ok", data) }
    val messageErr = { data: JsonElement -> QueueMessage("error", data) }

    val string = "test"
    val number = 1
    val boolean = true
    val attachment = "helloTest".toByteArray()
    val attachmentList = mutableMapOf<String, ByteArray>("test" to attachment)

    val stringData = JsonPrimitive(string)
    val numberData = JsonPrimitive(number)
    val booleanData = JsonPrimitive(boolean)
    val objectData = JsonObject()
    val arrayData = JsonArray()
    val nullData = JsonNull.INSTANCE

    var compatStringMessage: ByteArray
    var compatNumberMessage: ByteArray
    var compatBooleanMessage: ByteArray
    var compatObjectMessage: ByteArray
    var compatArrayMessage: ByteArray
    var compatNullMessage: ByteArray

    val jsonStringMessage: String = "{\"status\": \"ok\",\"data\":\"test\"}"
    val jsonNumberMessage: String = "{\"status\": \"ok\",\"data\":1}"
    val jsonBooleanMessage: String = "{\"status\": \"ok\",\"data\":true}"
    val jsonObjectMessage: String =
        "{\"status\": \"ok\",\"data\":{\"string\":\"test\",\"number\":1,\"array\":[1,\"testString\"]}}"
    val jsonArrayMessage: String = "{\"status\": \"ok\",\"data\":[1,\"testString\",{\"nested\":\"string\"}]}"
    val jsonNullMessage: String = "{\"status\": \"ok\",\"data\":null}"

    init {
        if (System.getenv("TEST_ENV") == "travis") {
            testConfig.url("amqp://guest:guest@localhost:5672")
        } else {
            testConfig.hostname("rabbitmq_services")
                .port(5671)
                .protocol(ConnectionProtocol.AMQPS)
                .options(
                    RabbitMqOptions()
                        .vhost("pdfservice")
                        .password("pdfservice")
                        .userName("pdfservice")
                        .allowTlsWithoutTrustStore(true)
                        .automaticRecoveryEnabled(true)
                )
        }

        val base64Decoder = Base64.getDecoder()

        // node > new QueueMessage('ok', 'test', 0).serialize().toString('base64')
        compatStringMessage =
            base64Decoder.decode("KwAAADp7InN0YXR1cyI6Im9rIiwiZGF0YSI6InRlc3QiLCJ0aW1lT3V0IjowLCJhdHRhY2hBcnJheSI6W119")
        // node > new QueueMessage('ok', 1, 0).serialize().toString('base64')
        compatNumberMessage =
            base64Decoder.decode("KwAAADV7InN0YXR1cyI6Im9rIiwiZGF0YSI6MSwidGltZU91dCI6MCwiYXR0YWNoQXJyYXkiOltdfQ==")
        // node > new QueueMessage('ok', true, 0).serialize().toString('base64')
        compatBooleanMessage =
            base64Decoder.decode("KwAAADh7InN0YXR1cyI6Im9rIiwiZGF0YSI6dHJ1ZSwidGltZU91dCI6MCwiYXR0YWNoQXJyYXkiOltdfQ==")
        // node > new QueueMessage('ok', { string: 'test', number: 1, array: [1, 'testString']}, 0).serialize().toString('base64')
        compatObjectMessage =
            base64Decoder.decode("KwAAAGl7InN0YXR1cyI6Im9rIiwiZGF0YSI6eyJzdHJpbmciOiJ0ZXN0IiwibnVtYmVyIjoxLCJhcnJheSI6WzEsInRlc3RTdHJpbmciXX0sInRpbWVPdXQiOjAsImF0dGFjaEFycmF5IjpbXX0=")
        // node > new QueueMessage('ok', [1, 'testString', { nested: 'string' }], 0).serialize().toString('base64')
        compatArrayMessage =
            base64Decoder.decode("KwAAAFh7InN0YXR1cyI6Im9rIiwiZGF0YSI6WzEsInRlc3RTdHJpbmciLHsibmVzdGVkIjoic3RyaW5nIn1dLCJ0aW1lT3V0IjowLCJhdHRhY2hBcnJheSI6W119")
        // node > new QueueMessage('ok', null, 0).serialize().toString('base64')
        compatNullMessage =
            base64Decoder.decode("KwAAADh7InN0YXR1cyI6Im9rIiwiZGF0YSI6bnVsbCwidGltZU91dCI6MCwiYXR0YWNoQXJyYXkiOltdfQ==")

        // [1, 'testString', { nested: 'string' }]
        arrayData.add(1)
        arrayData.add("testString")
        val objectForArray = JsonObject()
        objectForArray.addProperty("nested", "string")
        arrayData.add(objectForArray)

        // { string: 'test', number: 1, array: [1, 'testString']}
        objectData.addProperty("string", "test")
        objectData.addProperty("number", 1)
        val arrayForObject = JsonArray()
        arrayForObject.add(1)
        arrayForObject.add("testString")
        objectData.add("array", arrayForObject)
    }

    fun checkData(data: JsonElement?, expected: JsonElement): String? {
        if (expected.isJsonNull && data == null) return null // that's ok
        if (data == null) return "should not be null"

        if (expected.isJsonPrimitive) {
            val primitive = expected.asJsonPrimitive
            if (primitive.isString) {
                if (!data.isJsonPrimitive || !data.asJsonPrimitive.isString) return "not a json string"
                val expectedString = primitive.asString
                if (data.asString != expectedString) return "not equals with expected '$expectedString'"
            } else if (primitive.isNumber) {
                if (!data.isJsonPrimitive || !data.asJsonPrimitive.isNumber) return "not a json number"
                val expectedNumber = primitive.asNumber.toInt()
                if (data.asNumber.toInt() != expectedNumber) return "not equals with expected '$expectedNumber'"
            } else if (primitive.isBoolean) {
                if (!data.isJsonPrimitive || !data.asJsonPrimitive.isBoolean) return "not a json boolean"
                val expectedBoolean = primitive.asBoolean
                if (data.asBoolean != expectedBoolean) return "not equals with expected '$expectedBoolean'"
            }
        } else if (expected.isJsonNull) {
            if (!data.isJsonNull) return "not a json null value"
        } else if (expected.isJsonArray) {
            if (!data.isJsonArray) return "not a json array"
            val dataArray = data.asJsonArray
            val expectedArray = expected.asJsonArray
            expectedArray.forEachIndexed { index, expectedEl ->
                val error = checkData(dataArray[index], expectedEl)
                if (error != null) return error
            }
        } else if (expected.isJsonObject) {
            if (!data.isJsonObject) return "not a json object"
            val dataObj = data.asJsonObject
            val expectedObj = expected.asJsonObject
            expectedObj.keySet().forEach {
                val error = checkData(dataObj.get(it), expectedObj.get(it))
                if (error != null) return error
            }
        }

        return null
    }
}
