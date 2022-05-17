import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import mu.KLogger
import mu.KotlinLogging

open class Subscriber(
    var connection: QueueConnection,
    private var logger: KLogger = KotlinLogging.logger { },
    open val name: String,
    options: ConnectionOptions
) : ConnectionOptions {
    
    override val maxRetry: Int = options.maxRetry
    override val timeOutMs: Number = options.timeOutMs
    open val retryMap = mutableMapOf<String, Int>()
    open val actions = mutableMapOf<String, (
        Any,
        data: MutableMap<String, Any?>,
        props: BasicProperties,
        request: QueueMessage,
        delivery: Delivery
    ) -> Any>()

    open val channel = connection.getChannel()

    fun setLogger(loggerInput: Any) {
        logger = loggerInput as KLogger
    }

    open fun initialize() {

        channel.exchangeDeclare(name, "fanout")
        val queueName = channel.queueDeclare().queue;

        channel.queueBind(queueName, name, "")
        channel.basicConsume(queueName, true, deliverCallback) { consumerTag: String? -> }
    }

    open val deliverCallback = DeliverCallback { consumerTag: String?, delivery: Delivery ->
        // val message = String(delivery.body, "UTF-8")
        processMessage(channel, delivery, consumerTag)
    }

    open fun processMessage(channel: Channel, delivery: Delivery, consumerTag: String?) {
        val request = unserialize(delivery.body)

        if (request != null) {
            if (request.status != "ok") {
                ack(channel, delivery)
            }
        }

        if (consumerTag != null) {
            var counter = 1
            if (retryMap[consumerTag] != null) {
                counter += retryMap[consumerTag]!!
            } else {
                retryMap[consumerTag] = counter
            }

            if (counter > maxRetry) {
                logger.error("SUBSCRIBER TRIED TOO MANY TIMES $name, $request, $delivery")
                ack(channel, delivery)
                if (retryMap[consumerTag] != null) {
                    retryMap.remove(consumerTag)
                }
            }
        }

        val result = request?.let { callback(it.data, delivery.properties, request, delivery) }
    }

    open fun callback(
        data: MutableMap<String, Any?>,
        props: BasicProperties,
        request: QueueMessage,
        delivery: Delivery
    ): Any? {
        if (actions[data["action"]] != null) {
            return actions[data["action"]]?.invoke(this, data, props, request, delivery)
        }
        return null
    }

    open fun registerAction(
        action: String, handler: (
            Any,
            data: MutableMap<String, Any?>,
            props: BasicProperties,
            request: QueueMessage,
            delivery: Delivery
        ) -> Any
    ) {
        if (actions[action] != null) {
            logger.warn("Actions-handlers map already contains an action named $action")
        } else {
            actions[action] = handler
        }
    }

    open fun ack(channel: Channel, delivery: Delivery) {
        channel.basicAck(delivery.envelope.deliveryTag, false)
    }

    open fun nack(channel: Channel, delivery: Delivery) {
        channel.basicNack(delivery.envelope.deliveryTag, false, false)
    }

}

interface ConnectionOptions {
    val maxRetry: Int
    val timeOutMs: Number
    val prefetchCount: Int?
        get() = 1
}