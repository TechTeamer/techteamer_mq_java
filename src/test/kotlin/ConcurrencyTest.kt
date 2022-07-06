import com.facekom.mq_kotlin.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConcurrencyTest {
    private val testhelper = TestHelper()
    private val queueManager = QueueManager(testhelper.testConfig)
    private val queueManagerTwo = QueueManager(testhelper.testConfig)
    val rpcName = "techteamer-mq-java-test-rpc-concurrency"
    val rpcClientOptions = RpcClientOptions()
    val rpcServerOptions = RpcServerOptions()
    val publisherOptions = PublisherOptions()
    val subscriberOptions = SubscriberOptions()

    val queueName = "techteamer-mq-java-test-queue-concurrency"
    val queueClientOptions = QueueClientOptions()
    val queueServerOptions = QueueServerOptions()
    val connectionOptions = ConnectionOptions()

    val exchangeName = "techteamer-mq-java-test-pubsub-concurrency"
    private var rpcServer: RPCServer
    private var rpcClient: RPCClient
    private var rpcClientTwo: RPCClient
    private var queueServer: QueueServer
    private var queueClient: QueueClient
    private var subscriber: Subscriber
    private var subscriberTwo: Subscriber
    private var publisher: Publisher

    init {
        rpcClientOptions.prefetchCount = 3
        rpcClientOptions.queueMaxSize = 1
        rpcClientOptions.timeOutMs = 15000
        rpcClient = queueManager.getRPCClient(rpcName, rpcClientOptions)
        rpcClientTwo = queueManagerTwo.getRPCClient(rpcName, rpcClientOptions)

        rpcServerOptions.prefetchCount = 3
        rpcServerOptions.timeOutMs = 10000
        rpcServerOptions.queue.durable = false
        rpcServerOptions.queue.exclusive = true
        rpcServer = queueManager.getRPCServer(rpcName, rpcServerOptions)

        connectionOptions.prefetchCount = 2
        queueServerOptions.connection = connectionOptions
        queueServerOptions.queue.exclusive = true

        queueClientOptions.queue.assert = false
        queueClient = queueManager.getQueueClient(queueName, queueClientOptions)
        queueServer = queueManager.getQueueServer(queueName, queueServerOptions)

        publisherOptions.exchange.durable = false
        publisherOptions.exchange.autoDelete = true
        publisher = queueManager.getPublisher(exchangeName, publisherOptions)

        subscriberOptions.exchange.durable = false
        subscriberOptions.exchange.autoDelete = true
        subscriber = queueManager.getSubscriber(exchangeName, subscriberOptions)
        subscriberTwo = queueManagerTwo.getSubscriber(exchangeName, subscriberOptions)

        queueManager.connect()
        queueManagerTwo.connect()
    }

    @Test
    fun rpcServerNonBlockingTest() = runBlocking {
        assertTrue {
            rpcServer.registerAction("testAction") { data, request, response, delivery ->
                return@registerAction null
            }

            rpcServer.registerAction("testAction2") { data, request, response, delivery ->
                runBlocking {
                    Thread.sleep(1000)
                    return@runBlocking null
                }
            }

            var slowAnswer: QueueMessage? = null
            var call2Started = false
            CoroutineScope(Dispatchers.IO).launch {
                call2Started = true
                slowAnswer = rpcClientTwo.callSimpleAction("testAction2", "testData", null, null)
            }

            Thread.sleep(300)

            assertTrue {
                call2Started
            }

            val fastAnswer: QueueMessage? = rpcClient.callSimpleAction("testAction", "testData", null, null)

            val fastAnswerFinished = fastAnswer != null && slowAnswer == null
            delay(1500)
            val bothAnswerFinished = fastAnswer != null && slowAnswer != null

            return@assertTrue fastAnswerFinished && bothAnswerFinished
        }
    }

    @Test
    fun queueServerNonBlockingTest() = runBlocking {
        assertTrue {
            var messageFast: QueueMessage? = null
            var messageSlow: QueueMessage? = null
            queueServer.registerAction("testAction") { _, _, request, _ ->
                messageFast = request
                return@registerAction
            }

            queueServer.registerAction("testAction2") { _, _, request, _ ->
                Thread.sleep(1000)
                messageSlow = request
                return@registerAction
            }

            CoroutineScope(Dispatchers.IO).launch {
                queueClient.sendSimpleAction("testAction2", "testData", null, null)
            }

            Thread.sleep(300)

            queueClient.sendSimpleAction("testAction", "testData", null, null)


            delay(200)
            val fastAnswerFinished = messageFast != null && messageSlow == null
            delay(5000)
            val bothAnswerFinished = messageFast != null && messageSlow != null

            return@assertTrue fastAnswerFinished && bothAnswerFinished
        }
    }

    @Test
    fun pubSubNonBlockingTest() = runBlocking {
        assertTrue {
            var messageFast: QueueMessage? = null
            var messageSlow: QueueMessage? = null

            subscriberTwo.registerAction("testAction") { _, _, request, _ ->
                Thread.sleep(1000)
                messageSlow = request
                return@registerAction
            }

            subscriber.registerAction("testAction") { _, _, request, _ ->
                messageFast = request
                return@registerAction
            }

            Thread.sleep(300)

            publisher.sendSimpleAction("testAction", "testData", null, null)

            delay(200)
            val fastAnswerFinished = messageFast != null && messageSlow == null
            delay(5000)
            val bothAnswerFinished = messageFast != null && messageSlow != null

            return@assertTrue fastAnswerFinished && bothAnswerFinished
        }
    }
}
