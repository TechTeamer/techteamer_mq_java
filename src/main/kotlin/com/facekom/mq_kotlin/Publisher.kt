package com.facekom.mq_kotlin

import org.slf4j.Logger

open class Publisher(
    override val queueConnection: QueueConnection,
    override val logger: Logger,
    override val exchangeName: String,
    open val options: PublisherOptions = PublisherOptions()
) : AbstractPublisher(queueConnection, logger, exchangeName, "") {

    override fun initialize() {
        if (initialized) {
            logger.warn("Publisher already initialized exchange($exchangeName)")
            return
        }

        val durable = options.exchange.durable
        val autoDeleteExchange = options.exchange.autoDelete

        try {
            if (options.exchange.assert) {
                val channel = queueConnection.getChannel()
                channel.exchangeDeclare(exchangeName, "fanout", durable, autoDeleteExchange, options.exchange.arguments)
                logger.info("Publisher initialized exchange($exchangeName) durable($durable) autoDelete($autoDeleteExchange)")
            } else {
                logger.info("Publisher initialize exchange($exchangeName) skipped assertion")
            }

            initialized = true
        } catch (error: Exception) {
            logger.error("CANNOT INITIALIZE PUBLISHER exchange($exchangeName) durable($durable) autoDelete($autoDeleteExchange) $error")
            throw error
        }
    }
}
