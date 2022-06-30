import com.facekom.mq_kotlin.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueTest {
    private val testhelper = TestHelper()
    val clientManager = QueueManager(testhelper.testConfig)
    val serverManager = QueueManager(testhelper.testConfig)
    val queueName = "techteamer-mq-java-test-queue"
    val maxRetry = 3
    val timeoutMs = 5000
    val queueClientOptions = QueueClientOptions()
    val queueServerOptions = QueueServerOptions()

    val queueClient: QueueClient
    val queueServer: QueueServer

    init {
        testhelper.logger.debug("NEW QUEUE TEST")
        queueClientOptions.queue.assert = false // skip queue assertion for client, b/c the server initiates it exclusively
        queueClient = clientManager.getQueueClient(queueName, queueClientOptions)

        queueServerOptions.queue.durable = false
        queueServerOptions.queue.exclusive = true
        queueServerOptions.connection.maxRetry = maxRetry
        queueServerOptions.connection.timeOutMs = timeoutMs
        queueServer = serverManager.getQueueServer(queueName, queueServerOptions)

        runBlocking {
            testhelper.logger.debug("CONNECT clientManager")
            clientManager.connect()
            testhelper.logger.debug("CONNECT serverManager")
            serverManager.connect()
            testhelper.logger.debug("CONNECT DONE")
        }
    }

    @Test
    fun testConsumeCallback() = runBlocking {
        val oldCallback = queueServer._callback
        val newCallback : QueueHandler = { data, props, request, delivery -> run {}}
        queueServer.consume(newCallback)

        assertTrue ("Consume call should set new message handler callback") {
            oldCallback !== queueServer._callback && newCallback == queueServer._callback
        }
    }

    @Test
    fun testQueueStringMessage() = runBlocking {
        var testMessageReceived = false
        var testMessageValid = false

        queueServer.consume { data, props, request, delivery -> run {
            testMessageReceived = true
            if (data != null && !data.isJsonPrimitive) return@consume
            val dataString = data?.asString
            testMessageValid = dataString == "testData"
        }}

        queueClient.send("testData")

        delay(timeoutMs.toLong()) // allow time for network

        assertTrue ("Message should have arrived") {
            testMessageReceived
        }
        assertTrue ("Message should be valid") {
            testMessageValid
        }
    }

    @Test
    fun testQueueMessageMaxRetry() = runBlocking {
        var consumeCalled = 0
        queueServer.consume { data, props, request, delivery -> run {
            consumeCalled++
            throw Exception("message not processed well")
        }}

        queueClient.send("")

        delay(timeoutMs.toLong()) // allow time for network

        assertTrue ("Call count ($consumeCalled) differs from max retry count ($maxRetry)") {
            consumeCalled == maxRetry + 1
        }
    }

    @Test
    fun testQueueMessageTimeOut() = runBlocking {
        queueServer.consume { data, props, request, delivery -> runBlocking {
                queueServer.logger.debug("GOT")
                delay((timeoutMs + 500).toLong())
                queueServer.logger.debug("LOST")
            }
        }

        queueClient.send("")
        queueServer.logger.debug("SENT")
        delay((timeoutMs + 1000).toLong()) // allow time for network
        queueServer.logger.debug("WAITED")

//        assertTrue ("Call count ($consumeCalled) differs from max retry count ($maxRetry)") {
//            consumeCalled == maxRetry + 1
//        }
    }
}
