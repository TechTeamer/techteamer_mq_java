import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertTrue

class PubSubActionTest {
    private val testhelper = TestHelper()

    private val pubManager = QueueManager(testhelper.testConfig)
    private val publisherName = "test-publisher-action"

    private val subManager = QueueManager(testhelper.testConfig)
    var subscriber: Subscriber
    var publisher: Publisher

    init {
        pubManager.setLogger(testhelper.logger)
        subscriber = subManager.getSubscriber(publisherName, options = object : ConnectionOptions {
            override val maxRetry = 1
            override val timeOutMs = 5000
            override val prefetchCount = 1
        }) as Subscriber

        publisher = pubManager.getPublisher(publisherName) as Publisher

        pubManager.connect()
        subManager.connect()
    }

    private var testResult: QueueMessage? = null
    @Test
    fun testSubscriberRegisterAction() = runBlocking {

        subscriber.registerAction("testAction") { thiss, data, props, request, delivery ->
            testResult = request
            return@registerAction testResult!!
        }

        delay(200)
        publisher.sendAction("testAction", mutableMapOf("test" to "data"))

        delay(200)

        assertTrue {
            return@assertTrue testResult?.data?.get("action") == "testAction"
        }
    }
}