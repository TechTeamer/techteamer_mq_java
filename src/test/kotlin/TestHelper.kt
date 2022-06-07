import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestHelper {
    val testConfig = object : QueueConfig {
        override var url: String = "amqp://guest:guest@localhost:5672"
        override val options = null
    }

    val logger: Logger = LoggerFactory.getLogger("brandNewTestLogger")
}