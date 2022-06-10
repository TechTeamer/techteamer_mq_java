import org.slf4j.Logger

open class QueueServer(
    open val queueConnection: QueueConnection,
    override var logger: Logger,
    override val name: String,
    override val options: ConnectionOptions = object : ConnectionOptions {
        override val maxRetry = 1
        override val timeOutMs = 10000
        override val prefetchCount = 1
    }
) : Subscriber(queueConnection, logger, name, options) {


    override fun initialize() {
        val channel = queueConnection.getChannel()
        val prefetchCount = options.prefetchCount
        channel.queueDeclare(name, true, false, false, null)
        if (prefetchCount != null) {
            channel.basicQos(prefetchCount)
        }
        channel.basicConsume(name, true, deliverCallback) { _: String? -> } // consumerTag parameter
    }
    
}