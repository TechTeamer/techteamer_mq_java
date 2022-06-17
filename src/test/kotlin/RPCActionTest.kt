import com.facekom.mq_kotlin.*
import kotlinx.coroutines.*
import org.junit.Test
import kotlin.concurrent.thread
import kotlin.test.assertFails
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class RPCActionTest {
    private val rpcName = "test-rpc-action"
    private val testhelper = TestHelper()

    private val queueManager = QueueManager(testhelper.testConfig)

    private var rpcServer: RPCServer

    private var rpcClient: RPCClient

    init {
        rpcServer = queueManager.getRPCServer(rpcName, options = object : RpcServerOptions {
            override val prefetchCount: Int
                get() = 3
            override val timeOutMs: Int
                get() = 10000
        }) as RPCServer

        queueManager.connect()

        rpcClient = queueManager.getRPCClient(rpcName, options = object : RpcOptions {
            override val prefetchCount: Int
                get() = 3
            override val queueMaxSize: Int
                get() = 1
            override val timeOutMs: Int
                get() = 15000
        }) as RPCClient
        queueManager.connect()
    }

    var testRegisteredData: QueueMessage? = null

    @Test
    fun registerRPCActionTest() {
        rpcServer.registerAction("testAction") { _, _, _, response, message ->
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

    @Test
    fun rpcServerNonBlockingTest() = runBlocking {
        assertTrue {
            var answer1: QueueMessage? = null
            var answer2: QueueMessage? = null

            var call2Started = false

            val server = queueManager.getRPCServer("testmain", options = object : RpcServerOptions {
                override val prefetchCount: Int
                    get() = 3
                override val timeOutMs: Int
                    get() = 10000
            }) as RPCServer

            server.registerAction("testAction") { _, _, _, response, message ->
                response.addAttachment("testAttachmentAnswer", "helloAnswer".toByteArray())
                return@registerAction mutableMapOf<String, Any?>("testAnswer" to "answer")
            }

            server.registerAction("testAction2") { _, _, _, response, message ->
                Thread.sleep(1000)
                response.addAttachment("testAttachmentAnswer", "helloAnswer".toByteArray())
                return@registerAction mutableMapOf<String, Any?>("testAnswer2" to "answer2")
            }

            val client0 = queueManager.getRPCClient("testmain", options = object : RpcOptions {
                override val prefetchCount: Int
                    get() = 3
                override val queueMaxSize: Int
                    get() = 5
                override val timeOutMs: Int
                    get() = 15000
            }) as RPCClient


            queueManager.connect()

            CoroutineScope(Dispatchers.IO).launch {
                val pool2 = ConnectionPool(mapOf("other" to "mydefaultname"))

                pool2.setupQueueManagers(mapOf("mydefaultname" to testhelper.testConfig))

                val queue2 = pool2.defaultConnection
                val client = queue2.getRPCClient("testmain", options = object : RpcOptions {
                    override val prefetchCount: Int
                        get() = 3
                    override val queueMaxSize: Int
                        get() = 5
                    override val timeOutMs: Int
                        get() = 15000
                }) as RPCClient
                pool2.connect()
                call2Started = true
                answer2 = client.callAction("testAction2", mutableMapOf("test2" to "test2"), null, null)
            }

            Thread.sleep(300)

            assertTrue {
                call2Started
            }
            answer1 = client0.callAction("testAction", mutableMapOf("test2" to "test2"), null, null)

            val statement1 = answer1 != null && answer2 == null

            delay(500)
            val statement2 = answer1 != null && answer2 == null

            delay(1500)
            val statement3 = answer1 != null && answer2 != null

            return@assertTrue statement1 && statement2 && statement3
        }
    }

    @Test
    fun testFullQueue() {
        queueManager.setLogger(testhelper.logger)

        rpcServer.registerAction("testAction") { _, _, _, response, message ->
            return@registerAction mutableMapOf<String, Any?>("testAnswer" to "answer")
        }
        assertTrue {
            var answer1: QueueMessage? = null
            var answer2: QueueMessage? = null
            var answer3: QueueMessage? = null
            var answer4: QueueMessage? = null

            CoroutineScope(Dispatchers.IO).launch {
                answer1 = rpcClient.callAction(
                    "testAction",
                    mutableMapOf("testData" to "data"),
                    null,
                    mutableMapOf("testAttachment" to "helloTest".toByteArray())
                )
            }

            CoroutineScope(Dispatchers.IO).launch {
                answer2 = rpcClient.callAction(
                    "testAction",
                    mutableMapOf("testData" to "data"),
                    null,
                    mutableMapOf("testAttachment" to "helloTest".toByteArray())
                )
            }

            CoroutineScope(Dispatchers.IO).launch {
                answer3 = rpcClient.callAction(
                    "testAction",
                    mutableMapOf("testData" to "data"),
                    null,
                    mutableMapOf("testAttachment" to "helloTest".toByteArray())
                )
            }

            CoroutineScope(Dispatchers.IO).launch {
                answer4 = rpcClient.callAction(
                    "testAction",
                    mutableMapOf("testData" to "data"),
                    null,
                    mutableMapOf("testAttachment" to "helloTest".toByteArray())
                )
            }
            runBlocking {
                delay(3000)
            }

            // at least one answer should be null, as the queue has to be full because queueMaxSize was set to 1
            return@assertTrue !(answer1 != null && answer2 != null && answer3 != null && answer4 != null)
        }
    }
}
