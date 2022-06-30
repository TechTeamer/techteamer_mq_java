package com.facekom.mq_kotlin

import com.google.gson.JsonElement
import com.google.gson.JsonPrimitive
import com.rabbitmq.client.*
import org.slf4j.Logger
import java.util.UUID

open class RPCClient constructor(
    open val connection: QueueConnection,
    open val rpcName: String,
    open val logger: Logger,
    open val options: RpcClientOptions
) {
    private lateinit var client: RpcClient
    private val correlationIdList = mutableListOf<String>()
    var replyQueueName: String = ""
    var initialized: Boolean = false

    fun initialize() {
        if (initialized) {
            logger.warn("RPCClient already initialized queue($rpcName)")
            return
        }

        try {
            val channel = connection.getChannel()
            _getReplyQueue(connection.getChannel())

            val rpcOptions = RpcClientParams()
            rpcOptions.channel(channel)
            rpcOptions.exchange("")
            rpcOptions.routingKey(rpcName)
            rpcOptions.replyTo(replyQueueName)
            client = RpcClient(rpcOptions)
            logger.warn("RPCClient initialized queue($rpcName)")
            initialized = true
        } catch (error: Exception) {
            logger.error("CANNOT INITIALIZE RPCClient queue($rpcName) $error")
            throw error
        }
    }

    fun _getReplyQueue(channel: Channel) {
        replyQueueName = options.replyQueueName

        if (options.replyQueue.assert) {
            val durableReplyQueue = options.replyQueue.durable
            val exclusiveReplyQueue = options.replyQueue.exclusive
            val autoDeleteReplyQueue = options.replyQueue.autoDelete

            replyQueueName = channel.queueDeclare(replyQueueName, durableReplyQueue, exclusiveReplyQueue, autoDeleteReplyQueue, null).queue
            logger.info("RPCClient initialized reply queue($replyQueueName) durable($durableReplyQueue) exclusive($exclusiveReplyQueue) autoDelete($autoDeleteReplyQueue)")

        } else {
            logger.info("RPCClient initialize reply queue($replyQueueName) skipped assertion")
        }
    }

    open fun <T>callAction(
        action: QueueAction<T>,
        timeOutMs: Int?,
        attachments: MutableMap<String, ByteArray>?
    ): QueueMessage? {
        return call(action.toJSON(), timeOutMs, attachments)
    }

    open fun callSimpleAction(
        action: String,
        data: String,
        timeOutMs: Int?,
        attachments: MutableMap<String, ByteArray>?
    ): QueueMessage? {
        return callAction(SimpleStringAction(action, data), timeOutMs, attachments)
    }

    open fun call(
        message: String,
        timeOutMs: Int? = null,
        attachments: MutableMap<String, ByteArray>? = null
    ) : QueueMessage? {
        return call(JsonPrimitive(message), timeOutMs, attachments)
    }

    open fun call(
        message: JsonElement,
        timeOutMs: Int? = null,
        attachments: MutableMap<String, ByteArray>? = null
    ): QueueMessage? {
        var correlationId: String

        do {
            correlationId = UUID.randomUUID().toString()
        } while (correlationIdList.contains(correlationId))

        try {
            if (correlationIdList.size > options.queueMaxSize) {
                throw Exception("RPCCLIENT QUEUE FULL $rpcName")
            }

            val param = QueueMessage("ok", message)

            attachments?.forEach { t ->
                param.addAttachment(t.key, t.value)
            }

            correlationIdList.add(correlationId)

            val props = AMQP.BasicProperties.Builder()
            props.correlationId(correlationId)

            var timeOut: Int = options.timeOutMs

            if (timeOutMs != null) {
                timeOut = timeOutMs
            }

            val answer = timeOut.let {
                client.primitiveCall(props.build(), param.serialize(), timeOut)
            }

            correlationIdList.remove(correlationId)

            return QueueMessage.unserialize(answer)
        } catch (err: Exception) {
            correlationIdList.remove(correlationId)
            logger.error("RPC CLIENT: cannot make rpc call", err)
            throw Exception("RPC CLIENT: cannot make rpc call", err)
        }
    }

    fun getClient(): RpcClient {
        return client
    }
}
