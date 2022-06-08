import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.slf4j.Logger
import kotlin.test.assertTrue

private var sendStringResult: QueueMessage? = null
private var sendStringResultTwo: QueueMessage? = null

class PubSubTest {
    private val testhelper = TestHelper()

    private val pubManager = QueueManager(testhelper.testConfig)
    private val publisherName = "test-publisher"

    private val subManager = QueueManager(testhelper.testConfig)

    var publisher: MyTestPublisher
    var subscriber: MyTestSubscriber

    init {
        pubManager.setLogger(testhelper.logger)
        publisher = pubManager.getPublisher(publisherName, MyTestPublisher::class.java) as MyTestPublisher
        subscriber = subManager.getSubscriber(publisherName, MyTestSubscriber::class.java, object : ConnectionOptions {
            override val maxRetry = 1
            override val timeOutMs = 5000
            override val prefetchCount = 1
        }) as MyTestSubscriber

        pubManager.connect()
        subManager.connect()
    }

    @Test
    fun sendWithAttachment() = runBlocking {
        delay(200)
        publisher.sendIt("testMessage")
        delay(200)
        assertTrue {
            val testData = sendStringResult?.data?.get("data") as Map<*, *>?
            return@assertTrue testData?.get("testData") == "testMessage" &&
                    sendStringResult?.attachments?.get("otherTest")
                        ?.let { String(it) } == "testHello" &&
                    sendStringResult?.data?.get("action") == "send"
        }
    }

    @Test
    fun sendToMoreSubscribers() = runBlocking {
        val otherSubManager = QueueManager(testhelper.testConfig)

        otherSubManager.getSubscriber(publisherName, MyTestSubscriberTwo::class.java, object : ConnectionOptions {
            override val maxRetry = 1
            override val timeOutMs = 5000
            override val prefetchCount = 1
        }) as MyTestSubscriberTwo

        otherSubManager.connect()
        delay(200)

        publisher.sendIt("testNewMessage")
        delay(200)

        assertTrue {
            val testData = sendStringResult?.data?.get("data") as Map<*, *>?
            val testDataTwo = sendStringResultTwo?.data?.get("data") as Map<*, *>?

            return@assertTrue testData?.get("testData") == "testNewMessage" &&
                    sendStringResult?.attachments?.get("otherTest")
                        ?.let { String(it) } == "testHello" &&
                    sendStringResult?.data?.get("action") == "send" &&
                    testDataTwo?.get("testData") == "testNewMessage" &&
                    sendStringResultTwo?.attachments?.get("otherTest")
                        ?.let { String(it) } == "testHello" &&
                    sendStringResultTwo?.data?.get("action") == "send"
        }
    }


    class MyTestPublisher(
        override val queueConnection: QueueConnection, override val logger: Logger, override val exchange: String
    ) : Publisher(queueConnection, logger, exchange) {
        fun sendIt(msg: String) {
            sendAction(
                "send", mutableMapOf("testData" to msg), attachments = mutableMapOf(
                    "publisherTestAttachment" to "hello".toByteArray(), "otherTest" to "testHello".toByteArray()
                )
            )
        }
    }

    class MyTestSubscriber(
        override var connection: QueueConnection,
        override var logger: Logger,
        override val name: String,
        override val options: ConnectionOptions
    ) : Subscriber(connection, logger, name, options) {
        override fun callback(
            data: MutableMap<String, Any?>, props: BasicProperties, request: QueueMessage, delivery: Delivery
        ): Any? {
            sendStringResult = request

            return null
        }
    }

    class MyTestSubscriberTwo(
        override var connection: QueueConnection,
        override var logger: Logger,
        override val name: String,
        override val options: ConnectionOptions
    ) : Subscriber(connection, logger, name, options) {
        override fun callback(
            data: MutableMap<String, Any?>, props: BasicProperties, request: QueueMessage, delivery: Delivery
        ): Any? {
            sendStringResultTwo = request

            return null
        }
    }

}

