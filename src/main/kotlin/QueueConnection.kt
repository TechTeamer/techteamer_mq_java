import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class QueueConnection(private val config: QueueConfig) {
    private val factory = ConnectionFactory()
    private var connection: Connection? = null
    private var channel: Channel? = null

    private fun getConnection(): Connection? {
        if (connection == null) {
            connection = factory.newConnection(config.url)
            return connection
        }

        return connection
    }

    fun getQueueChannel(): Channel? {
        if (channel == null) {
            return getConnection()?.createChannel()
        }

        return channel
    }
}