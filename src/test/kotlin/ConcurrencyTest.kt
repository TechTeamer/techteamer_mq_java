import com.facekom.mq_kotlin.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConcurrencyTest {
    private val testhelper = TestHelper()
    private val queueManager = QueueManager(testhelper.testConfig)
    val rpcName = "techteamer-mq-java-test-rpc-concurrency"
    private var rpcServer: RPCServer
    private var rpcClient: RPCClient

    init {
        val rpcClientOptions = RpcClientOptions()
        rpcClientOptions.prefetchCount = 3
        rpcClientOptions.queueMaxSize = 1
        rpcClientOptions.timeOutMs = 15000
        rpcClient = queueManager.getRPCClient(rpcName, options = rpcClientOptions)

        val rpcServerOptions = RpcServerOptions()
        rpcServerOptions.prefetchCount = 3
        rpcServerOptions.timeOutMs = 10000
        rpcServerOptions.queue.durable = false
        rpcServerOptions.queue.exclusive = true
        rpcServer = queueManager.getRPCServer(rpcName, options = rpcServerOptions)

        queueManager.connect()
    }

    @Test
    fun rpcServerNonBlockingTest() = runBlocking {
        assertTrue {
            val fastAnswer: QueueMessage?
            var slowAnswer: QueueMessage? = null
            val queueName = "techteamer-mq-java-rpc-non-blocking"

            var call2Started = false

            val rpcServerOptions = RpcServerOptions()
            rpcServerOptions.prefetchCount = 3
            rpcServerOptions.timeOutMs = 10000
            rpcServerOptions.queue.durable = false
            rpcServerOptions.queue.exclusive = true
            val server = queueManager.getRPCServer(queueName, rpcServerOptions)

            server.registerAction("testAction") { data, request, response, delivery ->
                return@registerAction null
            }

            server.registerAction("testAction2") { data, request, response, delivery ->
                Thread.sleep(1000)
                return@registerAction null
            }

            val rpcClientOptions = RpcClientOptions()
            rpcClientOptions.prefetchCount = 3
            rpcClientOptions.queueMaxSize = 5
            rpcClientOptions.timeOutMs = 15000
            val client0 = queueManager.getRPCClient(queueName, rpcClientOptions)

            queueManager.connect()

            CoroutineScope(Dispatchers.IO).launch {
                val pool2 = ConnectionPool()

                pool2.setupQueueManagers(mapOf("default" to testhelper.testConfig))

                val queue2 = pool2.defaultConnection!!
                val rpcClientOptions2 = RpcClientOptions()
                rpcClientOptions2.prefetchCount = 3
                rpcClientOptions2.queueMaxSize = 5
                rpcClientOptions2.timeOutMs = 15000
                val client = queue2.getRPCClient(queueName, rpcClientOptions2)
                pool2.connect()
                call2Started = true
                slowAnswer = client.callSimpleAction("testAction2", "testData", null, null)
            }

            Thread.sleep(300)

            assertTrue {
                call2Started
            }
            fastAnswer = client0.callSimpleAction("testAction", "testData", null, null)

            val fasterCallFinished = fastAnswer != null && slowAnswer == null

            delay(500)
            val statement2 = fastAnswer != null && slowAnswer == null

            delay(1500)
            val statement3 = fastAnswer != null && slowAnswer != null

            return@assertTrue fasterCallFinished && statement2 && statement3
        }
    }
}
