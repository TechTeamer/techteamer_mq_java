package com.facekom.mq_kotlin

import com.rabbitmq.client.*
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
        }
    }

    override fun handleCall(delivery: Delivery, replyProperties: AMQP.BasicProperties?): ByteArray {
        val message: QueueMessage = unserialize(delivery.body)
        val response = QueueResponse()

        var answer: MutableMap<String, Any?>? = null
        var timedOut = false

        var timeOut: Int = options.timeOutMs

        if (message.timeOut != null) {
            timeOut = message.timeOut!!
        }
        runBlocking {
            try {
                withTimeout(timeOut.toLong()) {
                    answer = rpcServer.callback(message, delivery, response)
                }
            } catch (e: Exception) {
                logger.error("TIMEOUT ${e.message}")
                timedOut = true
            }
        }

        if (timedOut) {
            return QueueMessage("error", mutableMapOf("error" to "timeout")).serialize()
        }

        val replyAttachments = response.attachments

        return try {
            val reply: QueueMessage?

            if (answer != null) {
                reply = QueueMessage("ok", answer)
                replyAttachments.forEach { t ->
                    reply.addAttachment(t.key, t.value)
                }

                reply.serialize()
            } else {
                QueueMessage("ok", mutableMapOf("message" to "no reply generated")).serialize()
            }

        } catch (err: Exception) {
            QueueMessage("error", mutableMapOf("message" to "cannot encode answer")).serialize()
        }

    }
}

open class RPCServer(
    val connection: QueueConnection,
    val name: String,
    val logger: Logger,
    open val options: RpcServerOptions
) {
    open val actions = mutableMapOf<String, (
        Any,
        data: MutableMap<String, Any?>,
        delivery: Delivery,
        response: QueueResponse,
        request: QueueMessage
    ) -> MutableMap<String, Any?>?>()

    fun initialize() {
        val channel = connection.getChannel()
        channel.exchangeDeclare(name, "direct", true)
        channel.queueDeclare(name, true, false, false, null)
        channel.queueBind(name, name, "$name-key")
        channel.basicQos(options.prefetchCount)

        val server = RPCServerOverride(
            connection,
            name,
            logger,
            options,
            this
        )

        server.mainloop()
    }

    open fun registerAction(
        action: String,
        handler: (
            Any,
            data: MutableMap<String, Any?>,
            delivery: Delivery,
            response: QueueResponse,
            request: QueueMessage
        ) -> MutableMap<String, Any?>?
    ) {
        if (actions[action] != null) {
            logger.warn("Actions-handlers map already contains an action named $action")
        } else {
            actions[action] = handler
        }
    }

    open suspend fun callback(
        data: QueueMessage,
        delivery: Delivery,
        response: QueueResponse,
    ): MutableMap<String, Any?>? = run {
        if ((data.data?.get("action") != null) && (actions[data.data["action"]] != null)) {
            return actions[data.data["action"]]!!.invoke(this, data.data, delivery, response, data)
        }
        return mutableMapOf("data" to null)
    }

}
