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
    }) as MyRPCClientt


    val client = queue.getRPCClient("test2", MyRPCClient::class.java, object : RpcOptions {
        override val prefetchCount: Int
            get() = 3
        override val queueMaxSize: Int
            get() = 5
        override val timeOutMs: Int
            get() = 10000
    }) as MyRPCClient

    val server = queue.getQueueServer("test10", MyQueueServer::class.java, object : ConnectionOptions {
        override val maxRetry = 1
        override val timeOutMs = 5000
        override val prefetchCount = 1
    })

    val cqClient = queue.getQueueClient("test20", MyQueueClient::class.java) as MyQueueClient

    queue.getSubscriber("test15", MySubscriber::class.java, object : ConnectionOptions {
        override val maxRetry = 1
        override val timeOutMs = 5000
        override val prefetchCount = 1
    })
    queue.getSubscriber("test15", MySubscriber::class.java, object : ConnectionOptions {
        override val maxRetry = 1
        override val timeOutMs = 5000
        override val prefetchCount = 1
    })


    val pubber = queue.getPublisher("test25", MyPublisher::class.java) as MyPublisher

    println("SERVERS ${queue.queueServers}")
    pool.connect()

    client.call(mutableMapOf("testtttt" to 20), 5000, null)

    client0.call(mutableMapOf("testtttt00000" to 200000), 5000, null)

    cqClient.doAction(
        data = mutableMapOf("teszztem" to "tteszt")
    )

    pubber.set(10)


}

class MyRPCClientt(
    override val connection: QueueConnection, override val rpcName: String, override val logger: Logger
) : RPCClient(connection, rpcName, logger) {
    override fun call(
        message: MutableMap<String, Any?>, timeOutMs: Int?, attachments: MutableMap<String, ByteArray>?
    ): ByteArray? {
        val result = super.call(message, timeOutMs, attachments)

        logger.info(result?.let { String(it) })

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
    override val queueConnection: QueueConnection,
    override var logger: Logger,
    override val name: String,
    override val options: ConnectionOptions
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

class MyPublisher(
    override val queueConnection: QueueConnection,
    override val logger: Logger,
    override val exchange: String
) : Publisher(queueConnection, logger, exchange) {
    fun set(id: Int) {
        sendAction(
            "send",
            mutableMapOf("data" to id),
            attachments = mutableMapOf("publisehrTestAttachment" to "hello".toByteArray())
        )
    }
}

class MySubscriber(
    override var connection: QueueConnection,
    override var logger: Logger,
    override val name: String,
    override val options: ConnectionOptions
) : Subscriber(connection, logger, name, options) {
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









