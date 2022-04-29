import com.rabbitmq.client.MessageProperties
import com.rabbitmq.client.RpcClient
import com.rabbitmq.client.RpcClientParams
import java.nio.Buffer

open class RPCClient(
    private val queueConnection: QueueConnection,
    private val rpcName: String = "defaultName",
    private val options: RpcOption = object : RpcOption {
        override val queueMaxSize: Int = 100
        override val timeOutMs: Int = 100
    }
) {
    private val rpcQueueMaxSize = options.queueMaxSize
    private val rpcTimeOuts = options.timeOutMs
    var channel = queueConnection.getQueueChannel()
    private lateinit var client: RpcClient

    fun initalize() {
        val rpcOptions = RpcClientParams()
        rpcOptions.channel(channel)
        rpcOptions.exchange(rpcName)
        rpcOptions.routingKey("key")
        rpcOptions.timeout(options.timeOutMs)

        println(rpcOptions.exchange)
        client = RpcClient(rpcOptions)

        println(client)
    }

    open fun call(message: QueueMessage) {
        val serialized = message.serialize(message)
        client.stringCall(serialized)
    }


}

interface RpcOption {
    val queueMaxSize: Int
    val timeOutMs: Int
}


