import com.facekom.mq_kotlin.QueueConfig
import com.facekom.mq_kotlin.RabbitMqOptions
import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestHelper {
    var testConfig = object : QueueConfig {
        override var url: String = "amqps://guest:guest@localhost:5671"
        override val options: RabbitMqOptions? = object : RabbitMqOptions {
            override val key: String
                get() = "/workspace/vuer_docker/workspace/cert/vuer_mq_cert/client/keycert.p12"
            override val trust: String
                get() = "/workspace/vuer_docker/workspace/cert/vuer_mq_cert/rabbitstore"
            override val trustPwd: CharArray
                get() = "asdf1234".toCharArray()
            override val keyPwd: CharArray
                get() = "MySecretPassword".toCharArray()
        }
    }

    init {
        if (System.getenv("TEST_ENV") == "travis" ) {
            testConfig = object : QueueConfig {
                override var url = "amqp://guest:guest@localhost:5672"
                override val options = null
            }
        }
    }

    val logger: Logger = LoggerFactory.getLogger("brandNewTestLogger")
}
