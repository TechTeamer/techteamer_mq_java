package com.facekom.mq_kotlin

import org.slf4j.Logger

open class QueueClient(
    override val queueConnection: QueueConnection,
    override val logger: Logger,
    open val name: String
    ) : Publisher(queueConnection, logger, name) {

    override fun initialize() {
        val channel = queueConnection.getChannel()
        channel.exchangeDeclare(name, "direct", true)
        channel.queueDeclare(name, true, false, false, null)
        channel.queueBind(name, name, "")
    }
}
