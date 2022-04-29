import java.nio.Buffer

interface QueueMessage {
    var action: String
    val status: String
    val data: String
    val attachments: MutableMap<String, Buffer>?

    fun addAttachment(name: String, buffer: Buffer) {
        this.attachments?.set(name, buffer)
    }

    fun serialize(message: QueueMessage): String {

        val myMessage = mutableMapOf(
            "status" to message.status,
            "data" to message.data,
            "action" to message.action,
            "attachments" to message.attachments
        )

        return myMessage.toString()
    }
}