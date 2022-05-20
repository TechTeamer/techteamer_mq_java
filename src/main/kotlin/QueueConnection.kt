import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory

class QueueConnection constructor(config: QueueConfig) {

    private val factory = ConnectionFactory()
    private var connection: Connection = factory.newConnection(config.url)
    open var myChannel: Channel? = null
    var logger = config.logger

    fun getChannel(): Channel {
        if (myChannel != null) return myChannel as Channel
        myChannel = connection.createChannel()
        return myChannel as Channel
    }

}