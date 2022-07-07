package com.facekom.mq_kotlin

import kotlinx.coroutines.*
import org.slf4j.Logger

class QueueManager(private val config: QueueConfig) {
    var connection: QueueConnection = QueueConnection(config)
    var logger = config.logger
    var connected: Boolean = false

    var rpcClients = mutableMapOf<String, RPCClient>()
    var rpcServers = mutableMapOf<String, RPCServer>()
    var publishers = mutableMapOf<String, Publisher>()
    var subscribers = mutableMapOf<String, Subscriber>()
    var queueServers = mutableMapOf<String, QueueServer>()
    var queueClients = mutableMapOf<String, QueueClient>()

    fun connect() {
        try {
            connection.connect()
        } catch (err: Exception) {
            logger.error("Failed to connect to queue server $err")
            throw err
        }

        try {
            rpcServers.forEach { _, rpcServer ->
                runBlocking {
                    rpcServer.initialize()
                    while (!rpcServer.initialized) {
                        delay(100)
                    }
                }
            }
            rpcClients.forEach { _, rpcClient ->
                rpcClient.initialize()
            }
            publishers.forEach { _, publisher ->
                publisher.initialize()
            }
            subscribers.forEach { _, subscriber ->
                subscriber.initialize()
            }
            queueServers.forEach { _, queueServer ->
                queueServer.initialize()
            }
            queueClients.forEach { _, queueClient ->
                queueClient.initialize()
            }



            logger.info("RabbitMq servers and clients initialize finished")
        } catch (err: Exception) {
            logger.error("Failed to initialize servers and clients", err)
        }
    }

    fun setLogger(loggerInput: Any) {
        connection.logger = loggerInput as Logger
    }

    fun getRPCClient(rpcName: String): RPCClient {
        return getRPCClient(rpcName, RPCClient::class.java)
    }

    fun getRPCClient(rpcName: String, options: RpcClientOptions): RPCClient {
        return getRPCClient(rpcName, RPCClient::class.java, options)
    }

    fun getRPCClient(rpcName: String, overrideClass: Class<out RPCClient>): RPCClient {
        val options = RpcClientOptions()
        options.queueMaxSize = config.rpcQueueMaxSize
        options.timeOutMs = config.rpcTimeoutMs
        return getRPCClient(rpcName, overrideClass, options)
    }

    fun getRPCClient(rpcName: String, overrideClass: Class<out RPCClient>, options: RpcClientOptions): RPCClient {
        if (rpcClients.contains(rpcName)) return rpcClients.getValue(rpcName)

        val rpcClient = overrideClass
            .getConstructor(
                QueueConnection::class.java,
                String::class.java,
                Logger::class.java,
                RpcClientOptions::class.java
            )
            .newInstance(connection, rpcName, logger, options)

        rpcClients[rpcName] = rpcClient

        return rpcClient
    }

    fun getRPCServer(rpcName: String): RPCServer {
        return getRPCServer(rpcName, RPCServer::class.java)
    }

    fun getRPCServer(rpcName: String, options: RpcServerOptions): RPCServer {
        return getRPCServer(rpcName, RPCServer::class.java, options)
    }

    fun getRPCServer(rpcName: String, overrideClass: Class<out RPCServer>): RPCServer {
        val options = RpcServerOptions()
        options.timeOutMs = config.rpcTimeoutMs
        return getRPCServer(rpcName, overrideClass, options)
    }

    fun getRPCServer(rpcName: String, overrideClass: Class<out RPCServer>, options: RpcServerOptions): RPCServer {
        if (rpcServers.contains(rpcName)) return rpcServers.getValue(rpcName)

        val rpcServer = overrideClass
            .getConstructor(
                QueueConnection::class.java,
                String::class.java,
                Logger::class.java,
                RpcServerOptions::class.java
            )
            .newInstance(connection, rpcName, logger, options)

        rpcServers[rpcName] = rpcServer

        return rpcServer
    }

    fun getPublisher(exchangeName: String): Publisher {
        return getPublisher(exchangeName, Publisher::class.java)
    }

    fun getPublisher(exchangeName: String, options: PublisherOptions): Publisher {
        return getPublisher(exchangeName, Publisher::class.java, options)
    }

    fun getPublisher(exchangeName: String, overrideClass: Class<out Publisher>): Publisher {
        val options = PublisherOptions()
        return getPublisher(exchangeName, overrideClass, options)
    }

    fun getPublisher(exchangeName: String, overrideClass: Class<out Publisher>, options: PublisherOptions): Publisher {
        if (publishers.contains(exchangeName)) return publishers.getValue(exchangeName)

        val publisher = overrideClass
            .getConstructor(
                QueueConnection::class.java,
                Logger::class.java,
                String::class.java,
                PublisherOptions::class.java
            )
            .newInstance(connection, logger, exchangeName, options)

        publishers[exchangeName] = publisher

        return publisher
    }

    fun getSubscriber(exchangeName: String): Subscriber {
        return getSubscriber(exchangeName, Subscriber::class.java)
    }

    fun getSubscriber(exchangeName: String, options: SubscriberOptions): Subscriber {
        return getSubscriber(exchangeName, Subscriber::class.java, options)
    }

    fun getSubscriber(exchangeName: String, overrideClass: Class<out Subscriber>): Subscriber {
        val options = SubscriberOptions()
        return getSubscriber(exchangeName, overrideClass, options)
    }

    fun getSubscriber(
        exchangeName: String,
        overrideClass: Class<out Subscriber>,
        options: SubscriberOptions
    ): Subscriber {
        if (subscribers.contains(exchangeName)) return subscribers.getValue(exchangeName)

        val subscriber = overrideClass
            .getConstructor(
                QueueConnection::class.java,
                Logger::class.java,
                String::class.java,
                SubscriberOptions::class.java
            )
            .newInstance(connection, logger, exchangeName, options)

        subscribers[exchangeName] = subscriber

        return subscriber
    }

    fun getQueueClient(queueName: String): QueueClient {
        return getQueueClient(queueName, QueueClient::class.java)
    }

    fun getQueueClient(queueName: String, options: QueueClientOptions): QueueClient {
        return getQueueClient(queueName, QueueClient::class.java, options)
    }

    fun getQueueClient(queueName: String, overrideClass: Class<out QueueClient>): QueueClient {
        val options = QueueClientOptions()
        return getQueueClient(queueName, overrideClass, options)
    }

    fun getQueueClient(
        queueName: String,
        overrideClass: Class<out QueueClient>,
        options: QueueClientOptions
    ): QueueClient {
        if (queueClients.contains(queueName)) return queueClients.getValue(queueName)

        val queueClient = overrideClass
            .getConstructor(
                QueueConnection::class.java,
                Logger::class.java,
                String::class.java,
                QueueClientOptions::class.java
            )
            .newInstance(connection, logger, queueName, options)

        queueClients[queueName] = queueClient

        return queueClient
    }

    fun getQueueServer(queueName: String): QueueServer {
        return getQueueServer(queueName, QueueServer::class.java)
    }

    fun getQueueServer(queueName: String, options: QueueServerOptions): QueueServer {
        return getQueueServer(queueName, QueueServer::class.java, options)
    }

    fun getQueueServer(queueName: String, overrideClass: Class<out QueueServer>): QueueServer {
        val options = QueueServerOptions()
        return getQueueServer(queueName, overrideClass, options)
    }

    fun getQueueServer(
        queueName: String,
        overrideClass: Class<out QueueServer>,
        options: QueueServerOptions
    ): QueueServer {
        if (queueServers.contains(queueName)) return queueServers.getValue(queueName)

        val queueServer = overrideClass
            .getConstructor(
                QueueConnection::class.java,
                Logger::class.java,
                String::class.java,
                QueueServerOptions::class.java
            )
            .newInstance(connection, logger, queueName, options)

        queueServers[queueName] = queueServer

        return queueServer
    }
}
