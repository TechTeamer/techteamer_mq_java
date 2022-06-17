import com.facekom.mq_kotlin.*
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

class QueueTest {
    private val testhelper = TestHelper()
    val queueName = "test-queue"
    val clientManager = QueueManager(testhelper.testConfig)
    val serverManager = QueueManager(testhelper.testConfig)



    val queueServer =
        serverManager.getQueueServer(queueName, MyTestQueueServer::class.java) as MyTestQueueServer

    val queueClient = clientManager.getQueueClient(queueName) as QueueClient

    init {
        clientManager.connect()
        serverManager.connect()
    }

    @Test
    fun testQueueMessage() = runBlocking {
        queueClient.sendAction(
            "action",
            mutableMapOf("testData" to "test"),
            attachments = mutableMapOf("testAttachment" to "helloTest".toByteArray())
        )

        delay(200)
    }


}

class MyTestQueueServer(
    override val queueConnection: QueueConnection,
    override var logger: Logger,
    override val name: String,
    override val options: ConnectionOptions
) : QueueServer(queueConnection, logger, name, options) {
    override fun callback(
        data: MutableMap<String, Any?>,
        props: BasicProperties,
        request: QueueMessage,
        delivery: Delivery
    ): Any? {
        assertTrue {
            return@assertTrue data["action"] == "action" &&
                    request.attachments["testAttachment"]?.let { String(it) } == "helloTest"
        }

        return null
    }
}
