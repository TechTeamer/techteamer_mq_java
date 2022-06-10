import com.rabbitmq.client.AMQP.BasicProperties
import org.slf4j.Logger

open class Publisher(
    open val queueConnection: QueueConnection, open val logger: Logger, open val exchange: String
) {

    open fun initialize() {
        val channel = queueConnection.getChannel()
        channel.exchangeDeclare(exchange, "fanout", true)
    }

    open fun sendAction(
        action: String,
        data: MutableMap<String, Any?>,
        correlationId: String? = null,
        timeOut: Int? = null,
        attachments: MutableMap<String, ByteArray> = mutableMapOf()
    ) {
        send(mutableMapOf("action" to action, "data" to data), correlationId, timeOut, attachments)
    }

    open fun send(
        message: MutableMap<String, Any?>,
        correlationId: String? = null,
        timeOut: Int? = null,
        attachments: MutableMap<String, ByteArray> = mutableMapOf()
    ) {
        var props = BasicProperties.Builder()

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

            channel.basicPublish(exchange, "", props.build(), param.serialize())
        } catch (error: java.lang.Exception) {
            logger.error("CANNOT PUBLISH MESSAGE, $exchange, $error")
        }
    }
}
