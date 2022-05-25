import com.google.gson.Gson
import com.google.gson.JsonParser
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val gson = Gson()

class QueueMessage(
    val status: String,
    val data: MutableMap<String, Any?>?,
    var timeOut: Int? = null
) {
    val attachArray: MutableList<List<Any>> = mutableListOf()
    val attachments: MutableMap<String, ByteArray> = mutableMapOf()

    fun addAttachment(name: String, bytes: ByteArray) {
        attachments[name] = bytes
    }

    fun serialize(): ByteArray {
        val attachmentBuffers = mutableListOf<ByteArray>()
        val attachMap = mutableMapOf<String, Int>()
        val mapToJson = mutableMapOf("status" to status, "data" to data)

        attachments.forEach { entry ->
            attachmentBuffers.add(entry.value)
            attachMap[entry.key] = entry.value.size
        }

        mapToJson["attachArray"] = attachMap.toList().map {
            listOf(it.first, it.second)
        }

        val stringJson = gson.toJson(mapToJson)
        val attachmentJson = gson.toJson(attachmentBuffers)

        val formatBuf = "+".toByteArray()

        var lengthBuf = ByteBuffer.allocate(4)
        lengthBuf.order(ByteOrder.BIG_ENDIAN)
        lengthBuf.putInt(stringJson.length)

        val jsonBuf = stringJson.toByteArray()

        return formatBuf + lengthBuf.array() + jsonBuf + attachmentJson.toByteArray()
    }

}

fun fromJsonToQueueMessage(message: String): QueueMessage {
    try {
        val parsedData = JsonParser.parseString(message).asJsonObject

        val mappedData = gson.fromJson(parsedData, Map::class.java)

        if (mappedData["status"] == null) {
            return QueueMessage("error", mutableMapOf("error" to "cannot decode JSON string"))
        }

        var data: MutableMap<String, Any?>? = null

        if (mappedData["data"] != null) {
            data = mappedData["data"] as MutableMap<String, Any?>
        }

        val messageBack = QueueMessage(
            mappedData["status"] as String,
            data
        )

        if (mappedData["timeOut"] != null) {
            messageBack.timeOut = mappedData["timeOut"] as Int
        }

        return messageBack
    } catch (error: Exception) {
        return QueueMessage("error", mutableMapOf("error" to error))
    }
}

fun unserialize(byteArray: ByteArray): QueueMessage {
    val stringFromBytes = String(byteArray)
    if (stringFromBytes.startsWith('+')) {

        val jsonLength = BigInteger(byteArray.slice(IntRange(1, 4)).toByteArray()).toInt()
        val received = fromJsonToQueueMessage(String(byteArray).slice(IntRange(5, 4 + jsonLength)))

        var prevAttachmentLength = 5 + jsonLength
        for (el in received.attachArray) {
            received.addAttachment(
                el[0] as String, byteArray.slice(
                    IntRange(
                        prevAttachmentLength, (prevAttachmentLength + el[1] as Double - 1).toInt()
                    )
                ).toByteArray()
            )
            prevAttachmentLength += (el[1] as Double).toInt()
        }
        return received
    } else {
        return fromJsonToQueueMessage(String(byteArray))
    }
}
