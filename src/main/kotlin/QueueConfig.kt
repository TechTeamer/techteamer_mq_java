import mu.KLogger

interface QueueConfig {
    var url: String
    val options: String
    val logger: KLogger
}