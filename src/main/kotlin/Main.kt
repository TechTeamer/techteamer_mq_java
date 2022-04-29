fun main(): Unit {
    var myConfig = object : QueueConfig {
        override var url: String
            get() = "amqp://guest:guest@localhost:5672/"
            set(value) {}
        override val options: String
            get() = "Temp value"
    }
    val conn = QueueConnection(myConfig)

    // val client = RPCClient(conn)
    // client.initalize()

    val x = mapOf("ez" to "az")
    val y = x.toString()
    val z = y.toByteArray()
    println(String(z))
}