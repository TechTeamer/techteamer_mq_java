import com.facekom.mq_kotlin.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.test.assertTrue

class PubSubActionTest {
    private val testhelper = TestHelper()
    private val pubManager = QueueManager(testhelper.testConfig)
    private val subManager = QueueManager(testhelper.testConfig)
    private val publisherName = "techteamer-mq-java-test-publisher-action"

    var subscriber: Subscriber
    var publisher: Publisher

    init {
        pubManager.setLogger(testhelper.logger)
        publisher = pubManager.getPublisher(publisherName)
        subscriber = subManager.getSubscriber(publisherName)

        pubManager.connect()
        subManager.connect()
    }

    private var testResult: QueueMessage? = null
    @Test
    fun testSubscriberRegisterAction() = runBlocking {

        subscriber.registerAction("testAction") { _, _, _, request, _ ->
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
