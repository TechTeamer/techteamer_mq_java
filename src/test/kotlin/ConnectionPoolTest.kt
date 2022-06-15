import com.facekom.mq_kotlin.ConnectionPool
import com.facekom.mq_kotlin.QueueConfig
import com.facekom.mq_kotlin.QueueManager
import org.junit.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ConnectionPoolTest {
    private val testhelper = TestHelper()

    @Test
    fun testConnection() {
        val pool = ConnectionPool(mapOf("other" to "mydefaultname"))
        pool.setLogger(testhelper.logger)
        pool.setupQueueManagers(mapOf("mydefaultname" to testhelper.testConfig, "other" to testhelper.testConfig))
        val queue = pool.defaultConnection

        assertTrue(pool.hasConnection("mydefaultname"))
        assertTrue(pool.hasConnection("other"))
        assertIs<QueueManager>(queue)
    }

    @Test
    fun testConnect() {
        val pool = ConnectionPool(mapOf("other" to "mydefaultname"))
        pool.setLogger(testhelper.logger)
        pool.setupQueueManagers(mapOf("mydefaultname" to testhelper.testConfig, "other" to testhelper.testConfig))
        pool.connect()
    }

    @Test
    fun testConnectFail() {
        val myWrongConfig = object : QueueConfig {
            override var url = "amqp://guest:gue@localhost:67/"
            override val options = null
        }

        val pool = ConnectionPool(mapOf("other" to "default"))
        pool.setLogger(testhelper.logger)
        pool.setupQueueManagers(mapOf("mydefaultname" to myWrongConfig))

        pool.connect()

        assertTrue { pool.connections["mydefaultname"]?.connection?.myChannel == null }
    }
}