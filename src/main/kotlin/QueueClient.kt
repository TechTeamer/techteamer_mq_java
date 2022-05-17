import mu.KLogger

class QueueClient(
    override val queueConnection: QueueConnection,
    override val logger: KLogger,
    val name: String
    ) : Publisher(queueConnection, logger, name) {
    override val channel = queueConnection.getChannel()

    override fun initialize() {
        channel.queueDeclare(name, true, false, false, null)
    }
}