package com.facekom.mq

import org.slf4j.Logger

open class Subscriber(
    override var connection: QueueConnection,
    override var logger: Logger,
    override val exchangeName: String,
    open val options: SubscriberOptions = SubscriberOptions()
) : AbstractSubscriber(connection, logger, exchangeName, options.connection) {

    override fun initialize() {
        if (initialized) {
            logger.warn("Subscriber already initialized exchange($exchangeName)")
            return
        }

        val assert = options.exchange.assert
        val durable = options.exchange.durable
        val autoDeleteExchange = options.exchange.autoDelete

        try {
            val channel = connection.getChannel()

            if (assert) {
                channel.exchangeDeclare(exchangeName, "fanout", durable, autoDeleteExchange, options.exchange.arguments)
                logger.info("Subscriber initialized exchange($exchangeName) durable($durable) autoDelete($autoDeleteExchange)")
            } else {
                logger.info("Subscriber initialize exchange($exchangeName) skipped assertion")
            }

            val queueName = channel.queueDeclare(
                "",
                options.queue.durable,
                options.queue.exclusive,
                options.queue.autoDelete,
                null
            ).queue
            channel.queueBind(queueName, exchangeName, "")
            channel.basicConsume(queueName, false, ::onConsumeMessage) { _: String? -> } // consumerTag parameter
            initialized = true
        } catch (error: Exception) {
            logger.error("CANNOT INITIALIZE SUBSCRIBER exchange($exchangeName) assert($assert) durable($durable) autoDelete($autoDeleteExchange) $error")
            throw error
        }
    }
}
