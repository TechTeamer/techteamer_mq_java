package com.facekom.mq

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class QueueConfig {
    var urls = mutableListOf<String>()
    var protocol: String = ConnectionProtocol.AMQP.protocol
    var hostname: String? = null
    var port: Int? = null
    var options: RabbitMqOptions = RabbitMqOptions()
    var logger: Logger = LoggerFactory.getLogger("mq")
    var rpcTimeoutMs: Int = 10000
    var rpcQueueMaxSize: Int = 100

    fun urls(value: MutableList<String>) {
        urls = value
    }

    fun url(value: String): QueueConfig {
        urls = mutableListOf(value)
        return this
    }

    fun hostname(value: String): QueueConfig {
        hostname = value
        return this
    }

    fun protocol(value: ConnectionProtocol): QueueConfig {
        protocol = value.protocol
        return this
    }

    fun port(value: Int): QueueConfig {
        port = value
        return this
    }

    fun options(value: RabbitMqOptions): QueueConfig {
        options = value
        return this
    }

    fun logger(value: Logger): QueueConfig {
        logger = value
        return this
    }

    fun rpcTimeoutMs(value: Int): QueueConfig {
        rpcTimeoutMs = value
        return this
    }

    fun rpcQueueMaxSize(value: Int): QueueConfig {
        rpcQueueMaxSize = value
        return this
    }

    fun isValid(): Boolean {
        if (hostname == "") return false
        if (!options.isValid()) return false
        return true
    }

    companion object {
        fun getUrl(config: QueueConfig): String {
            if (config.urls.size > 0) {
                return config.urls[0]
            }

            var credentials = ""
            var port = ""
            if (config.options.userName != null && config.options.password != null) {
                credentials = "${config.options.userName}:${config.options.password}@"
            }
            if (config.port != null) {
                port = ":${config.port}"
            }

            return "${config.protocol}://$credentials${config.hostname}$port/${config.options.vhost}"
        }

        fun stripCredentialsFromUrl(url: String): String {
            if (!url.contains("@")) {
                return url
            }

            val protocol=url.split("://")[0]
            val ending=url.split("@").last()

            return "$protocol://$ending"
        }
    }
}

enum class ConnectionProtocol(val protocol: String) {
    AMQP("amqp"),
    AMQPS("amqps")
}
