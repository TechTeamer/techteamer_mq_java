package com.facekom.mq

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.sun.net.httpserver.Authenticator.Success

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
            logger.warn("RabbitMq connection already established")
            return
        }

        if (config.urls.size == 0) { //backwards compatibility - add url object as url string...
            config.url(QueueConfig.getUrl(config))
        }

        connected = connectToUrls(config.urls)

        if (!connected) {
            throw Exception("All connection attempts failed!")
        }
    }

    private fun connectToUrls(urls: MutableList<String>): Boolean {
        for (url in urls) {
            val logUrl = QueueConfig.stripCredentialsFromUrl(url) // Don't log credentials...

            try {
                val match = "(^amqps?):.*".toRegex().find(url) ?: throw Exception("Cannot identify the proper protocol from url.")

                val (protocol) = match.destructured

                if (protocol == "amqps") {
                    val sslContext = config.options.getSSLContext()
                    factory.useSslProtocol(sslContext)
                }

                factory.setUri(url)
                factory.isAutomaticRecoveryEnabled = config.options.automaticRecoveryEnabled

                connection = factory.newConnection()
                logger.info("RabbitMq connection established to $logUrl")
                return true
            } catch (error: Exception) {
                logger.error("Failed to connect to $logUrl $error")
            }
        }

        return false
    }
}
