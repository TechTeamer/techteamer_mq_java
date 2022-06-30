package com.facekom.mq_kotlin

import org.slf4j.Logger
import org.slf4j.LoggerFactory

class ConnectionPool(poolConfig: ConnectionPoolConfig = ConnectionPoolConfig()) {
    private var logger = LoggerFactory.getLogger("testLogger")

    var connections = mutableMapOf<String, QueueManager>()
    var defaultConnection: QueueManager? = null
    var defaultConnectionName = poolConfig.defaultConnectionName
    var connected: Boolean = false

    fun setupQueueManagers(connectionConfig: QueueConfig) {
        val connection = createConnection(connectionConfig)
        registerConnection(defaultConnectionName, connection)
        defaultConnection = connection
    }

    fun setupQueueManagers(connectionConfigs: Map<String, QueueConfig>) {
        val defaultConnectionConfig: QueueConfig? = connectionConfigs[defaultConnectionName]

        if (defaultConnectionConfig != null) {
            setupQueueManagers(defaultConnectionConfig)
        }

        connectionConfigs.forEach { (connectionName, connectionConfig) ->
            if (connectionName == defaultConnectionName) return@forEach
            val connection = createConnection(connectionConfig)
            registerConnection(connectionName, connection)
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

    fun hasConnection(name: String): Boolean {
        return connections[name] != null
    }

    fun connect() {
        connections.forEach { _, connection ->
            connection.connect()
        }
        connected = true
    }
}
