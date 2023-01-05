package com.facekom.mq

import com.google.gson.JsonElement
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.Delivery
import kotlinx.coroutines.*
import org.slf4j.Logger

abstract class AbstractSubscriber(
    open var connection: QueueConnection,
    open var logger: Logger,
    open val exchangeName: String,
    private val connectionOptions: ConnectionOptions = ConnectionOptions()
) {
    open val retryMap = mutableMapOf<String, Int>()
    open val actions = mutableMapOf<String, QueueHandler>()
    var initialized: Boolean = false
    var _callback: QueueHandler? = null

    fun setLogger(loggerInput: Any) {
        logger = loggerInput as Logger
    }

    abstract fun initialize()

    fun onConsumeMessage(consumerTag: String?, delivery: Delivery) {
        val channel = connection.getChannel()
        processMessage(channel, delivery, consumerTag)
    }

    private fun parseMessage(delivery: Delivery): QueueMessage? {
        try {
            val request = QueueMessage.unserialize(delivery.body)

            if (request.status != "ok") {
                logger.error("CANNOT GET QUEUE MESSAGE PARAMS", exchangeName, request.data)
                return null
            }

            return request
        } catch (e: Exception) {
            return null
        }
    }

    private fun handleMessageRetry(delivery: Delivery, request: QueueMessage, consumerTag: String?): Boolean {
        if (!delivery.envelope.isRedeliver || consumerTag == null) {
            return false
        }

        var counter = 1
        if (retryMap[consumerTag] != null) {
            counter = retryMap[consumerTag]!! + 1
            retryMap[consumerTag] = counter
        } else {
            retryMap[consumerTag] = counter
        }
        val maxRetry = connectionOptions.maxRetry
        if (counter > maxRetry) {
            logger.error("SUBSCRIBER TRIED TOO MANY TIMES $exchangeName, $counter > $maxRetry ${request.data}, deliveryTag: $consumerTag")
            retryMap.remove(consumerTag)
            return true
        }

        return false
    }

    open fun processMessage(
        channel: Channel,
        delivery: Delivery,
        consumerTag: String?
    ): Any? = CoroutineScope(Dispatchers.IO).launch {
        val request = parseMessage(delivery)

        if (request == null) {
            ack(channel, delivery)
            return@launch
        }

        val triedTooManyTimes = handleMessageRetry(delivery, request, consumerTag)

        if (triedTooManyTimes) {
            ack(channel, delivery)
            return@launch
        }

        var timeOut: Int = connectionOptions.timeOutMs

        if (request.timeOut != null) {
            timeOut = request.timeOut!!
        }

        runBlocking {
            try {
                withTimeout(timeOut.toLong()) {
                    callback(request.data, delivery.properties, request, delivery)
                }

                ack(channel, delivery)
                retryMap.remove(consumerTag)
            } catch (e: TimeoutCancellationException) {
                logger.error("Timeout in Subscriber $e")
                nack(channel, delivery)
            } catch (e: Exception) {
                logger.error("Error processing incoming message $e")
                nack(channel, delivery)
            } finally {
                cancel()
            }
        }
    }

    open suspend fun callback(
        data: JsonElement?,
        props: BasicProperties,
        request: QueueMessage,
        delivery: Delivery
    ): Any? {
        var handler: QueueHandler? = getActionHandlerForMessage(data)
        var messageBody = data

        if (handler != null) {
            // get message body for action
            messageBody = data?.asJsonObject?.get("data")
        }

        if (handler == null && _callback != null) {
            handler = _callback
        }

        handler?.invoke(messageBody, props, request, delivery)

        return null
    }

    private fun getActionHandlerForMessage(data: JsonElement?): QueueHandler? {
        var handler: QueueHandler? = null

        // handle action command received in message data
        if (data != null && data.isJsonObject && data.asJsonObject.has("action")) {
            val actionEl = data.asJsonObject.get("action")
            if (actionEl.isJsonPrimitive && actionEl.asJsonPrimitive.isString) {
                handler = actions.get(actionEl.asJsonPrimitive.asString)
            }
        }

        return handler
    }

    fun consume(callback: QueueHandler) {
        _callback = callback
    }

    open fun registerAction(action: String, handler: QueueHandler) {
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
