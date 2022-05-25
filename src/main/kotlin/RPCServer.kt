import com.rabbitmq.client.*
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.slf4j.Logger

open class RPCServer(
    open val ch: Channel,
    open val name: String,
    val logger: Logger,
    val options: RpcServerOptions = object : RpcServerOptions {
        override val timeOutMs: Int = 10000
        override val prefetchCount: Int = 1
    }
) : RpcServer(ch, name) {

    open val actions = mutableMapOf<String, (
        Any,
        msg: MutableMap<String, Any?>,
        delivery: Delivery,
        response: QueueResponse,
        data: QueueMessage
    ) -> MutableMap<String, Any?>>()

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
                    answer = callback(message, delivery, response)
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
            var reply: QueueMessage? = null

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

    open fun callback(
        data: QueueMessage,
        delivery: Delivery,
        response: QueueResponse,
    ): MutableMap<String, Any?> {
        if ((data.data?.get("action") != null) && (actions[data.data["action"]] != null)) {
            return actions[data.data["action"]]!!.invoke(this, data.data, delivery, response, data)
        }
        return mutableMapOf("data" to null)
    }

    open fun registerAction(
        action: String, handler: (
            Any,
            msg: MutableMap<String, Any?>,
            delivery: Delivery,
            response: QueueResponse,
            data: QueueMessage
        ) -> MutableMap<String, Any?>
    ) {
        if (actions[action] != null) {
            logger.warn("Actions-handlers map already contains an action named $action")
        } else {
            actions[action] = handler
        }
    }


    fun initialize() {
        this.logger.info("RPC server mainloop started - $name")
        mainloop()
    }
}