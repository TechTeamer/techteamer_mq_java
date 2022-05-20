class QueueResponse(
    var statusMessage: String? = "",
    var statusCode: String? = "",
    var attachments: MutableMap<String, ByteArray> = mutableMapOf()
) {
    val OK = "OK"
    val NOT_FOUND = "NOT_FOUND"
    val ERROR = "ERROR"
    val statuses = mapOf(OK to OK, NOT_FOUND to NOT_FOUND, ERROR to ERROR)

    fun setStatus(statusCode: String, statusMessage: String? = statuses[statusCode]) {
        this.statusCode = statusCode
        this.statusMessage = statusMessage
    }

    fun ok(statusMessage: String?) {
        setStatus(OK, statusMessage)
    }

    fun notFound(statusMessage: String?) {
        setStatus(NOT_FOUND, statusMessage)
    }

    fun error(statusMessage: String?) {
        setStatus(ERROR, statusMessage)
    }

    fun addAttachment(name: String, bytes: ByteArray) {
        attachments[name] = bytes
    }

    fun getAttachment(name: String): ByteArray? {
        return attachments[name]
    }

    fun hasAttachment(name: String): Boolean {
        return attachments.contains(name)
    }

    fun hasAnyAttachments(): Boolean {
        return attachments.isEmpty()
    }

}