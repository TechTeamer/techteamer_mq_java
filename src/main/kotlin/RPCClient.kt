import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.RpcClient
import com.rabbitmq.client.RpcClientParams
import org.slf4j.Logger
import java.util.UUID

open class RPCClient constructor(
    open val connection: QueueConnection,
    open val rpcName: String,
    open val logger: Logger,
    open val options: RpcOptions = object : RpcOptions {
        override val queueMaxSize: Int = 100
        override val timeOutMs: Int = 5000
        override val prefetchCount: Int = 1
    }
) {


    private val rpcQueueMaxSize = options.queueMaxSize
    private val rpcTimeOuts = options.timeOutMs
    private val prefetchCount = options.prefetchCount

    private lateinit var client: RpcClient

    private val keyName = "$rpcName-key"
    private val exchangeName = "$rpcName"

    val correlationIdList = mutableListOf<String>()

    fun initialize() {
        val channel = connection.getChannel()
        channel.exchangeDeclare(rpcName, "direct", true)
        val queue = channel.queueDeclare("$rpcName-reply", true, false, false, null)
        channel.queueBind(rpcName, rpcName, keyName)
        channel.basicQos(prefetchCount)

        println(queue.queue)

        val rpcOptions = RpcClientParams()
        rpcOptions.channel(channel)
        rpcOptions.exchange(rpcName)
        rpcOptions.routingKey(keyName)
        rpcOptions.timeout(rpcTimeOuts)
        rpcOptions.replyTo(queue.queue)

        client = RpcClient(rpcOptions)
    }

    open fun call(
        message: MutableMap<String, Any?>,
        timeOutMs: Int? = null,
        attachments: MutableMap<String, ByteArray>? = null
    ): ByteArray? {
        var correlationId: String

        do {
            correlationId = UUID.randomUUID().toString()
        } while (correlationIdList.contains(correlationId))
        try {
            if (correlationIdList.size > rpcQueueMaxSize) {
                throw Exception("RPCCLIENT QUEUE FULL $rpcName")
            }

            val param = QueueMessage("ok", message)

            attachments?.forEach { t ->
                param.addAttachment(t.key, t.value)
            }

            correlationIdList.add(correlationId)

            val props = AMQP.BasicProperties.Builder()
            props.correlationId(correlationId)
            props.replyTo(rpcName)

            val answer = timeOutMs?.let { client.primitiveCall(props.build(), param.serialize(), it) }


            correlationIdList.remove(correlationId)

            return answer
        } catch (err: Exception) {
            correlationIdList.remove(correlationId)
            logger.error("RPCCLIENT: cannot make rpc call", err)
            throw Exception("RPCCLIENT: cannot make rpc call", err)
        }

    }

    fun getClient(): RpcClient {
        return client
    }

}




