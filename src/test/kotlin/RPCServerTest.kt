import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import io.mockk.MockKAnnotations
import io.mockk.impl.annotations.*
import io.mockk.junit4.MockKRule
import org.junit.*
import org.slf4j.Logger
import kotlin.test.assertEquals


internal class RPCServerTest {

//    private val testInstance: RPCServer = RPCServer(channel, "test", logger)
//    @Test
//    fun testRegisterAction() {
//        fun handler (
//            instance: Any,
//            msg: MutableMap<String, Any?>,
//            delivery: Delivery,
//            response: QueueResponse,
//            data: QueueMessage
//        ) : MutableMap<String, Any?> {
//            return mutableMapOf("test" to "test")
//        }
//
//        testInstance.registerAction("testAction", ::handler)
//        assertEquals(true, testInstance.actions.contains("testAction"))
//    }

}