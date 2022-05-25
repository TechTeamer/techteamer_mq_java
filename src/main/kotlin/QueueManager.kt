import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.sun.jdi.InterfaceType
import mu.KLogger
import org.slf4j.Logger
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread


class QueueManager(private val config: QueueConfig) {
    var connection: QueueConnection = QueueConnection(config)
    var logger = config.logger

    var rpcClients = mutableMapOf<String, Any>()
    var rpcServers = mutableMapOf<String, Any>()
    var publishers = mutableMapOf<String, Any>()
    var subscribers = mutableMapOf<String, Any>()
    var queueServers = mutableMapOf<String, Any>()
    var queueClients = mutableMapOf<String, Any>()
    fun connect() {

        try {
            rpcServers.forEach { t ->
                thread {
                    val tClass = t.value as RPCServer
                    tClass.initialize()
                }
            }
            rpcClients.forEach { t ->
                val tClass = t.value as RPCClient
                tClass.initialize()
            }
            publishers.forEach { t ->
                val tClass = t.value as Publisher
                tClass.initialize()
            }
            subscribers.forEach { t ->
                thread {
                    val tClass = t.value as Subscriber
                    tClass.initialize()
                }
            }
            queueServers.forEach { t ->
                thread {
                    val tClass = t.value as QueueServer
                    tClass.initialize()
                }
            }
            queueClients.forEach { t ->
                val tClass = t.value as QueueClient
                tClass.initialize()
            }
        } catch (err: Exception) {
            logger.error("Failed to initialize servers", err)
        }

    }

    fun setLogger(loggerInput: Any) {
        logger = loggerInput as Logger
    }

    fun getRPCClient(rpcName: String, OverrideClass: Any, options: RpcOptions): Any? {
        if (rpcClients.contains(rpcName)) return rpcClients[rpcName]

        val myClass = OverrideClass as Class<RPCClient>
        val rpcClient = myClass.constructors.last().newInstance(connection, rpcName, logger)

        rpcClients[rpcName] = rpcClient

        return rpcClient
    }

    fun getRPCServer(rpcName: String, OverrideClass: Any, options: RpcOptions): Any? {
        if (rpcServers.contains(rpcName)) return rpcServers[rpcName]

        val ch = connection.getChannel()
        ch.queueDeclare(rpcName, true, false, true, null)

        val myClass = OverrideClass as Class<RPCServer>
        val rpcServer = myClass.constructors.last().newInstance(ch, rpcName, logger)

        rpcServers[rpcName] = rpcServer

        return rpcServer
    }

    fun getPublisher(exchangeName: String, OverrideClass: Any): Any? {
        if (publishers.contains(exchangeName)) return publishers[exchangeName]

        val myClass = OverrideClass as Class<Publisher>
        val publisher = myClass.constructors.last().newInstance(connection, logger, exchangeName)

        publishers[exchangeName] = publisher

        return publisher
    }

    fun getSubscriber(exchangeName: String, OverrideClass: Any, options: ConnectionOptions): Any? {
        if (subscribers.contains(exchangeName)) return subscribers[exchangeName]

        val myClass = OverrideClass as Class<Subscriber>
        val subscriber = myClass.constructors.last().newInstance(connection, logger, exchangeName, options)

        subscribers[exchangeName] = subscriber

        return subscriber
    }

    fun getQueueClient(queueName: String, OverrideClass: Any): Any? {
        if (queueClients.contains(queueName)) return queueClients[queueName]

        val myClass = OverrideClass as Class<QueueClient>
        val queueClient = myClass.constructors.last().newInstance(connection, logger, queueName)

        queueClients[queueName] = queueClient

        return queueClient
    }

    fun getQueueServer(
        queueName: String, OverrideClass: Any, options: ConnectionOptions
    ): Any? {
        if (queueServers.contains(queueName)) return queueServers[queueName]

        val myClass = OverrideClass as Class<QueueServer>
        val queueServer = myClass.constructors.last().newInstance(connection, logger, queueName, options)

        queueServers[queueName] = queueServer

        return queueServer
    }
}

