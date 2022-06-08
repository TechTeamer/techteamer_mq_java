import org.junit.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class QueueMessageTest {
    val message = QueueMessage("ok", mutableMapOf("test" to "test"))

    @Test
    fun testFromJsonToQueueMessageWithInvalidJson() {
        val result = fromJsonToQueueMessage("invalid")

        assertTrue {
            result.status == "error"
        }
    }

    @Test
    fun testFromJsonToQueueMessageWithInvalidData() {
        val result = fromJsonToQueueMessage("{\"test\": 10}")

        assertTrue {
            result.status == "error" &&
                    result.data?.get("error") == "cannot decode JSON string"
        }
    }

    @Test
    fun testFromJsonToQueueMessageWithValidData() {
        val result = fromJsonToQueueMessage("{\"status\": \"ok\"}")

        assertTrue {
            result.status == "ok"
        }
    }

    @Test
    fun testSerialize() {
        message.addAttachment("test", "helloTest".toByteArray())
        val result = message.serialize()

        assertIs<ByteArray>(result)
    }

    @Test
    fun testUnSerialize() {
        message.addAttachment("test", "helloTest".toByteArray())
        val message = message.serialize()
        val result = unserialize(message)

        assertTrue {
            result.status == "ok" &&
            result.attachments.contains("test")
        }
    }

    @Test
    fun testUnSerializeWithOldFormat() {
        val message = "{\"status\": \"ok\", \"data\": {\"test\": 1}}".toByteArray()
        val result = unserialize(message)

        assertTrue {
            result.status == "ok" &&
            result.data?.get("test") == 1.0
        }
    }
}