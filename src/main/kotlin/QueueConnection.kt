import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class QueueConnection(private val config: QueueConfig) {
    private val factory = ConnectionFactory()
    private var connection: Connection = factory.newConnection(config.url)
    open lateinit var myChannel: Channel
    var logger = config.logger

    fun getChannel(): Channel {
        if (myChannel != null) return myChannel
        myChannel = connection.createChannel()
        return myChannel
    }
}