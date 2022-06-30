package com.facekom.mq_kotlin

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.rabbitmq.client.AMQP.BasicProperties
import org.slf4j.Logger

abstract class AbstractPublisher(
    open val queueConnection: QueueConnection,
    open val logger: Logger,
    open val exchangeName: String,
    open val routingKey: String
) {
    open var initialized: Boolean = false

    abstract fun initialize()

    open fun <T>sendAction(
        action: QueueAction<T>,
        correlationId: String? = null,
        timeOut: Int? = null,
        attachments: MutableMap<String, ByteArray> = mutableMapOf()
    ) {
        send(action.toJSON(), correlationId, timeOut, attachments)
    }

    open fun sendSimpleAction(
        action: String,
        data: String,
        correlationId: String? = null,
        timeOut: Int? = null,
        attachments: MutableMap<String, ByteArray> = mutableMapOf()
    ) {
        sendAction(SimpleStringAction(action, data), correlationId, timeOut, attachments)
    }

    open fun send(
        message: String,
        correlationId: String? = null,
        timeOut: Int? = null,
        attachments: MutableMap<String, ByteArray> = mutableMapOf()
    ) {
        send(JsonPrimitive(message), correlationId, timeOut, attachments)
    }

    open fun send(
        message: JsonElement,
        correlationId: String? = null,
        timeOut: Int? = null,
        attachments: MutableMap<String, ByteArray> = mutableMapOf()
    ) {
        val props = BasicProperties.Builder()

        try {
            val channel = queueConnection.getChannel()
            val param = QueueMessage("ok", message, timeOut)
            if (correlationId != null) {
                props.correlationId(correlationId)
            }
            if (attachments.isNotEmpty()) {
                attachments.forEach { t ->
                    param.addAttachment(t.key, t.value)
                }
            }

            channel.basicPublish(exchangeName, routingKey, props.build(), param.serialize())
        } catch (error: Exception) {
            logger.error("CANNOT PUBLISH MESSAGE, $exchangeName, $error")
        }
    }
}
