import com.facekom.mq.*
import com.google.gson.JsonElement
import com.google.gson.JsonParser
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
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
        rpcServerOptions.timeOutMs = 1000
        rpcServerOptions.queue.durable = false
        rpcServerOptions.queue.exclusive = true
        rpcServer = queueManager.getRPCServer(rpcName, rpcServerOptions)

        queueManager.connect()
    }

    @Test
    fun testConsumeCallback() = runBlocking {
        val oldCallback = rpcServer._callback
        val newCallback: RpcHandler =
            { data, props, request, delivery -> run { return@run JsonParser.parseString(testhelper.jsonStringMessage) } }
        rpcServer.consume(newCallback)

        assertTrue("Consume call should set new message handler callback") {
            oldCallback !== rpcServer._callback && newCallback == rpcServer._callback
        }
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

        assertTrue("Message should have been received") {
            testMessageReceived
        }
        assertTrue("Response should be valid") {
            error == null
        }
    }

    @Test
    fun testRpcStringMessage() = runBlocking {
        var testMessageReceived = false

        rpcServer.consume { data, request, response, delivery ->
            testMessageReceived = true
            return@consume data
        }

        val result = rpcClient.call(testhelper.string)

        assertTrue("Message should have been received") {
            testMessageReceived
        }
    }

    @Test
    fun registerRPCActionTest() {
        rpcServer.registerAction("testAction") { data, request, response, delivery ->
            response.addAttachment("testAttachmentAnswer", "helloAnswer".toByteArray())
            return@registerAction data
        }

        class MyAction(myAction: String) : QueueAction<String>(myAction) {
            override val data: String
                get() = testhelper.jsonStringMessage

            override fun getData(): JsonElement {
                return JsonParser.parseString(data)
            }

        }

        val action = MyAction("testAction")

        val result = rpcClient.callAction(
            action,
            1000,
            mutableMapOf("testAttachment" to "helloTest".toByteArray())
        )

        val data = result?.data?.asJsonObject?.get("data")?.asString
        val attachment = result?.attachments?.get("testAttachmentAnswer")

        assertTrue {
            data == "test" && attachment?.let { String(it) } == "helloAnswer"
        }
    }

    @Test
    fun rpcServerTimeoutTest() = runBlocking {
        var testMessageReceived = false

        suspend fun cb(
            data: JsonElement?,
            request: QueueMessage,
            response: QueueResponse,
            delivery: Delivery
        ): JsonElement? {
            delay(1500)
            testMessageReceived = true
            return data
        }

        rpcServer.consume(::cb)

        val result = rpcClient.call(testhelper.stringData)
        val error = result?.data?.asJsonObject?.get("error")?.asString

        assertTrue("Message should have been received") {
            !testMessageReceived
        }
        assertTrue("Error should be timeout") {
            result?.status == "error" && error == "timeout"
        }
    }
}
