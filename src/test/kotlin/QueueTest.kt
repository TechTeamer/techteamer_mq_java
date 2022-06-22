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
    val clientManager = QueueManager(testhelper.testConfig)
    val serverManager = QueueManager(testhelper.testConfig)
    val queueName = "techteamer-mq-java-test-queue"
    val queueServer = serverManager.getQueueServer(queueName, MyTestQueueServer::class.java)
    val queueClient = clientManager.getQueueClient(queueName)

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

    @Test
    fun testQueueMessageTimeOutAndMaxRetry() = runBlocking {
        val name = "timeOutTest"

        serverManager.getQueueServer(
            name,
            MyTestQueueServerTimeOut::class.java,
            options = object : ConnectionOptions {
                override val timeOutMs: Int
                    get() = 200
                override val maxRetry: Int?
                    get() = 3
            }) as MyTestQueueServerTimeOut

        val client = clientManager.getQueueClient(name)

        serverManager.connect()
        clientManager.connect()

        client.sendAction(
            "action",
            mutableMapOf("testData" to "test"),
            attachments = mutableMapOf("testAttachment" to "helloTest".toByteArray())
        )

        assertTrue {
            delay(1300)
            // because of timeouts and retries we will try to send the message 4 times (it means that we Retried it 3 times)
            return@assertTrue errorCounter == 3
        }
    }
}

class MyTestQueueServer(
    override val queueConnection: QueueConnection,
    override var logger: Logger,
    override val name: String,
    override val options: ConnectionOptions
) : QueueServer(queueConnection, logger, name, options) {
    override suspend fun callback(
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

class MyTestQueueServerTimeOut(
    override val queueConnection: QueueConnection,
    override var logger: Logger,
    override val name: String,
    override val options: ConnectionOptions
) : QueueServer(queueConnection, logger, name, options) {

    override suspend fun callback(
        data: MutableMap<String, Any?>,
        props: BasicProperties,
        request: QueueMessage,
        delivery: Delivery
    ): Any? {
        errorCounter++
        delay(1000)

        return null
    }
}

var errorCounter = 0

