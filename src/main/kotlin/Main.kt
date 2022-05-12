import kotlin.concurrent.thread

fun main(): Unit {
    var myConfig = object : QueueConfig {
        override var url: String
            get() = "amqp://guest:guest@localhost:5672/"
            set(value) {}
        override val options: String
            get() = "Temp value"
    }
    val conn = QueueConnection(myConfig)

    val myChannel = conn.getChannel()

    val client = RPCClient(myChannel, "test2")
    client.initialize()

//    thread(true) {
//        val serv = RPCServer(myChannel, "test")
//        serv.mainloop()
//    }

    val message = QueueMessage(
        "ok",
        mutableMapOf("x" to "x", "y" to true, "z" to 70),
    )

    message.addAttachment("testAtt", "test".toByteArray())

    message.addAttachment("testAtt2", "testdfguzsdgvf".toByteArray())

    println(message.attachments)

    val resp = client.call(message)
    if (resp != null) {
        val unserializedResp = unserialize(resp)
        if (unserializedResp != null) {
            println(unserializedResp.data)
            println(unserializedResp.attachments)
        }
    }

}

class TestC<T>(private val i: T) {
    fun getInt(): T {
        return i
    }

    val myMap = mapOf("egy" to 1, "kettö" to 2, "harom" to "harom")
}