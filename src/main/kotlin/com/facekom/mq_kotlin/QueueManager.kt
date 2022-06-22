package com.facekom.mq_kotlin

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.slf4j.Logger

class QueueManager(private val config: QueueConfig) {
    var connection: QueueConnection = QueueConnection(config)
    var logger = config.logger

    var rpcClients = mutableMapOf<String, RPCClient>()
    var rpcServers = mutableMapOf<String, RPCServer>()
    var publishers = mutableMapOf<String, Publisher>()
    var subscribers = mutableMapOf<String, Subscriber>()
    var queueServers = mutableMapOf<String, QueueServer>()
    var queueClients = mutableMapOf<String, QueueClient>()

    fun connect() = runBlocking {
        try {
            connection.connect()
        } catch (err: Exception) {
            logger.error("Failed to connect to queue server $err")
        }

        try {
            rpcServers.forEach { t ->
                CoroutineScope(Dispatchers.IO).launch {
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
                CoroutineScope(Dispatchers.IO).launch {
                    t.value.initialize()
                }
            }
            queueServers.forEach { t ->
                CoroutineScope(Dispatchers.IO).launch {
                    t.value.initialize()
                }
            }
            queueClients.forEach { t ->
                t.value.initialize()
            }
        } catch (err: Exception) {
            logger.error("Failed to initialize servers", err)
        }
    }

    fun setLogger(loggerInput: Any) {
        connection.logger = loggerInput as Logger
    }

    fun getRPCClient(rpcName: String, overrideClass: Class<out RPCClient>? = RPCClient::class.java, options: RpcOptions?): RPCClient {
        if (rpcClients.contains(rpcName)) return rpcClients.getValue(rpcName)

        var optionsToSet: RpcOptions = object : RpcOptions {
            override val queueMaxSize: Int = config.rpcQueueMaxSize
            override val timeOutMs: Int = config.rpcTimeoutMs
        }

        if (options != null) {
            optionsToSet = options
        }

        val rpcClient = if (overrideClass != null) {
            overrideClass
                .getConstructor(QueueConnection::class.java, String::class.java, Logger::class.java, RpcOptions::class.java)
                .newInstance(connection, rpcName, logger, optionsToSet)
        } else {
            RPCClient(connection, rpcName, logger, optionsToSet)
        }

        rpcClients[rpcName] = rpcClient

        return rpcClient
    }

    fun getRPCServer(rpcName: String, overrideClass: Class<out RPCServer>? = RPCServer::class.java, options: RpcServerOptions?): RPCServer {
        if (rpcServers.contains(rpcName)) return rpcServers.getValue(rpcName)

        var optionsToSet: RpcServerOptions = object : RpcServerOptions {
            override val timeOutMs: Int = config.rpcTimeoutMs
        }

        if (options != null) {
            optionsToSet = options
        }

        val rpcServer = if (overrideClass != null) {
            overrideClass
                .getConstructor(QueueConnection::class.java, String::class.java, Logger::class.java, RpcServerOptions::class.java)
                .newInstance(connection, rpcName, logger, optionsToSet)
        } else {
            RPCServer(connection, rpcName, logger, optionsToSet)
        }

        rpcServers[rpcName] = rpcServer

        return rpcServer
    }

    fun getPublisher(exchangeName: String, overrideClass: Class<out Publisher>? = Publisher::class.java): Publisher {
        if (publishers.contains(exchangeName)) return publishers.getValue(exchangeName)

        val publisher = if (overrideClass != null) {
            overrideClass
                .getConstructor(QueueConnection::class.java, Logger::class.java, String::class.java)
                .newInstance(connection, logger, exchangeName)
        } else {
            Publisher(connection, logger, exchangeName)
        }

        publishers[exchangeName] = publisher

        return publisher
    }

    fun getSubscriber(exchangeName: String, overrideClass: Class<out Subscriber>? = Subscriber::class.java, options: ConnectionOptions = object : ConnectionOptions {}): Subscriber {
        if (subscribers.contains(exchangeName)) return subscribers.getValue(exchangeName)

        val subscriber = if (overrideClass != null) {
            overrideClass
                .getConstructor(QueueConnection::class.java, Logger::class.java, String::class.java, ConnectionOptions::class.java )
                .newInstance(connection, logger, exchangeName, options)
        } else {
            Subscriber(connection, logger, exchangeName, options)
        }

        subscribers[exchangeName] = subscriber

        return subscriber
    }

    fun getQueueClient(queueName: String, overrideClass: Class<out QueueClient>? = QueueClient::class.java): QueueClient {
        if (queueClients.contains(queueName)) return queueClients.getValue(queueName)

        val queueClient = if (overrideClass != null) {
            overrideClass
                .getConstructor(QueueConnection::class.java, Logger::class.java, String::class.java)
                .newInstance(connection, logger, queueName)
        } else {
            QueueClient(connection, logger, queueName)
        }

        queueClients[queueName] = queueClient

        return queueClient
    }

    fun getQueueServer(queueName: String, overrideClass: Class<out QueueServer>? = QueueServer::class.java, options: ConnectionOptions = object: ConnectionOptions{}): QueueServer {
        if (queueServers.contains(queueName)) return queueServers.getValue(queueName)

        val queueServer = if (overrideClass!= null) {
            overrideClass
                .getConstructor(QueueConnection::class.java, Logger::class.java, String::class.java, ConnectionOptions::class.java)
                .newInstance(connection, logger, queueName, options)
        } else {
            QueueServer(connection, logger, queueName, options)
        }

        queueServers[queueName] = queueServer

        return queueServer
    }
}
