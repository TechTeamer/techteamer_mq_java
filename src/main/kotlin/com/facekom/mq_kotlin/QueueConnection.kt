package com.facekom.mq_kotlin

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
            logger.info("RabbitMq connection already established to ${config.url}")
            return
        }

        val sslContext = config.options.getSSLContext()
        if (sslContext != null) {
            factory.useSslProtocol(sslContext)
        }

        factory.setUri(config.url)
        connection = factory.newConnection(config.url)
        connected = true
        logger.info("RabbitMq connection established to ${config.url}")
    }

}
