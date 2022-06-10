import org.slf4j.Logger
import org.slf4j.LoggerFactory

class TestHelper {
    val testConfig = object : QueueConfig {
        override var url: String = "amqps://guest:guest@localhost:5671"
        override val options: RabbitMqOptions? = object : RabbitMqOptions {
            override val key: String
                get() = "/client/client_certificate.p12"
            override val trust: String
                get() = "/rabbitstore/mystore"
            override val trustPwd: CharArray
                get() = "123456".toCharArray()
            override val keyPwd: CharArray
                get() = "MySecretPassword".toCharArray()
        }
    }

    val logger: Logger = LoggerFactory.getLogger("brandNewTestLogger")
}