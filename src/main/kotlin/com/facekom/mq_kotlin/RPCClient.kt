package com.facekom.mq_kotlin

import com.rabbitmq.client.AMQP
import com.rabbitmq.client.RpcClient
import com.rabbitmq.client.RpcClientParams
import org.slf4j.Logger
import java.util.UUID

open class RPCClient constructor(
    open val connection: QueueConnection,
    open val rpcName: String,
    open val logger: Logger,
    open val options: RpcOptions
) {

    private lateinit var client: RpcClient
    private val correlationIdList = mutableListOf<String>()


    fun initialize() {
        val channel = connection.getChannel()

        val queue = channel.queueDeclare("", true, false, true, null).queue

        val rpcOptions = RpcClientParams()
        rpcOptions.channel(channel)
        rpcOptions.exchange(rpcName)
        rpcOptions.routingKey("$rpcName-key")
        rpcOptions.replyTo(queue)
        client = RpcClient(rpcOptions)
    }

    open fun callAction(
        action: String, data: MutableMap<String, Any?>, timeOutMs: Int?, attachments: MutableMap<String, ByteArray>?
    ): QueueMessage? {
        return call(mutableMapOf("action" to action, "data" to data), timeOutMs, attachments)
    }

    open fun call(
        message: MutableMap<String, Any?>, timeOutMs: Int? = null, attachments: MutableMap<String, ByteArray>? = null
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

            val answer = timeOut.let { client.primitiveCall(props.build(), param.serialize(), timeOut) }

            correlationIdList.remove(correlationId)

            return unserialize(answer)
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