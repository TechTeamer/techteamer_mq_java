import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class QueueConnection(private val config: QueueConfig) {
    private val factory = ConnectionFactory()
    private var connection: Connection = factory.newConnection(config.url)
    private var channel: Channel = connection.createChannel()

    fun getChannel(): Channel {
        return channel
    }

    fun getConnection(): Connection {
        return connection
    }
}