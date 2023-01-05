package com.facekom.mq

import com.google.gson.JsonElement
import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Delivery
import com.rabbitmq.client.RpcServer
import kotlinx.coroutines.*
import org.slf4j.Logger

open class RPCServerOverride(
    val connection: QueueConnection,
    val name: String,
    val logger: Logger,
    open val options: RpcServerOptions,
    private val rpcServer: RPCServer
) : RpcServer(connection.getChannel(), name) {

    override fun processRequest(request: Delivery) {
        CoroutineScope(Dispatchers.IO).launch {
            super.processRequest(request)
            cancel()
        }
    }

    override fun handleCall(delivery: Delivery, replyProperties: AMQP.BasicProperties?): ByteArray {
        val message: QueueMessage = QueueMessage.unserialize(delivery.body)
        val response = QueueResponse()

        var answer: JsonElement? = null
        var timedOut = false
        var errored = false

        var timeOut: Int = options.timeOutMs

        if (message.timeOut != null) {
            timeOut = message.timeOut!!
        }

        runBlocking {
            try {
                withTimeout(timeOut.toLong()) {
                    answer = rpcServer.callback(message.data, message, delivery, response)
                }
            } catch (e: TimeoutCancellationException) {
                logger.error("Timeout in Rpc server $e")
                timedOut = true
            } catch (e: Exception) {
                logger.error("ERROR ${e.message}")
                errored = true
            }
        }

        if (timedOut) {
            return QueueMessage.createErrorMessage("timeout").serialize()
        }

        if (errored) {
            return QueueMessage.createErrorMessage("error during handle rpc request").serialize()
        }

        val replyAttachments = response.attachments

        return try {
            val reply = QueueMessage("ok", answer)

            replyAttachments.forEach { t ->
                reply.addAttachment(t.key, t.value)
            }

            reply.serialize()
        } catch (err: Exception) {
            QueueMessage.createErrorMessage("cannot encode answer").serialize()
        }
    }
}

open class RPCServer(
    val connection: QueueConnection,
    val name: String,
    val logger: Logger,
    open val options: RpcServerOptions
) {
    var _callback: RpcHandler? = null
    open val actions = mutableMapOf<String, RpcHandler?>()
    var initialized: Boolean = false

    fun initialize() {
        if (initialized) {
            logger.warn("RPCServer already initialized queue($name)")
            return
        }

        val durableQueue = options.queue.durable
        val exclusiveQueue = options.queue.exclusive
        val autoDeleteQueue = options.queue.autoDelete

        try {
            val channel = connection.getChannel()

            if (options.queue.assert) {
                channel.queueDeclare(name, durableQueue, exclusiveQueue, autoDeleteQueue, null)
                logger.info("RPCServer initialized queue($name) durable($durableQueue) exclusive($exclusiveQueue) autoDelete($autoDeleteQueue)")
            } else {
                logger.info("RPCServer initialize queue($name) skipped assertion")
            }

            channel.basicQos(options.prefetchCount)

            val server = RPCServerOverride(
                connection,
                name,
                logger,
                options,
                this
            )

            CoroutineScope(Dispatchers.IO).launch {
                server.mainloop()
            }

            initialized = true
        } catch (error: Exception) {
            logger.error("CANNOT INITIALIZE RPCServer queue($name) durable($durableQueue) exclusive($exclusiveQueue) autoDelete($autoDeleteQueue) $error")
            throw error
        }
    }

    open fun registerAction(
        action: String,
        handler: RpcHandler
    ) {
        if (actions[action] != null) {
            logger.warn("Actions-handlers map already contains an action named $action")
        } else {
            actions[action] = handler
        }
    }

    fun consume(callback: RpcHandler) {
        _callback = callback
    }

    open suspend fun callback(
        data: JsonElement?,
        request: QueueMessage,
        delivery: Delivery,
        response: QueueResponse
    ): JsonElement? = run {
        var handler: RpcHandler? = getActionHandlerForMessage(data)
        var messageBody = data

        if (handler != null) {
            // get message body for action
            messageBody = data?.asJsonObject?.get("data")
        }

        if (handler == null && _callback != null) {
            handler = _callback
        }

        if (handler == null) {
            return null
        }

        return handler.invoke(messageBody, request, response, delivery)
    }

    private fun getActionHandlerForMessage(data: JsonElement?): RpcHandler? {
        var handler: RpcHandler? = null

        // handle action command received in message data
        if (data != null && data.isJsonObject && data.asJsonObject.has("action")) {
            val actionEl = data.asJsonObject.get("action")
            if (actionEl.isJsonPrimitive && actionEl.asJsonPrimitive.isString) {
                handler = actions.get(actionEl.asJsonPrimitive.asString)
            }
        }

        return handler
    }
}
