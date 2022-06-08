import org.slf4j.Logger
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
        connection.logger = loggerInput as Logger
    }

    fun getRPCClient(rpcName: String, OverrideClass: Any = RPCClient::class.java, options: RpcOptions): Any? {
        if (rpcClients.contains(rpcName)) return rpcClients[rpcName]

        var optionsToSet: RpcOptions = object : RpcOptions {
            override val queueMaxSize: Int = config.rpcQueueMaxSize
            override val timeOutMs: Int = config.rpcTimeoutMs
        }

        if (options != null) {
            optionsToSet = options
        }

        val myClass = OverrideClass as Class<RPCClient>
        val rpcClient = myClass.constructors.first().newInstance(
            connection,
            rpcName,
            logger,
            optionsToSet
        )

        rpcClients[rpcName] = rpcClient

        return rpcClient
    }

    fun getRPCServer(rpcName: String, OverrideClass: Any = RPCServer::class.java, options: RpcServerOptions): Any? {
        if (rpcServers.contains(rpcName)) return rpcServers[rpcName]

        var optionsToSet: RpcServerOptions = object : RpcServerOptions {
            override val timeOutMs: Int = config.rpcTimeoutMs
        }

        if (options != null) {
            optionsToSet = options
        }

        val ch = connection.getChannel()
        ch.queueDeclare(rpcName, true, false, true, null)

        val myClass = OverrideClass as Class<RPCServer>
        val rpcServer = myClass.constructors.first().newInstance(ch, rpcName, logger, optionsToSet)

        rpcServers[rpcName] = rpcServer

        return rpcServer
    }

    fun getPublisher(exchangeName: String, OverrideClass: Any = Publisher::class.java): Any? {
        if (publishers.contains(exchangeName)) return publishers[exchangeName]

        val myClass = OverrideClass as Class<Publisher>
        val publisher = myClass.constructors.first().newInstance(connection, logger, exchangeName)

        publishers[exchangeName] = publisher

        return publisher
    }

    fun getSubscriber(exchangeName: String, OverrideClass: Any = Subscriber::class.java, options: ConnectionOptions): Any? {
        if (subscribers.contains(exchangeName)) return subscribers[exchangeName]

        val myClass = OverrideClass as Class<Subscriber>
        val subscriber = myClass.constructors.first().newInstance(connection, logger, exchangeName, options)

        subscribers[exchangeName] = subscriber

        return subscriber
    }

    fun getQueueClient(queueName: String, OverrideClass: Any = QueueClient::class.java): Any? {
        if (queueClients.contains(queueName)) return queueClients[queueName]

        val myClass = OverrideClass as Class<QueueClient>
        val queueClient = myClass.constructors.first().newInstance(connection, logger, queueName)

        queueClients[queueName] = queueClient

        return queueClient
    }

    fun getQueueServer(
        queueName: String, OverrideClass: Any = QueueServer::class.java, options: ConnectionOptions
    ): Any? {
        if (queueServers.contains(queueName)) return queueServers[queueName]

        val myClass = OverrideClass as Class<QueueServer>
        val queueServer = myClass.constructors.first().newInstance(connection, logger, queueName, options)

        queueServers[queueName] = queueServer

        return queueServer
    }
}
