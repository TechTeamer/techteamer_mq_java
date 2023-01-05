import com.facekom.mq.ConnectionPool
import com.facekom.mq.ConnectionPoolConfig
import com.facekom.mq.QueueConfig
import com.facekom.mq.QueueManager
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ConnectionPoolTest {
    private val testhelper = TestHelper()
    val defaultConfig = testhelper.testConfig
    val badConfig = QueueConfig()
        .url("amqp://guest:gue@localhost:67/")
    val defaultPoolConfig = ConnectionPoolConfig()
    val defaultConnectionName = defaultPoolConfig.defaultConnectionName

    @Test
    fun testDefaultConfig() {
        val pool = ConnectionPool()

        assertEquals(pool.defaultConnectionName, defaultConnectionName)
    }

    @Test
    fun testDefaultConnectionSetup() {
        val pool = ConnectionPool()
        pool.setLogger(testhelper.logger)
        pool.setupQueueManagers(mapOf(defaultConnectionName to defaultConfig))

        assertTrue(pool.hasConnection(defaultConnectionName))
        assertIs<QueueManager>(pool.defaultConnection)
        assertSame(pool.getConnection(defaultConnectionName), pool.defaultConnection)
    }

    @Test
    fun testCustomDefaultConnectionSetup() {
        val customConnectionPoolConfig = ConnectionPoolConfig()
        val customDefaultConnectionName = ""
        customConnectionPoolConfig.defaultConnectionName = customDefaultConnectionName
        val pool = ConnectionPool(customConnectionPoolConfig)
        pool.setLogger(testhelper.logger)
        pool.setupQueueManagers(mapOf(customDefaultConnectionName to defaultConfig, "other" to defaultConfig))

        assertTrue(pool.hasConnection(customDefaultConnectionName))
        assertTrue(pool.hasConnection("other"))
        assertIs<QueueManager>(pool.defaultConnection)
        assertSame(pool.getConnection(customDefaultConnectionName), pool.defaultConnection)
    }

    @Test
    fun testConnect() {
        val pool = ConnectionPool()
        pool.setLogger(testhelper.logger)
        pool.setupQueueManagers(mapOf(defaultConnectionName to defaultConfig))

        pool.connect()

        assertTrue { pool.connected }
    }

    @Test
    fun testMultipleConnect() {
        val pool = ConnectionPool()
        pool.setLogger(testhelper.logger)
        pool.setupQueueManagers(mapOf(defaultConnectionName to defaultConfig, "other" to defaultConfig))

        pool.connect()

        assertTrue { pool.connected }
    }

    @Test
    fun testConnectFail() {
        val pool = ConnectionPool()
        pool.setLogger(testhelper.logger)
        pool.setupQueueManagers(mapOf(defaultConnectionName to defaultConfig, "bad" to badConfig))

        assertFails("Connection should fail with wrong config") {
            pool.connect()
        }

        assertFalse {
            pool.connected
        }
    }
}
