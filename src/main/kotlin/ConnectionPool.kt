import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ConnectionPool(poolConfig: Map<String, String>) {
    var connections = mutableMapOf<String, QueueManager>()
    var defaultConnectionName = poolConfig["defaultConnectionName"] ?: "default"

    private var logger = LoggerFactory.getLogger("testLogger")
    lateinit var defaultConnection: QueueManager

    fun setupQueueManagers(connectionConfigs: Map<String, QueueConfig>) {
        var defaultConnectionConfig: QueueConfig? = connectionConfigs[defaultConnectionName]

        if (defaultConnectionConfig != null) {
            val connection = createConnection(defaultConnectionConfig)
            registerConnection(defaultConnectionName, connection)
            defaultConnection = connection
        }

        connectionConfigs.forEach { t ->
            if (t.key == defaultConnectionName) return
            val connection = createConnection(t.value)
            registerConnection(t.key, connection)
            if (!this::defaultConnection.isInitialized) {
                defaultConnection = connection
            }
        }
    }

    private fun createConnection(config: QueueConfig): QueueManager {
        return QueueManager(config)
    }

    fun registerConnection(connectionName: String, connection: QueueManager) {
        connections[connectionName] = connection
    }

    fun setLogger(loggerInput: Any) {
        logger = loggerInput as Logger
    }

    fun getConnection(name: String): QueueManager? {
        return connections[name]
    }

    fun hasConnection(name:String): Boolean {
        return connections[name] != null
    }

    fun connect() {
        connections.forEach() {t ->
            t.value.connect()
        }
    }
}
