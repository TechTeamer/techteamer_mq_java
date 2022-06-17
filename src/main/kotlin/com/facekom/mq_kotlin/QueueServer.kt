package com.facekom.mq_kotlin

import org.slf4j.Logger

open class QueueServer(
    open val queueConnection: QueueConnection,
    override var logger: Logger,
    override val name: String,
    override val options: ConnectionOptions
) : Subscriber(queueConnection, logger, name, options) {


    override fun initialize() {
        try {
            val channel = queueConnection.getChannel()
            val prefetchCount = options.prefetchCount
            channel.queueDeclare(name, true, false, false, null)
            if (prefetchCount != null) {
                channel.basicQos(prefetchCount)
            }
            channel.basicConsume(name, true, deliverCallback) { _: String? -> } // consumerTag parameter
        } catch (error: Exception) {
            logger.error("CANNOT INITIALIZE QUEUE SERVER $error")
        }
    }
}