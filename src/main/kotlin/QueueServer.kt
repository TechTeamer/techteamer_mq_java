import mu.KLogger

class QueueServer(
    val queueConnection: QueueConnection,
    val logger: KLogger,
    override val name: String,
    val options: ConnectionOptions
) : Subscriber(queueConnection, logger, name, options)  {

    override val channel = queueConnection.getChannel()
    override val prefetchCount = options.prefetchCount

    override fun initialize() {
        channel.queueDeclare(name, false, false, false, null)
        if (prefetchCount != null) {
            channel.basicQos(prefetchCount)
        }
        channel.basicConsume(name, true, deliverCallback) { consumerTag: String? -> }
    }
}