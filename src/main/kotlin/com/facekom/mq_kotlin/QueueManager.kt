package com.facekom.mq_kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger


class QueueManager(private val config: QueueConfig) {
    var connection: QueueConnection = QueueConnection(config)
    var logger = config.logger

    var rpcClients = mutableMapOf<String, Any>()
    var rpcServers = mutableMapOf<String, Any>()
    var publishers = mutableMapOf<String, Any>()
    var subscribers = mutableMapOf<String, Any>()
    var queueServers = mutableMapOf<String, Any>()
    var queueClients = mutableMapOf<String, Any>()
    fun connect() = runBlocking {
        try {
            connection.connect()
        } catch (err: Exception) {
            logger.error("Failed to connect to queue server $err")
        }
        try {
            rpcServers.forEach { t ->
                CoroutineScope(Dispatchers.IO).launch {
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
                CoroutineScope(Dispatchers.IO).launch {
                    val tClass = t.value as Subscriber
                    tClass.initialize()
                }
            }
            queueServers.forEach { t ->
                CoroutineScope(Dispatchers.IO).launch {
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

    fun getRPCClient(rpcName: String, overrideClass: Any = RPCClient::class.java, options: RpcOptions?): Any? {
        if (rpcClients.contains(rpcName)) return rpcClients[rpcName]

        var optionsToSet: RpcOptions = object : RpcOptions {
            override val queueMaxSize: Int = config.rpcQueueMaxSize
            override val timeOutMs: Int = config.rpcTimeoutMs
        }

        if (options != null) {
            optionsToSet = options
        }

        val myClass = overrideClass as Class<RPCClient>
        val rpcClient = myClass.getConstructor(
            QueueConnection::class.java,
            String::class.java,
            Logger::class.java,
            RpcOptions::class.java
        ).newInstance(
            connection,
            rpcName,
            logger,
            optionsToSet
        )

        rpcClients[rpcName] = rpcClient

        return rpcClient
    }

    fun getRPCServer(rpcName: String, overrideClass: Any = RPCServer::class.java, options: RpcServerOptions?): Any? {
        if (rpcServers.contains(rpcName)) return rpcServers[rpcName]

        var optionsToSet: RpcServerOptions = object : RpcServerOptions {
            override val timeOutMs: Int = config.rpcTimeoutMs
        }

        if (options != null) {
            optionsToSet = options
        }

        val myClass = overrideClass as Class<RPCServer>
        val rpcServer = myClass.getConstructor(
            QueueConnection::class.java,
            String::class.java,
            Logger::class.java,
            RpcServerOptions::class.java
        ).newInstance(connection, rpcName, logger, optionsToSet)

        rpcServers[rpcName] = rpcServer

        return rpcServer
    }

    fun getPublisher(exchangeName: String, overrideClass: Any = Publisher::class.java): Any? {
        if (publishers.contains(exchangeName)) return publishers[exchangeName]

        val myClass = overrideClass as Class<Publisher>
        val publisher = myClass.getConstructor(QueueConnection::class.java, Logger::class.java, String::class.java)
            .newInstance(connection, logger, exchangeName)

        publishers[exchangeName] = publisher

        return publisher
    }

    fun getSubscriber(
        exchangeName: String,
        overrideClass: Any = Subscriber::class.java,
        options: ConnectionOptions
    ): Any? {
        if (subscribers.contains(exchangeName)) return subscribers[exchangeName]

        val myClass = overrideClass as Class<Subscriber>
        val subscriber = myClass.getConstructor(
            QueueConnection::class.java,
            Logger::class.java,
            String::class.java,
            ConnectionOptions::class.java
        ).newInstance(connection, logger, exchangeName, options)

        subscribers[exchangeName] = subscriber

        return subscriber
    }

    fun getQueueClient(queueName: String, overrideClass: Any = QueueClient::class.java): Any? {
        if (queueClients.contains(queueName)) return queueClients[queueName]

        val myClass = overrideClass as Class<QueueClient>
        val queueClient = myClass.getConstructor(QueueConnection::class.java, Logger::class.java, String::class.java)
            .newInstance(connection, logger, queueName)

        queueClients[queueName] = queueClient

        return queueClient
    }

    fun getQueueServer(
        queueName: String, overrideClass: Any = QueueServer::class.java, options: ConnectionOptions
    ): Any? {
        if (queueServers.contains(queueName)) return queueServers[queueName]

        val myClass = overrideClass as Class<QueueServer>
        val queueServer = myClass.getConstructor(
            QueueConnection::class.java,
            Logger::class.java,
            String::class.java,
            ConnectionOptions::class.java
        ).newInstance(connection, logger, queueName, options)

        queueServers[queueName] = queueServer

        return queueServer
    }
}
