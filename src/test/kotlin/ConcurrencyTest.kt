import com.facekom.mq_kotlin.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Date
import kotlin.math.abs
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
            var slowCallStarted: Date? = null
            var slowCallFinished: Date? = null

            var fastCallStarted: Date? = null
            val fastCallFinished: Date?
            var fastCallFinishedTwo: Date? = null

            rpcServer.registerAction("testAction") { data, request, response, delivery ->
                fastCallStarted = Date()
                return@registerAction null
            }

            rpcServer.registerAction("testAction2") { data, request, response, delivery ->
                runBlocking {
                    slowCallStarted = Date()
                    Thread.sleep(1000)
                    rpcClient.callSimpleAction("testAction", "testData", null, null)
                    fastCallFinishedTwo = Date()
                    return@runBlocking null
                }
            }

            println("1: $fastCallStarted")
            CoroutineScope(Dispatchers.IO).launch {
                rpcClientTwo.callSimpleAction("testAction2", "testData", null, null)
                slowCallFinished = Date()
            }

            Thread.sleep(300)

            rpcClient.callSimpleAction("testAction", "testData", null, null)
            println("2: $fastCallStarted")
            fastCallFinished = Date()

            delay(1500)
            println("3: $fastCallStarted")
            return@assertTrue slowCallStarted!! < fastCallStarted &&
                    slowCallFinished!! > fastCallFinished &&
                    fastCallFinishedTwo!! < slowCallFinished
        }
    }

    @Test
    fun queueServerNonBlockingTest() = runBlocking {
        assertTrue {
            var fastCallStarted: Date? = null
            var slowCallStarted: Date? = null

            var fastCallFinished: Date? = null
            var slowCallFinished: Date? = null

            queueServer.registerAction("testAction") { _, _, request, _ ->
                fastCallStarted = Date()
                fastCallFinished = Date()
                return@registerAction
            }

            queueServer.registerAction("testAction2") { _, _, request, _ ->
                slowCallStarted = Date()
                Thread.sleep(1000)
                slowCallFinished = Date()
                return@registerAction
            }

            CoroutineScope(Dispatchers.IO).launch {
                queueClient.sendSimpleAction("testAction2", "testData", null, null)
            }

            Thread.sleep(300)

            queueClient.sendSimpleAction("testAction", "testData", null, null)

            delay(1500)
            return@assertTrue slowCallStarted!! < fastCallStarted && slowCallFinished!! > fastCallFinished
        }
    }

    @Test
    fun pubSubNonBlockingTest() = runBlocking {
        assertTrue {
            var callOneStarted: Date? = null
            var callOneFinished: Date? = null
            var callTwoStarted: Date? = null
            var callTwoFinished: Date? = null

            delay(300)

            subscriberTwo.registerAction("testAction") { _, _, request, _ ->
                println("CALLEDTWO")
                callOneStarted = Date()
                callOneFinished = Date()
                return@registerAction
            }

            subscriber.registerAction("testAction") { _, _, request, _ ->
                println("CALLEDONE")
                callTwoStarted = Date()
                Thread.sleep(100)
                callTwoFinished = Date()
                return@registerAction
            }


            publisher.sendSimpleAction("testAction", "testData", null, null)

            delay(200)

            println(subscriberTwo.actions)
            println(callOneFinished)
            println(callTwoFinished)
            println(callOneStarted)
            println(callTwoStarted)

            val diff = abs(callOneStarted?.time!! - callTwoStarted?.time!!)
            println(diff)

            return@assertTrue diff < 100 && callOneFinished!! < callTwoFinished
        }
    }
}
