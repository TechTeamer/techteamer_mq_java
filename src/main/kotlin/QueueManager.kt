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

    var rpcClients = mutableMapOf<String, RPCClient>()
    var rpcServers = mutableMapOf<String, RPCServer>()
    var publishers = mutableMapOf<String, Publisher>()
    var subscribers = mutableMapOf<String, Subscriber>()
    var queueServers = mutableMapOf<String, QueueServer>()
    var queueClients = mutableMapOf<String, QueueClient>()
    fun connect() {

        try {
            rpcServers.forEach { t ->
                thread {
                    t.value.initialize()
                }
            }
            rpcClients.forEach { t ->
                t.value.initialize()
            }
            publishers.forEach { t ->
                t.value.initialize()
            }
            subscribers.forEach { t ->
                thread {
                    t.value.initialize()
                }
            }
            queueServers.forEach { t ->
                thread {
                    t.value.initialize()
                    logger.info("Queueserver ${t.key} initiated")
                }
            }
            queueClients.forEach { t ->
                t.value.initialize()
                logger.info("QueueClient ${t.key} initiated")
            }
        } catch (err: Exception) {
            logger.error("Failed to initialize servers", err)
        }

    }

    fun setLogger(loggerInput: Any) {
        logger = loggerInput as KLogger
    }

    fun getRPCClient(rpcName: String, OverrideClass: Any, options: RpcOptions): RPCClient? {
        if (rpcClients.contains(rpcName)) return rpcClients[rpcName]

        println(QueueConnection::class.java.getDeclaredConstructor(QueueConfig::class.java))
        val myClass = OverrideClass as Class<RPCClient>
        val rpcClient = myClass.constructors.last().newInstance(connection, rpcName, logger) as RPCClient

        rpcClients[rpcName] = rpcClient

        return rpcClient
    }

    fun getRPCServer(rpcName: String, OverrideClass: Any, options: RpcOptions): RPCServer? {
        if (rpcServers.contains(rpcName)) return rpcServers[rpcName]

        val ch = connection.getChannel()
        ch.queueDeclare(rpcName, true, false, true, null)

        val myClass = OverrideClass as Class<RPCServer>
        val rpcServer = myClass.constructors.last().newInstance(ch, rpcName, logger) as RPCServer

        rpcServers[rpcName] = rpcServer

        return rpcServer
    }

    fun getPublisher(exchangeName: String, OverrideClass: Any): Publisher? {
        if (publishers.contains(exchangeName)) return publishers[exchangeName]

        val myClass = OverrideClass as Class<Publisher>
        val publisher = myClass.constructors.last().newInstance(connection, logger, exchangeName) as Publisher

        publishers[exchangeName] = publisher

        return publisher
    }

    fun getSubscriber(exchangeName: String, OverrideClass: Any): Subscriber? {
        if (subscribers.contains(exchangeName)) return subscribers[exchangeName]

        val myClass = OverrideClass as Class<Subscriber>
        val subscriber = myClass.constructors.last().newInstance(connection, logger, exchangeName) as Subscriber

        subscribers[exchangeName] = subscriber

        return subscriber
    }

    fun getQueueClient(queueName: String, OverrideClass: Any): QueueClient? {
        if (queueClients.contains(queueName)) return queueClients[queueName]

        val myClass = OverrideClass as Class<QueueClient>
        println(myClass.constructors.last())
        val queueClient = myClass.constructors.last().newInstance(connection, logger, queueName) as QueueClient

        queueClients[queueName] = queueClient

        return queueClient
    }

    fun getQueueServer(
        queueName: String, OverrideClass: Any, options: ConnectionOptions
    ): QueueServer? {
        if (queueServers.contains(queueName)) return queueServers[queueName]

        val myClass = OverrideClass as Class<QueueServer>
        val queueServer = myClass.constructors.last().newInstance(connection, logger, queueName, options) as QueueServer

        queueServers[queueName] = queueServer

        return queueServer
    }
}

