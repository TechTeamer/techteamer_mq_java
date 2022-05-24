import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Delivery
import org.slf4j.Logger

open class QueueServer(
    open val queueConnection: QueueConnection,
    override var logger: Logger,
    override val name: String,
    override val options: ConnectionOptions
) : Subscriber(queueConnection, logger, name, options) {


    override fun initialize() {
        val channel = queueConnection.getChannel()
        val prefetchCount = options.prefetchCount
        channel.queueDeclare(name, true, false, false, null)
        if (prefetchCount != null) {
            channel.basicQos(prefetchCount)
        }
        channel.basicConsume(name, true, deliverCallback) { consumerTag: String? -> }
    }

    override fun callback(
        data: MutableMap<String, Any?>,
        props: BasicProperties,
        request: QueueMessage,
        delivery: Delivery
    ): Any? {
        logger.info("message")
        return super.callback(data, props, request, delivery)
    }

}