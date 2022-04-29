import com.rabbitmq.client.*

open class RPCServer(channel: Channel?, queueName: String) : RpcServer(channel, queueName) {


    open override fun handleCall(requestBody: ByteArray?, replyProperties: AMQP.BasicProperties?): ByteArray {
        println("handleCall executed ${requestBody?.let { String(it) }}")
        return "received".toByteArray()
    }

    override fun processRequest(request: Delivery?) {
        println("Process started")
        super.processRequest(request)
    }

    override fun mainloop(): ShutdownSignalException {
        println("mainloop started")
        return super.mainloop()
    }
}