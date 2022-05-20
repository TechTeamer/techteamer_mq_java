import org.slf4j.Logger

interface QueueConfig {
    var url: String
    val options: String
    val logger: Logger
}

interface RpcOptions {
    val queueMaxSize: Int
    val timeOutMs: Int
    val prefetchCount: Int
}

interface ConnectionOptions {
    val maxRetry: Int
    val timeOutMs: Number
    val prefetchCount: Int?
        get() = 1
}