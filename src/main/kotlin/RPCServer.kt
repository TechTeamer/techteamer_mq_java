import com.rabbitmq.client.*

open class RPCServer(channel: Channel?, queueName: String) : RpcServer(channel, queueName) {

    open override fun handleCall(requestBody: ByteArray, replyProperties: AMQP.BasicProperties?): ByteArray {
        val message: QueueMessage? = unserialize(requestBody)

        if (message != null) {
            println(message.data)
        }
        return "received".toByteArray()
    }

    override fun processRequest(request: Delivery?) {
        if (request != null) {
            println(request.properties)
        }
        val requestProperties = request!!.properties
        val correlationId = requestProperties.correlationId
        val replyTo = requestProperties.replyTo
        if (correlationId != null && replyTo != null) {
            val replyPropertiesBuilder = AMQP.BasicProperties.Builder().correlationId(correlationId)
            var replyProperties = preprocessReplyProperties(request, replyPropertiesBuilder)
            val replyBody = handleCall(request, replyProperties)
            replyProperties = postprocessReplyProperties(request, replyProperties.builder())
            println("I am here")
            println(replyBody)
            val channel = channel
            println(replyTo.javaClass)
            channel.basicPublish("", replyTo, replyProperties, replyBody)
        } else {
            handleCast(request)
        }
    }

    override fun mainloop(): ShutdownSignalException {
        println("mainloop started")
        return super.mainloop()
    }
}