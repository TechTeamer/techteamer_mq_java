import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.util.UUID


val logger: Logger = LoggerFactory.getLogger("testLogger")

fun main() {
    val myLogger = logger
    var myConfig = object : QueueConfig {
        override var url: String
            get() = "amqp://guest:guest@localhost:5672/"
            set(value) {}
        override val options: String
            get() = "Temp value"
        override val logger: Logger
            get() = myLogger
    }

    val pool = ConnectionPool(mapOf("defaultConnectionName" to "mydefaultname"))

    pool.setupQueueManagers(mapOf("mydefaultname" to myConfig))

    val queue = pool.defaultConnection



    queue.getRPCServer("test3", MyFirstRpcServer::class.java, object : RpcOptions {
        override val prefetchCount: Int
            get() = 3
        override val queueMaxSize: Int
            get() = 5
        override val timeOutMs: Int
            get() = 10000
    })


    queue.getRPCServer("test5", MyFirstRpcServer::class.java, object : RpcOptions {
        override val prefetchCount: Int
            get() = 3
        override val queueMaxSize: Int
            get() = 5
        override val timeOutMs: Int
            get() = 10000
    })

    // println(QueueConnection::class.java.getDeclaredConstructor())


    val client0 = queue.getRPCClient("test1", MyRPCClientt::class.java, object : RpcOptions {
        override val prefetchCount: Int
            get() = 3
        override val queueMaxSize: Int
            get() = 5
        override val timeOutMs: Int
            get() = 10000
    })


    val client = queue.getRPCClient("test2", MyRPCClient::class.java, object : RpcOptions {
        override val prefetchCount: Int
            get() = 3
        override val queueMaxSize: Int
            get() = 5
        override val timeOutMs: Int
            get() = 10000
    })

    val server = queue.getQueueServer("test10", MyQueueServer::class.java, object : ConnectionOptions {
        override val maxRetry = 1
        override val timeOutMs = 5000
        override val prefetchCount = 1
    })

    val cqClient = queue.getQueueClient("test20", MyQueueClient::class.java)


    println("SERVERS ${queue.queueServers}")
    pool.connect()

    client?.call(mutableMapOf("testtttt" to 20), 5000, null)

    client0?.call(mutableMapOf("testtttt00000" to 200000), 5000, null)

    cqClient?.sendAction(
        "action",
        data = mutableMapOf("teszztem" to "tteszt"),
        attachments = mutableMapOf(),
        correlationId = UUID.randomUUID().toString()
    )

//    val client = RPCClient(myChannel, "test2")
//    client.initialize()
//
//    val message = QueueMessage(
//        "ok",
//        mutableMapOf("x" to "x", "y" to true, "z" to 70),
//    )
//
//    message.addAttachment("testAtt", "test".toByteArray())
//    message.addAttachment("testAtt2", "testdfguzsdgvf".toByteArray())
//
//    println(message.attachments)
//
//    val resp = client.call(message)
//    if (resp != null) {
//        val unserializedResp = unserialize(resp)
//        if (unserializedResp != null) {
//            println(unserializedResp.data)
//            println(unserializedResp.attachments)
//        }
//    }


//    println(testFun3())
    //testFun(TestClass(2).javaClass)
}

class MyRPCClientt(
    override val connection: QueueConnection, override val rpcName: String, override val logger: Logger
) : RPCClient(connection, rpcName, logger) {
    override fun call(
        message: MutableMap<String, Any?>, timeOutMs: Int?, attachments: MutableMap<String, ByteArray>?
    ): ByteArray? {
        val result = super.call(message, timeOutMs, attachments)

        return result
    }
}

class MyRPCClient constructor(
    override val connection: QueueConnection, override val rpcName: String, override val logger: Logger
) : RPCClient(connection, rpcName, logger) {


    override fun call(
        message: MutableMap<String, Any?>, timeOutMs: Int?, attachments: MutableMap<String, ByteArray>?
    ): ByteArray? {
        logger.info("started")
        return super.call(message, timeOutMs, attachments)
    }
}

class MyFirstRpcServer(override val ch: Channel, override val name: String, logger: Logger) :
    RPCServer(ch, name, logger) {

    override fun callback(
        requestBody: ByteArray, response: QueueResponse, message: QueueMessage?
    ): MutableMap<String, Any?> {
        println("called callback")
        response.addAttachment("testAtt", "test".toByteArray())
        response.ok("fain")
        return mutableMapOf("test" to "test", "valami" to "valami,", "igen" to 20)
    }
}

class MyQueueServer(
    override val queueConnection: QueueConnection, override val logger: Logger,
    override val name: String, override val options: ConnectionOptions
) : QueueServer(queueConnection, logger, name, options) {
    override fun callback(
        data: MutableMap<String, Any?>,
        props: BasicProperties,
        request: QueueMessage,
        delivery: Delivery
    ): Any? {
        logger.info("received $data")
        return null
    }
}

class MyQueueClient(
    override val queueConnection: QueueConnection,
    override val logger: Logger,
    override val name: String
) : QueueClient(queueConnection, logger, name) {
    fun doAction(data: MutableMap<String, Any?>) {
        sendAction("action", data, attachments = mutableMapOf())
    }
}

//open class TestOne(open val i: Int) {
//    open fun print() {
//        println("test1")
//    }
//}
//
//class TestClass constructor(override val i: Int) : TestOne(i) {
//
//    override fun print() {
//        println("test2 $i")
//        println(UUID.randomUUID().toString())
//    }
//}
//
//fun testFun(OverrideClass: Class<TestOne>) {
//    val i = 30
//    val testI = OverrideClass.getConstructor(1.javaClass).newInstance(i)
//
//    testI.print()
//}







