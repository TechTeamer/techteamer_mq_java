import com.rabbitmq.client.DeliverCallback
import com.rabbitmq.client.Delivery
import mu.KLogger

class GatheringClient(
    val queueConnection: QueueConnection,
    val logger: KLogger,
    val name: String,
    val options: GatheringOptions
) {
    var correlationIdMap = mutableMapOf<String, Int>()
    val channel = queueConnection.getChannel()
    lateinit var replyQueue: String

    fun initialize() {
        try {
            channel.exchangeDeclare(name, "fanout", true)

            val myReplyQueue = channel.queueDeclare()
            replyQueue = myReplyQueue.queue

            channel.basicConsume(replyQueue, deliverCallback) { consumerTag: String? -> }
        } catch (err: Exception) {
            logger.error("QUEUE GATHERING CLIENT: Error initializing 'name'")
        }

    }

    open val deliverCallback = DeliverCallback { consumerTag: String?, delivery: Delivery ->
        val correlationId = delivery.properties.correlationId

        try {
            if (delivery.properties.type == "status") {
                handleStatusResponse(delivery)
            } else if (delivery.properties.type == "reply") {
                handleGatheringResponse(delivery)
            } else {
                logger.error(
                    "QUEUE GATHERING CLIENT: INVALID REPLY ON '$name': UNKNOWN MESSAGE TYPE ON REPLY",
                    correlationId,
                    delivery.body
                )
            }
        } catch (err: Exception) {
            logger.error("QUEUE GATHERING CLIENT: FAILED TO HANDLE MESSAGE ON '$name'", correlationId, delivery.body)
        }

    }

    private fun handleGatheringResponse(delivery: Delivery) {
        val correlationId = delivery.properties.correlationId

        if (!correlationIdMap.contains(correlationId)) {
            logger.warn(
                "QUEUE GATHERING CLIENT: RECEIVED UNKNOWN REPLY (possibly timed out or already received) '$name'",
                correlationId
            )
            return
        }

        val requestData = correlationIdMap[correlationId]

    }

    private fun handleStatusResponse(delivery: Delivery) {

    }
}

interface GatheringOptions {
    val queueMaxSize: Int
    val timeoutMs: Long
    val serverCount: Int
}