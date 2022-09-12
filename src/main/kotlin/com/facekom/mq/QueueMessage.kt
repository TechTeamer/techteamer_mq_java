package com.facekom.mq

import com.google.gson.*
import java.math.BigInteger
import java.nio.ByteBuffer
import java.nio.ByteOrder

private val gson = Gson()

class QueueMessage(
    val status: String,
    val data: JsonElement?,
    var timeOut: Int? = null
) {
    val attachments: MutableMap<String, ByteArray> = mutableMapOf()

    fun addAttachment(name: String, bytes: ByteArray) {
        attachments[name] = bytes
    }

    fun serialize(): ByteArray {
        val obj = JsonObject()
        // {
        //   status: this.status,
        //   data: this.data,
        //   timeOut: this.timeOut
        //   attachArray: []
        // }

        // set status
        obj.addProperty("status", status)

        // set timeout
        if (timeOut != null) {
            obj.addProperty("timeOut", timeOut)
        }

        // set data
        if (data != null) {
            obj.add("data", data)
        } else {
            obj.add("data", JsonNull.INSTANCE)
        }

        // set attachments
        var attachmentBuffers = byteArrayOf()
        val attachArray = JsonArray()
        attachments.forEach { name, attachmentBuffer ->
            attachmentBuffers += attachmentBuffer
            val attachMapEl = JsonArray()
            attachMapEl.add(name)
            attachMapEl.add(attachmentBuffer.size)
            attachArray.add(attachMapEl) // name length pair: ["name", 1]
        }
        obj.add("attachArray", attachArray)

        // compile output
        val stringJson = gson.toJson(obj)
        val formatBuf = "+".toByteArray()
        val jsonBuf = stringJson.toByteArray()
        val lengthBuf = ByteBuffer.allocate(4)
            .order(ByteOrder.BIG_ENDIAN)
            .putInt(jsonBuf.size)
            .array()

        return formatBuf + lengthBuf + jsonBuf + attachmentBuffers
    }

    companion object {
        private fun parseJsonMessagePart(jsonString: String): JsonObject {
            val parsedJson = JsonParser.parseString(jsonString)

            if (!parsedJson.isJsonObject) {
                throw Exception("Invalid message format: not an object")
            }

            val messageObj = parsedJson.asJsonObject

            if (messageObj.get("status") == null) {
                throw Exception("Missing status")
            }

            try {
                messageObj.get("status")!!.asString
            } catch (e: Exception) {
                throw Exception("Invalid status")
            }

            try {
                val timeoutEl = messageObj.get("timeOut")
                timeoutEl?.asInt
            } catch (e: Exception) {
                throw Exception("Invalid timeout")
            }

            return messageObj
        }

        private fun createFromParsedMessage(parsedMessage: JsonObject): QueueMessage {
            val status = parsedMessage.get("status").asString!!
            val timeoutEl = parsedMessage.get("timeOut")
            val timeout = timeoutEl?.asInt
            val dataEl = parsedMessage.get("data")

            return QueueMessage(status, dataEl, timeout)
        }

        fun createErrorMessage(errorMessage: String?): QueueMessage {
            val errorObj = JsonObject()
            errorObj.add("error", JsonPrimitive(errorMessage ?: ""))
            return QueueMessage("error", errorObj)
        }

        fun fromJson(jsonString: String): QueueMessage {
            try {
                val parsedMessage = parseJsonMessagePart(jsonString)
                return createFromParsedMessage(parsedMessage)
            } catch (error: Exception) {
                return createErrorMessage(error.message)
            }
        }

        fun unserialize(byteArray: ByteArray): QueueMessage {
            val stringFromBytes = String(byteArray)

            if (stringFromBytes.startsWith('+')) {
                val jsonLength = BigInteger(byteArray.slice(IntRange(1, 4)).toByteArray()).toInt()
                val jsonString = String(byteArray.slice(IntRange(5, 4 + jsonLength)).toByteArray())
                val parsedMessage = parseJsonMessagePart(jsonString)
                val queueMessage = createFromParsedMessage(parsedMessage)
                val attachArray = parsedMessage.get("attachArray").asJsonArray
                var prevAttachmentLength = 5 + jsonLength

                for (el in attachArray) {
                    val nameLengthPair = el.asJsonArray
                    val attachmentName = nameLengthPair[0].asString
                    val attachmentBufferLength = nameLengthPair[1].asInt
                    val attachmentBufferEnd = (prevAttachmentLength + attachmentBufferLength - 1).toInt()
                    val attachmentBuffer = byteArray
                        .slice(IntRange(prevAttachmentLength, attachmentBufferEnd))
                        .toByteArray()
                    queueMessage.addAttachment(attachmentName, attachmentBuffer)
                    prevAttachmentLength += attachmentBufferLength
                }
                return queueMessage
            } else {
                return fromJson(String(byteArray))
            }
        }
    }
}
