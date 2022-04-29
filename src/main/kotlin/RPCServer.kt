import com.rabbitmq.client.AMQP
import com.rabbitmq.client.Channel
import com.rabbitmq.client.RpcServer

open class RPCServer(channel: Channel?) : RpcServer(channel) {
    open override fun handleCall(requestBody: ByteArray?, replyProperties: AMQP.BasicProperties?): ByteArray {
        return super.handleCall(requestBody, replyProperties)
    }
}