import com.rabbitmq.client.Channel
import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.RpcClient
import com.rabbitmq.client.RpcClientParams
import java.nio.Buffer

open class RPCClient(
    private val channel: Channel,
    private val rpcName: String = "defaultName",
    private val options: RpcOption = object : RpcOption {
        override val queueMaxSize: Int = 100
        override val timeOutMs: Int = 5000
    }
) {
    private val rpcQueueMaxSize = options.queueMaxSize
    private val rpcTimeOuts = options.timeOutMs
    private lateinit var client: RpcClient

    private val keyName = "$rpcName-key"
    private val exchangeName = "$rpcName-exchange"

    fun initalize() {
        channel.exchangeDeclare(exchangeName, "direct", true)
        channel.queueDeclare(rpcName, true, true, false, null)
        channel.queueBind(rpcName, exchangeName, keyName)

        val rpcOptions = RpcClientParams()
        rpcOptions.channel(channel)
        rpcOptions.exchange(exchangeName)
        rpcOptions.routingKey(keyName)
        rpcOptions.timeout(options.timeOutMs)

        client = RpcClient(rpcOptions)
    }

    open fun call(message: QueueMessage): String {

        val serialized = message.serialize(message)

        val x = client.stringCall(serialized)
        return x
    }

    fun getClient(): RpcClient {
        return client
    }

}

interface RpcOption {
    val queueMaxSize: Int
    val timeOutMs: Int
}


