import com.facekom.mq_kotlin.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PubSubTest {
    private val testhelper = TestHelper()
    private val timeoutMs = 5000
    private val pubManager = QueueManager(testhelper.testConfig)
    private val subManager = QueueManager(testhelper.testConfig)
    private val subManager2 = QueueManager(testhelper.testConfig)
    private val publisherName = "techteamer-mq-java-test-publisher"

    var publisher: Publisher
    var subscriber: Subscriber
    var subscriber2: Subscriber

    init {
        val publisherOptions = PublisherOptions()
        publisherOptions.exchange.durable = false
        publisherOptions.exchange.autoDelete = true
        pubManager.setLogger(testhelper.logger)
        publisher = pubManager.getPublisher(publisherName, publisherOptions)

        val subscriberOptions = SubscriberOptions()
        subscriberOptions.connection.timeOutMs = 5000
        subscriberOptions.exchange.durable = false
        subscriberOptions.exchange.autoDelete = true
        subscriber = subManager.getSubscriber(publisherName, subscriberOptions)
        subscriber2 = subManager2.getSubscriber(publisherName, subscriberOptions)

        pubManager.connect()
        subManager.connect()
        subManager2.connect()
    }

    @Test
    fun sendWithAttachment() = runBlocking {
        var testMessageReceived = false
        var testMessageValid = false

        subscriber.consume { data, props, request, delivery -> run {
            testMessageReceived = true
            if (data != null && !data.isJsonPrimitive) return@consume
            val dataString = data?.asString
            testMessageValid = dataString == ("testData")
        }}

        publisher.send("testData", attachments = testhelper.attachmentList)

        delay(timeoutMs.toLong()) // allow time for network

        assertTrue ("Message should have arrived") {
            testMessageReceived
        }
        assertTrue ("Message should be valid") {
            testMessageValid
        }
    }

    @Test
    fun sendToMoreSubscribers() = runBlocking {
        var testMessageReceived = 0
        var testMessageValid = 0
        val commonConsumer : QueueHandler = { data, props, request, delivery -> run {
            testMessageReceived++
            if (data != null && !data.isJsonPrimitive) return@run

            if (data?.asString == ("testData")) {
                testMessageValid++
            }
        }}

        subscriber.consume(commonConsumer)
        subscriber2.consume(commonConsumer)

        publisher.send("testData", attachments = testhelper.attachmentList)

        delay(timeoutMs.toLong()) // allow time for network

        assertTrue ("Both messages should have arrived: $testMessageReceived != 2") {
            testMessageReceived == 2
        }
        assertTrue ("Both message should be valid: $testMessageValid != 2") {
            testMessageValid == 2
        }
    }

    @Test
    fun testSubscriberRegisterAction() = runBlocking {
        var testMessageReceived = false
        var testResult: String? = null

        subscriber.registerAction("testAction") { data, _, request, _ ->
            testMessageReceived = true
            testResult = testhelper.checkData(data, testhelper.stringData)
        }

        publisher.sendSimpleAction("testAction", testhelper.string)

        delay(timeoutMs.toLong())

        assertTrue ("Message should have been processed") {
            testMessageReceived
        }
        assertTrue ("Message data should match") {
            testResult == null
        }
    }
}

