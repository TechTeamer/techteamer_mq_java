import org.slf4j.Logger

interface QueueConfig {
    var url: String
    val options: RabbitMqOptions?
    val logger: Logger
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
