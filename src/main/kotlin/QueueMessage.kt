import java.nio.Buffer

interface QueueMessage {
    val action: String
    val status: String
    val data: String
    val attachments: MutableMap<String, String>

    fun addAttachment(name: String, buffer: Buffer) {
        attachments[name] = buffer.toString()
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