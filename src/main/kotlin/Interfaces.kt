import org.slf4j.Logger
import org.slf4j.LoggerFactory

interface QueueConfig {
    var url: String
    val options: RabbitMqOptions?
    val logger: Logger
        get() = LoggerFactory.getLogger("mq-logger")
    val rpcTimeoutMs: Int
        get() = 10000
    val rpcQueueMaxSize: Int
        get() = 100
}

interface RabbitMqOptions {
    val rejectUnauthorized: Boolean
        get() = false
    val key: String
    val trust: String
    val trustPwd: CharArray
    val keyPwd: CharArray
}

interface RpcOptions {
    val queueMaxSize: Int
    val timeOutMs: Int
    val prefetchCount: Int
        get() = 1
}

interface RpcServerOptions {
    val timeOutMs: Int
    val prefetchCount: Int
        get() = 1
}

interface ConnectionOptions {
    val maxRetry: Int?
    val timeOutMs: Int
    val prefetchCount: Int?
        get() = 1
}
