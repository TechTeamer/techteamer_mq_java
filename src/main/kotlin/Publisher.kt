import com.rabbitmq.client.AMQP
import com.rabbitmq.client.AMQP.BasicProperties
import mu.KLogger

open class Publisher(open val queueConnection: QueueConnection, open val logger: KLogger, val exchange: String) {
    open val channel = queueConnection.getChannel()

    open fun initialize() {
        channel.exchangeDeclare(exchange, "fanout", true)
    }

    open fun sendAction(
        action: String,
        data: MutableMap<String, Any?>,
        correlationId: String?,
        timeOut: Long?,
        attachments: MutableMap<String, ByteArray>
    ) {
        send(mutableMapOf("action" to action, "data" to data), correlationId, timeOut, attachments)
    }

    open fun send(
        message: MutableMap<String, Any?>,
        correlationId: String?,
        timeOut: Long?,
        attachments: MutableMap<String, ByteArray> = mutableMapOf()
    ) {
        var props = BasicProperties.Builder()

        try {
            val param = QueueMessage("ok", message)
            if (correlationId != null) {
                props.correlationId(correlationId)
            }
            if (attachments.isNotEmpty()) {
                attachments.forEach { t ->
                    param.addAttachment(t.key, t.value)
                }
            }
            channel.basicPublish(exchange, "", props.build(), param.serialize())
        } catch (error: java.lang.Exception) {
            logger.error("CANNOT PUBLISH MESSAGE, $exchange, $error")
        }
    }
}