import com.facekom.mq_kotlin.*
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.slf4j.Logger
import java.lang.Thread.sleep
import kotlin.test.assertTrue

var rpcServerTestResult: QueueMessage? = null

class RPCTest {
    private val testhelper = TestHelper()
    private val queueManager = QueueManager(testhelper.testConfig)
    private var rpcClient: MyRPCClient? = null
    val rpcName = "techteamer-mq-js-test-rpc"

    init {
        queueManager.getRPCServer(rpcName, MyRpcServer::class.java, options = object : RpcServerOptions {
            override val prefetchCount: Int
                get() = 3
            override val timeOutMs: Int
                get() = 10000
        }) as MyRpcServer
        queueManager.connect()

        rpcClient = queueManager.getRPCClient(rpcName, MyRPCClient::class.java, options = object : RpcOptions {
            override val prefetchCount: Int
                get() = 3
            override val queueMaxSize: Int
                get() = 5
            override val timeOutMs: Int
                get() = 15000
        }) as MyRPCClient
        queueManager.connect()
    }

    @Test
    fun sendAndReceiveRpc() = runBlocking {
        val result = rpcClient?.sendTest()
        assertTrue {
            return@assertTrue result?.data?.get("test") == "test" &&
                    result.attachments.contains("testAtt") &&
                    rpcServerTestResult?.data?.get("testData") == "test" &&
                    rpcServerTestResult?.attachments?.contains("testAttachment") == true
        }
    }

    @Test
    fun rpcServerTimeoutTest() = runBlocking {
        queueManager.getRPCServer("rpcServerDelayed", MyRpcServerTimeout::class.java, options = object :
            RpcServerOptions {
            override val prefetchCount: Int
                get() = 3
            override val timeOutMs: Int
                get() = 1000
        }) as MyRpcServerTimeout
        queueManager.connect()

        val rpcClientTwo = queueManager.getRPCClient("rpcServerDelayed", MyRPCClient::class.java, options = object :
            RpcOptions {
            override val prefetchCount: Int
                get() = 3
            override val queueMaxSize: Int
                get() = 5
            override val timeOutMs: Int
                get() = 15000
        }) as MyRPCClient
        queueManager.connect()

        val res = rpcClientTwo.sendTest()

        assertTrue {
            return@assertTrue (res?.status == "error") &&
                    (res.data?.get("error") == "timeout")
        }
    }

    class MyRPCClient(
        override val connection: QueueConnection,
        override val rpcName: String,
        override val logger: Logger,
        override val options: RpcOptions
    ) : RPCClient(connection, rpcName, logger, options) {

        fun sendTest(): QueueMessage? {
            return call(
                mutableMapOf("testData" to "test"),
                15000,
                mutableMapOf("testAttachment" to "hello".toByteArray())
            )
        }
    }

    class MyRpcServer(
        connection: QueueConnection,
        name: String,
        logger: Logger,
        override val options: RpcServerOptions
    ) : RPCServer(connection, name, logger, options) {

        override suspend fun callback(
            data: QueueMessage,
            delivery: Delivery,
            response: QueueResponse,
        ): MutableMap<String, Any?> {
            rpcServerTestResult = data
            response.addAttachment("testAtt", "test".toByteArray())
            response.addAttachment("testAtt2222", "test20".toByteArray())
            response.ok("ok")
            return mutableMapOf("test" to "test")
        }
    }

    class MyRpcServerTimeout(
        connection: QueueConnection,
        name: String,
        logger: Logger,
        override val options: RpcServerOptions
    ) :
        RPCServer(connection, name, logger, options) {

        override suspend fun callback(
            data: QueueMessage,
            delivery: Delivery,
            response: QueueResponse,
        ): MutableMap<String, Any?> {
            delay(5000)
            response.addAttachment("testAtt", "test".toByteArray())
            response.addAttachment("testAtt2222", "test20".toByteArray())
            response.ok("ok")
            return mutableMapOf("test" to "test")
        }
    }
}
