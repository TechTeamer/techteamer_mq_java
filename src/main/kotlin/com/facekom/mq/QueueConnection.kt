package com.facekom.mq

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class QueueConnection(private val config: QueueConfig) {
    private var factory = ConnectionFactory()
    private lateinit var connection: Connection

    var myChannel: Channel? = null
    var logger = config.logger
    var connected: Boolean = false

    fun getChannel(): Channel {
        if (myChannel != null) return myChannel!!
        myChannel = connection.createChannel()
        return myChannel!!
    }

    fun connect() {
        if (connected) {
            logger.info("RabbitMq connection already established to ${config.hostname ?: config.url}")
            return
        }

        if (config.protocol == "amqps") {
            val sslContext = config.options.getSSLContext()
            factory.useSslProtocol(sslContext)
        }

        if (config.options.userName != null && config.options.password != null) {
            factory.username = config.options.userName
            factory.password = config.options.password
        }

        if (config.hostname == null && config.port == null) {
            factory.setUri(config.url)
        } else {
            factory.host = config.hostname
            factory.port = config.port!!
        }

        factory.virtualHost = config.options.vhost ?: ConnectionFactory.DEFAULT_VHOST

        connection = factory.newConnection()
        connected = true
        logger.info("RabbitMq connection established to ${config.hostname ?: config.url}")
    }

}
