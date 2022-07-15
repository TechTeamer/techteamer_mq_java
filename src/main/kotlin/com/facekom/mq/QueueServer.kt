package com.facekom.mq

import org.slf4j.Logger

open class QueueServer(
    open val queueConnection: QueueConnection,
    override var logger: Logger,
    override val exchangeName: String,
    open val options: QueueServerOptions
) : AbstractSubscriber(queueConnection, logger, exchangeName, options.connection) {

    override fun initialize() {
        if (initialized) {
            logger.warn("QueueServer already initialized queue($exchangeName)")
            return
        }

        val durableQueue = options.queue.durable
        val exclusiveQueue = options.queue.exclusive
        val autoDeleteQueue = options.queue.autoDelete

        try {
            val channel = queueConnection.getChannel()
            val prefetchCount = options.connection.prefetchCount

            if (options.queue.assert) {
                channel.queueDeclare(exchangeName, durableQueue, exclusiveQueue, autoDeleteQueue, null)
                logger.info("QueueServer initialized queue($exchangeName) durable($durableQueue) exclusive($exclusiveQueue) autoDelete($autoDeleteQueue)")

            } else {
                logger.info("QueueServer initialize queue($exchangeName) skipped assertion")
            }

            if (prefetchCount != null) {
                channel.basicQos(prefetchCount)
            }

            channel.basicConsume(exchangeName, false, ::onConsumeMessage) { _: String? -> } // consumerTag parameter
            initialized = true
        } catch (error: Exception) {
            logger.error("CANNOT INITIALIZE QueueServer queue($exchangeName) durable($durableQueue) exclusive($exclusiveQueue) autoDelete($autoDeleteQueue) $error")
            throw error
        }
    }
}
