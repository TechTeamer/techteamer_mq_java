import com.facekom.mq_kotlin.QueueConfig
import com.facekom.mq_kotlin.QueueConnection
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueConnectionTest {
    private val testhelper = TestHelper()

    @Test
    fun testConnect() {
        val connection = QueueConnection(testhelper.testConfig)

        connection.connect()

        assertTrue { connection.connected }
    }

    @Test
    fun testConnectFail() {
        val myWrongConfig = QueueConfig()
            .url("amqp://guest:gue@localhost:67/")
        val connection = QueueConnection(myWrongConfig)

        assertFails ("Connection should fail with wrong config") {
            connection.connect()
        }

        assertFalse {
            connection.connected
        }
    }
}
