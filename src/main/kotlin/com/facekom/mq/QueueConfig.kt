package com.facekom.mq

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class QueueConfig {
    var url: String = "amqps://localhost:5672"
    var options: RabbitMqOptions = RabbitMqOptions()
    var logger: Logger = LoggerFactory.getLogger("mq")
    var rpcTimeoutMs: Int = 10000
    var rpcQueueMaxSize: Int = 100

    fun url (value: String): QueueConfig {
        url = value
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
        if (url == "") return false
        if (!options.isValid()) return false
        return true
    }
}
