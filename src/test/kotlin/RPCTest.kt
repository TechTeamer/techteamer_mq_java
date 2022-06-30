import com.facekom.mq_kotlin.*
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

var rpcServerTestResult: QueueMessage? = null


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RPCTest {
    private val testhelper = TestHelper()
    private val queueManager = QueueManager(testhelper.testConfig)
    private val rpcClient: RPCClient
    private val rpcServer: RPCServer
    val rpcName = "techteamer-mq-java-test-rpc"

    init {
        val rpcClientOption = RpcClientOptions()
        rpcClientOption.prefetchCount = 3
        rpcClientOption.queueMaxSize = 5
        rpcClientOption.timeOutMs = 15000
        rpcClient = queueManager.getRPCClient(rpcName, rpcClientOption)

        val rpcServerOptions = RpcServerOptions()
        rpcServerOptions.prefetchCount = 3
        rpcServerOptions.timeOutMs = 10000
        rpcServerOptions.queue.durable = false
        rpcServerOptions.queue.exclusive = true
        rpcServer = queueManager.getRPCServer(rpcName, rpcServerOptions)

        queueManager.connect()
    }

    @Test
    fun testRpcEchoRoundtrip() = runBlocking {
        var testMessageReceived = false

        rpcServer.consume { data, request, response, delivery ->
            testMessageReceived = true
            return@consume data
        }

        val result = rpcClient.call(testhelper.stringData)
        val error = testhelper.checkData(result?.data, testhelper.stringData)

        assertTrue ("Message should have been received") {
            testMessageReceived
        }
        assertTrue ("Response should be valid") {
            error == null
        }
    }

    @Test
    fun rpcServerTimeoutTest() = runBlocking {
        var testMessageReceived = false

        rpcServer.consume { data, request, response, delivery -> runBlocking {
            testMessageReceived = true
            delay(5000)
            return@runBlocking data
        }}

        val result = rpcClient.call(testhelper.stringData)
        val error = testhelper.checkData(result?.data, testhelper.stringData)

        assertTrue ("Message should have been received") {
            testMessageReceived
        }
        assertTrue ("Response should be valid") {
            error == null
        }
    }
}
