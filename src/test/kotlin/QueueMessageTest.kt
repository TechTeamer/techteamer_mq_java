import com.facekom.mq_kotlin.QueueMessage
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueMessageTest {
    val testHelper = TestHelper()

    // conversion tests

    @Test
    fun testConvertStringData () {
        val queueMessage = testHelper.messageOK(testHelper.stringData)
        val serialized = queueMessage.serialize()
        val converted = QueueMessage.unserialize(serialized)
        val error = testHelper.checkData(converted.data, testHelper.stringData)
        assertTrue ("Converted string message data should decode fine: $error") { error == null }
    }

    @Test
    fun testConvertNumberData () {
        val queueMessage = testHelper.messageOK(testHelper.numberData)
        val serialized = queueMessage.serialize()
        val converted = QueueMessage.unserialize(serialized)
        val error = testHelper.checkData(converted.data, testHelper.numberData)
        assertTrue ("Converted number message data should decode fine: $error") { error == null }
    }

    @Test
    fun testConvertBooleanData () {
        val queueMessage = testHelper.messageOK(testHelper.booleanData)
        val serialized = queueMessage.serialize()
        val converted = QueueMessage.unserialize(serialized)
        val error = testHelper.checkData(converted.data, testHelper.booleanData)
        assertTrue ("Converted boolean message data should decode fine: $error") { error == null }
    }

    @Test
    fun testConvertNullData () {
        val queueMessage = testHelper.messageOK(testHelper.nullData)
        val serialized = queueMessage.serialize()
        val converted = QueueMessage.unserialize(serialized)
        val error = testHelper.checkData(converted.data, testHelper.nullData)
        assertTrue ("Converted null message data should decode fine: $error") { error == null }
    }

    @Test
    fun testConvertArrayData () {
        val queueMessage = testHelper.messageOK(testHelper.arrayData)
        val serialized = queueMessage.serialize()
        val converted = QueueMessage.unserialize(serialized)
        val error = testHelper.checkData(converted.data, testHelper.arrayData)
        assertTrue ("Converted array message data should decode fine: $error") { error == null }
    }

    @Test
    fun testConvertObjectData () {
        val queueMessage = testHelper.messageOK(testHelper.objectData)
        val serialized = queueMessage.serialize()
        val converted = QueueMessage.unserialize(serialized)
        val error = testHelper.checkData(converted.data, testHelper.objectData)
        assertTrue ("Converted object message data should decode fine: $error") { error == null }
    }

    // compatibility tests

    @Test
    fun testCompatStringData () {
        val queueMessage = QueueMessage.unserialize(testHelper.compatStringMessage)
        val error = testHelper.checkData(queueMessage.data, testHelper.stringData)
        assertTrue ("Compatibility string message data should match: $error") { error == null }
    }

    // message decoding tests

    @Test
    fun testDecodeInvalidJsonEncoding() {
        val result = QueueMessage.fromJson("not even a json")
        assertTrue ("Message should not decode if the input is not even valid json") { result.status == "error" }
    }

    @Test
    fun testDecodeMissingMessageStatus() {
        val result = QueueMessage.fromJson("{}")
        assertTrue ("Message should not decode if at least the status is not present") { result.status == "error" }
    }

    @Test
    fun testDecodeInvalidMessageStatus() {
        val result = QueueMessage.fromJson("{\"statusNOPE\": 10}")
        assertTrue ("Message should not decode if status is missing or invalid") { result.status == "error" }
    }

    @Test
    fun testDecodeInvalidMessageTimeout() {
        val result = QueueMessage.fromJson("{\"status\": \"ok\",\"timeOut\": []}")
        assertTrue ("Message should not decode if timeout is invalid") { result.status == "error" }
    }

    // json decoding

    @Test
    fun testJsonStringMessageDecoding() {
        val result = QueueMessage.fromJson(testHelper.jsonStringMessage)
        val error = testHelper.checkData(result.data, testHelper.stringData)
        assertTrue ("Json string message data should decode fine: $error") { error == null }
    }

    @Test
    fun testJsonNumberMessageDecoding() {
        val result = QueueMessage.fromJson(testHelper.jsonNumberMessage)
        val error = testHelper.checkData(result.data, testHelper.numberData)
        assertTrue ("Json number message data should decode fine: $error") { error == null }
    }

    @Test
    fun testJsonBooleanMessageDecoding() {
        val result = QueueMessage.fromJson(testHelper.jsonBooleanMessage)
        val error = testHelper.checkData(result.data, testHelper.booleanData)
        assertTrue ("Json boolean message data should decode fine: $error") { error == null }
    }

    @Test
    fun testJsonNullMessageDecoding() {
        val result = QueueMessage.fromJson(testHelper.jsonNullMessage)
        val error = testHelper.checkData(result.data, testHelper.nullData)
        assertTrue ("Json null message data should decode fine: $error") { error == null }
    }

    @Test
    fun testJsonArrayMessageDecoding() {
        val result = QueueMessage.fromJson(testHelper.jsonArrayMessage)
        val error = testHelper.checkData(result.data, testHelper.arrayData)
        assertTrue ("Json array message data should decode fine: $error") { error == null }
    }

    @Test
    fun testJsonObjectMessageDecoding() {
        val result = QueueMessage.fromJson(testHelper.jsonObjectMessage)
        val error = testHelper.checkData(result.data, testHelper.objectData)
        assertTrue ("Json object message data should decode fine: $error") { error == null }
    }

    // attachments

    @Test
    fun testAttachmentConversion() {
        val queueMessage = testHelper.messageOK(testHelper.nullData)
        queueMessage.addAttachment("test", testHelper.attachment)
        val serialized = queueMessage.serialize()
        val converted = QueueMessage.unserialize(serialized)

        assertTrue ("Conversion should retain attachment count") { converted.attachments.size == 1 }
        assertTrue ("Conversion should retain attachment name") { converted.attachments.contains("test") }
        val convertedAttachment = converted.attachments.get("test")!!
        val sentAttachmentContent = String(testHelper.attachment)
        val convertedAttachmentContent = String(convertedAttachment)
        assertTrue ("Conversion should retain attachment content: $sentAttachmentContent != $convertedAttachment") {
            sentAttachmentContent == convertedAttachmentContent
        }
    }

    // backwards compatibility

    @Test
    fun testBackwardsCompatibility() {
        val message = "{\"status\": \"ok\", \"data\": {\"test\": 1}}".toByteArray()
        val result = QueueMessage.unserialize(message)

        println(result.data?.asJsonObject?.get("test"))

        val testValue = result.data?.asJsonObject?.get("test")?.asInt
        assertTrue {
            result.status == "ok" && testValue == 1
        }
    }
}
