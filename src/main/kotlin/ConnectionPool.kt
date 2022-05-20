import mu.KLogger
import mu.KotlinLogging

class ConnectionPool(poolConfig: Map<String, String>) {
    var connections = mutableMapOf<String, QueueManager>()
    var defaultConnectionName = poolConfig["defaultConnectionName"] ?: "default"

    private var logger = KotlinLogging.logger {  }
    lateinit var defaultConnection: QueueManager

    fun setupQueueManagers(connectionConfigs: Map<String, QueueConfig>) {
        var defaultConnectionConfig: QueueConfig? = connectionConfigs["$defaultConnectionName"]

        if (defaultConnectionConfig != null) {
            val connection = createConnection(defaultConnectionConfig)
            registerConnection(defaultConnectionName, connection)
            defaultConnection = connection
        }

        connectionConfigs.forEach { t ->
            if (t.key == defaultConnectionName) return
            val connection = createConnection(t.value)
            registerConnection(t.key, connection)
        }
    }

    private fun createConnection(config: QueueConfig): QueueManager {
        return QueueManager(config)
    }

    fun registerConnection(connectionName: String, connection: QueueManager) {
        connections["$connectionName"] = connection
    }

    fun setLogger(loggerInput: Any) {
        logger = loggerInput as KLogger
    }

    fun getConnection(name: String): QueueManager? {
        return connections["$name"]
    }

    fun hasConnection(name:String): Boolean {
        if (connections["$name"] != null) return true
        return false
    }

    fun connect() {
        connections.forEach() {t ->
            t.value.connect()
        }
    }
}