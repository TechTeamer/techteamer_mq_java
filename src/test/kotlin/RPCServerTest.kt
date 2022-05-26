import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import io.mockk.impl.annotations.MockK
import org.junit.Test
import org.slf4j.Logger
import kotlin.test.assertEquals

internal class RPCServerTest {
    @MockK
    lateinit var channel: Channel

    @MockK
    lateinit var logger: Logger

    private val testInstance: RPCServer = RPCServer(channel, "test", logger)

    @Test
    private fun testRegisterAction() {
        fun handler (
            instance: Any,
            msg: MutableMap<String, Any?>,
            delivery: Delivery,
            response: QueueResponse,
            data: QueueMessage
        ) : MutableMap<String, Any?> {
            return mutableMapOf("test" to "test")
        }

        testInstance.registerAction("testAction", ::handler)
        assertEquals(true, testInstance.actions.contains("testAction"))
    }

}