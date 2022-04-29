import netscape.javascript.JSObject
import java.nio.Buffer
import java.nio.ByteBuffer
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


    var message = object : QueueMessage {
        override var action: String = ""
            get() = "test"
        override val status: String
            get() = "test"
        override val data: String
            get() = "test"
        override val attachments: MutableMap<String, Buffer>?
            get() = mutableMapOf("test" to ByteBuffer.wrap("testttttt".toByteArray()))
    }


    val client = RPCClient(myChannel, "test")
    client.initalize()
    thread(true) {
        val serv = RPCServer(myChannel, "test")
        serv.mainloop()
    }
    val resp = client.call(message)
    println("RESP $resp")

    val resp2 = client.call(message)
    println("RESP2222 $resp2")


}