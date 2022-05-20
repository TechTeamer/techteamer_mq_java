import com.rabbitmq.client.*
import org.slf4j.Logger

open class RPCServer(open val ch: Channel, open val name: String, val logger: Logger) : RpcServer(ch, name) {

    override fun handleCall(requestBody: ByteArray, replyProperties: AMQP.BasicProperties?): ByteArray {
        val message: QueueMessage? = unserialize(requestBody)

        val response = QueueResponse()

        val answer = callback(requestBody, response, message)

        val replyAttachments = response.attachments

        var reply: QueueMessage? = null

        return try {
            answer.remove("response")
            reply = QueueMessage("ok", answer)
            replyAttachments.forEach { t ->
                reply.addAttachment(t.key, t.value)
            }
            reply.serialize()
        } catch (err: Exception) {
            QueueMessage("error", mutableMapOf("message" to "cannot encode answer")).serialize()
        }

    }

    open fun callback(
        requestBody: ByteArray, response: QueueResponse, message: QueueMessage?
    ): MutableMap<String, Any?> {
        return mutableMapOf("test" to "test", "valami" to "valami,", "igen" to 20, "response" to response)
    }


    fun initialize() {
        this.logger.info("RPC server mainloop started - $name")
        mainloop()
    }
}