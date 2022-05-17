import mu.KLogger


class QueueManager(private val config: QueueConfig) {
    lateinit var connection: QueueConnection
    var logger = config.logger

    var rpcClients = mutableMapOf<String, RPCClient>()
    var rpcServers = mutableMapOf<String, RPCServer>()
    var publishers = mutableMapOf<String, Publisher>()
    var subscribers = mutableMapOf<String, Subscriber>()
    var queueServers = mutableMapOf<String, QueueServer>()
    var queueClients = mutableMapOf<String, QueueClient>()
    fun connect() {
        connection = QueueConnection(config)

        try {
            rpcServers.forEach { t ->
                t.value.initialize()
            }
            rpcClients.forEach { t ->
                t.value.initialize()
            }
            publishers.forEach { t ->
                t.value.initialize()
            }
            subscribers.forEach { t ->
                t.value.initialize()
            }
            queueServers.forEach { t ->
                t.value.initialize()
            }
            queueClients.forEach { t ->
                t.value.initialize()
            }
        } catch (err: Exception) {
            logger.error("Failed to initialize servers", err)
        }

    }

    fun setLogger(loggerInput: Any) {
        logger = loggerInput as KLogger
    }

    fun getRPCClient(rpcName: String, OverrideClass: Class<RPCClient>, options: RpcOptions): RPCClient? {
        if (rpcClients.contains(rpcName)) return rpcClients[rpcName]

        val rpcClient = OverrideClass.getDeclaredConstructor().newInstance(connection, rpcName, options)

        rpcClients[rpcName] = rpcClient

        return rpcClient
    }

    fun getRPCServer(rpcName: String, OverrideClass: Class<RPCServer>, options: RpcOptions): RPCServer? {
        if (rpcServers.contains(rpcName)) return rpcServers[rpcName]

        val rpcServer = OverrideClass.getDeclaredConstructor().newInstance(connection, rpcName, options)

        rpcServers[rpcName] = rpcServer

        return rpcServer
    }

    fun getPublisher(exchangeName: String, OverrideClass: Class<Publisher>): Publisher? {
        if (publishers.contains(exchangeName)) return publishers[exchangeName]

        val publisher = OverrideClass.getDeclaredConstructor().newInstance(connection, logger, exchangeName)

        publishers[exchangeName] = publisher

        return publisher
    }

    fun getSubscriber(exchangeName: String, OverrideClass: Class<Subscriber>): Subscriber? {
        if (subscribers.contains(exchangeName)) return subscribers[exchangeName]

        val subscriber = OverrideClass.getDeclaredConstructor().newInstance(connection, logger, exchangeName)

        subscribers[exchangeName] = subscriber

        return subscriber
    }

    fun getQueueClient(queueName: String, OverrideClass: Class<QueueClient>): QueueClient? {
        if (queueClients.contains(queueName)) return queueClients[queueName]

        val queueClient = OverrideClass.getDeclaredConstructor().newInstance(connection, logger, queueName)

        queueClients[queueName] = queueClient

        return queueClient
    }

    fun getQueueServer(
        queueName: String,
        OverrideClass: Class<QueueServer>,
        options: ConnectionOptions
    ): QueueServer? {
        if (queueServers.contains(queueName)) return queueServers[queueName]

        val queueServer = OverrideClass.getDeclaredConstructor().newInstance(connection, logger, queueName, options)

        queueServers[queueName] = queueServer

        return queueServer
    }
}

