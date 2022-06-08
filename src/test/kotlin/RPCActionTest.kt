import org.junit.Test
import kotlin.test.assertTrue

class RPCActionTest {
    val rpcName = "test-rpc-action"
    private val testhelper = TestHelper()

    private val queueManager = QueueManager(testhelper.testConfig)

    val rpcServer = queueManager.getRPCServer(rpcName, options = object : RpcServerOptions {
        override val prefetchCount: Int
            get() = 3
        override val timeOutMs: Int
            get() = 10000
    }) as RPCServer

    val rpcClient = queueManager.getRPCClient(rpcName, options = object : RpcOptions {
        override val prefetchCount: Int
            get() = 3
        override val queueMaxSize: Int
            get() = 5
        override val timeOutMs: Int
            get() = 15000
    }) as RPCClient

    init {
        queueManager.connect()
    }

    var testRegisteredData: QueueMessage? = null

    @Test
    fun registerRPCActionTest() {

        rpcServer.registerAction("testAction") { thiss, data, delivery, response, message ->
            testRegisteredData = message
            response.addAttachment("testAttachmentAnswer", "helloAnswer".toByteArray())
            return@registerAction mutableMapOf<String, Any?>("testAnswer" to "answer")
        }


        val result = rpcClient.callAction(
            "testAction",
            mutableMapOf("testData" to "data"),
            null,
            mutableMapOf("testAttachment" to "helloTest".toByteArray())
        )

        assertTrue {
            val testRegisteredDataData = testRegisteredData?.data?.get("data") as MutableMap<*, *>
            return@assertTrue testRegisteredDataData["testData"] == "data" &&
                    result?.data?.get("testAnswer") == "answer" &&
                    testRegisteredData?.attachments?.get("testAttachment")?.let { String(it) } == "helloTest" &&
                    result.attachments["testAttachmentAnswer"]?.let { String(it) } == "helloAnswer"

        }
    }

}