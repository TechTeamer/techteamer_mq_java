import com.google.gson.Gson
import com.rabbitmq.client.Channel
import com.rabbitmq.client.RpcClient
import com.rabbitmq.client.RpcClientParams

open class RPCClient(
    private val connection: QueueConnection,
    private val rpcName: String = "defaultName",
    private val options: RpcOptions = object : RpcOptions {
        override val queueMaxSize: Int = 100
        override val timeOutMs: Int = 5000
    }
) {
    private val rpcQueueMaxSize = options.queueMaxSize
    private val rpcTimeOuts = options.timeOutMs
    private lateinit var client: RpcClient

    private val keyName = "$rpcName-key"
    private val exchangeName = "$rpcName-exchange"

    fun initialize() {
        val channel = connection.getChannel()
        channel.exchangeDeclare(exchangeName, "direct", true)
        channel.queueDeclare(rpcName, true, false, false, null)
        channel.queueBind(rpcName, exchangeName, keyName)

        val rpcOptions = RpcClientParams()
        rpcOptions.channel(channel)
        rpcOptions.exchange(exchangeName)
        rpcOptions.routingKey(keyName)
        rpcOptions.timeout(options.timeOutMs)

        client = RpcClient(rpcOptions)
    }

    open fun call(message: QueueMessage): ByteArray? {
        return client.primitiveCall(message.serialize())
    }

    fun getClient(): RpcClient {
        return client
    }

}

interface RpcOptions {
    val queueMaxSize: Int
    val timeOutMs: Int
}


