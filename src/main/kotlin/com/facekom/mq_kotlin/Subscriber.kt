package com.facekom.mq_kotlin

import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.*
import org.slf4j.Logger

open class Subscriber(
    open var connection: QueueConnection,
    open var logger: Logger,
    open val name: String,
    open val options: ConnectionOptions
) {
    open val retryMap = mutableMapOf<String, Int>()
    open val actions = mutableMapOf<String, (
        Any,
        data: MutableMap<String, Any?>,
        props: BasicProperties,
        request: QueueMessage,
        delivery: Delivery
    ) -> Any>()

    fun setLogger(loggerInput: Any) {
        logger = loggerInput as Logger
    }

    open fun initialize() {
        try {
            val channel = connection.getChannel()
            channel.exchangeDeclare(name, "fanout", true)
            val queueName = channel.queueDeclare("", true, true, false, null)?.queue;
            channel.queueBind(queueName, name, "")
            channel.basicConsume(queueName, false, deliverCallback) { _: String? -> } // consumerTag parameter
        } catch (error: Exception) {
            logger.error("CANNOT INITIALIZE SUBSCRIBER $error")
        }
    }

    open val deliverCallback = DeliverCallback { consumerTag: String?, delivery: Delivery ->
        val channel = connection.getChannel()
        processMessage(channel, delivery, consumerTag)
    }

    open fun processMessage(channel: Channel, delivery: Delivery, consumerTag: String?): Any? =
        CoroutineScope(Dispatchers.IO).launch {
            val request = unserialize(delivery.body)

            if (request.status != "ok") {
                ack(channel, delivery)
                return@launch
            }

            if (consumerTag != null) {
                var counter = 1
                if (retryMap[consumerTag] != null) {
                    counter += retryMap[consumerTag]!!
                    retryMap[consumerTag] = counter
                } else {
                    retryMap[consumerTag] = counter
                }

                if (options.maxRetry != null && delivery.envelope.isRedeliver && counter > options.maxRetry!!) {
                    logger.error("SUBSCRIBER TRIED TOO MANY TIMES $name, ${request.data}, deliveryTag: $consumerTag")
                    ack(channel, delivery)

                    retryMap.remove(consumerTag)
                    return@launch
                }
            }

            var timeOut: Int = options.timeOutMs

            if (request.timeOut != null) {
                timeOut = request.timeOut!!
            }

            try {
                withTimeout(timeOut.toLong()) {
                    request.data?.let { it -> callback(it, delivery.properties, request, delivery) }
                }
                ack(channel, delivery)
                retryMap.remove(consumerTag)
            } catch (e: Exception) {
                logger.error("ERROR BY SERVER SIDE $e")
                nack(channel, delivery)
            } finally {
                cancel()
            }
        }

    open suspend fun callback(
        data: MutableMap<String, Any?>,
        props: BasicProperties,
        request: QueueMessage,
        delivery: Delivery
    ): Any? {
        if (actions[data["action"]] != null) {
            return actions[data["action"]]?.invoke(this, data, props, request, delivery)
        }
        return null
    }

    open fun registerAction(
        action: String, handler: (
            Any,
            data: MutableMap<String, Any?>,
            props: BasicProperties,
            request: QueueMessage,
            delivery: Delivery
        ) -> Any
    ) {
        if (actions[action] != null) {
            logger.warn("Actions-handlers map already contains an action named $action")
        } else {
            actions[action] = handler
        }
    }

    open fun ack(channel: Channel, delivery: Delivery) {
        channel.basicAck(delivery.envelope.deliveryTag, false)
    }

    open fun nack(channel: Channel, delivery: Delivery) {
        channel.basicNack(delivery.envelope.deliveryTag, false, true)
    }

}

