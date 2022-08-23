package com.facekom.mq

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class QueueConfig {
    var url: String? = null
    var protocol: String = ConnectionProtocol.AMQP.protocol
    var hostname: String? = null
    var port: Int? = null
    var options: RabbitMqOptions = RabbitMqOptions()
    var logger: Logger = LoggerFactory.getLogger("mq")
    var rpcTimeoutMs: Int = 10000
    var rpcQueueMaxSize: Int = 100

    fun url (value: String): QueueConfig {
        url = value
        return this
    }
    fun hostname (value: String): QueueConfig {
        hostname = value
        return this
    }

    fun protocol (value: ConnectionProtocol): QueueConfig {
        protocol = value.protocol
        return this
    }

    fun port (value: Int): QueueConfig {
        port = value
        return this
    }

    fun options (value: RabbitMqOptions): QueueConfig {
        options = value
        return this
    }

    fun logger (value: Logger): QueueConfig {
        logger = value
        return this
    }

    fun rpcTimeoutMs (value: Int): QueueConfig {
        rpcTimeoutMs = value
        return this
    }

    fun rpcQueueMaxSize (value: Int): QueueConfig {
        rpcQueueMaxSize = value
        return this
    }

    fun isValid () :Boolean {
        if (hostname == "") return false
        if (!options.isValid()) return false
        return true
    }
}

enum class ConnectionProtocol(val protocol: String) {
    AMQP("amqp"),
    AMQPS("amqps")
}
