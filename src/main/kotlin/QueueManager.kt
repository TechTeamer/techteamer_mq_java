class QueueManager(private val config: QueueConfig) {
    val connection = QueueConnection(config)
    var rpcClients = mapOf<String, Int>()
}