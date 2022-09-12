package com.facekom.mq

import org.slf4j.Logger

open class QueueClient(
    override val queueConnection: QueueConnection,
    override val logger: Logger,
    val queueName: String,
    val options: QueueClientOptions = QueueClientOptions()
) : AbstractPublisher(queueConnection, logger, "", queueName) {

    override fun initialize() {
        if (initialized) {
            logger.warn("QueueClient already initialized queue($queueName)")
            return
        }

        val durableQueue = options.queue.durable
        val exclusiveQueue = options.queue.exclusive
        val autoDeleteQueue = options.queue.autoDelete

        try {
            val channel = queueConnection.getChannel()

            if (options.queue.assert) {
                channel.queueDeclare(queueName, durableQueue, exclusiveQueue, options.queue.autoDelete, null)
                logger.info("QueueClient initialized queue($queueName) durable($durableQueue) exclusive($exclusiveQueue) autoDelete($autoDeleteQueue)")
            } else {
                logger.info("QueueClient initialize queue($queueName) skipped assertion")
            }

            initialized = true
        } catch (error: Exception) {
            logger.error("CANNOT INITIALIZE QueueClient queue($queueName) durable($durableQueue) exclusive($exclusiveQueue) autoDelete($autoDeleteQueue) $error")
            throw error
        }
    }
}
