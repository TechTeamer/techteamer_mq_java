import com.facekom.mq.QueueConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.test.*

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class QueueConfigTest {
    @Test
    fun testStripCredentialsFromUrl() {
        val url = QueueConfig.stripCredentialsFromUrl("amqps://user:pass@localhost:5671/vhost")
        assertEquals("amqps://localhost:5671/vhost", url)
    }
}
