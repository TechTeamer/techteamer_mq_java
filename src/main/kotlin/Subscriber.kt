import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Channel
import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import org.slf4j.Logger

open class Subscriber(
    open var connection: QueueConnection,
    open var logger: Logger,
    open val name: String,
    open val options: ConnectionOptions? = object : ConnectionOptions {
        override val maxRetry = 1
        override val timeOutMs = 5000
        override val prefetchCount = 1
    }
) {
    open val retryMap = mutableMapOf<String, Int>()
    open val actions = mutableMapOf<String, (
        Any,
        data: MutableMap<String, Any?>,
        props: BasicProperties,
        request: QueueMessage,
        delivery: Delivery
    ) -> Any>()

    fun setLogger(loggerInput: Any) {
        logger = loggerInput as Logger
    }

    open fun initialize() {
        val channel = connection.getChannel()
        channel.exchangeDeclare(name, "fanout", true)
        channel.queueDeclare(name, true, false, false, null)?.queue;
        channel.queueBind(name, name, "")
        channel.basicConsume(name, true, deliverCallback) { consumerTag: String? -> }
    }

    open val deliverCallback = DeliverCallback { consumerTag: String?, delivery: Delivery ->
        val channel = connection.getChannel()
        processMessage(channel, delivery, consumerTag)
    }

    open fun processMessage(channel: Channel, delivery: Delivery, consumerTag: String?): Any? {
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

            println(options?.maxRetry)
            if (options?.maxRetry != null) {
                if (counter > options!!.maxRetry!!) {
                    logger.error("SUBSCRIBER TRIED TOO MANY TIMES $name, $request, ${delivery.body}")
                    ack(channel, delivery)
                    if (retryMap[consumerTag] != null) {
                        retryMap.remove(consumerTag)
                    }
                }
            }
        }

        val result = request?.let { callback(it.data, delivery.properties, request, delivery) }
        retryMap.remove(consumerTag)

        return result
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

