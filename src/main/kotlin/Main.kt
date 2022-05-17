import mu.KLogger
import kotlin.concurrent.thread

fun main(): Unit {
//    var myConfig = object : QueueConfig {
//        override var url: String
//            get() = "amqp://guest:guest@localhost:5672/"
//            set(value) {}
//        override val options: String
//            get() = "Temp value"
//        override val logger: KLogger
//            get() = TODO("Not yet implemented")
//    }
//    val conn = QueueConnection(myConfig)
//
//    val myChannel = conn.getChannel()
//
//
//
//    thread(true) {
//        val serv = RPCServer(myChannel, "test2")
//        serv.mainloop()
//    }

//    val client = RPCClient(myChannel, "test2")
//    client.initialize()
//
//    val message = QueueMessage(
//        "ok",
//        mutableMapOf("x" to "x", "y" to true, "z" to 70),
//    )
//
//    message.addAttachment("testAtt", "test".toByteArray())
//    message.addAttachment("testAtt2", "testdfguzsdgvf".toByteArray())
//
//    println(message.attachments)
//
//    val resp = client.call(message)
//    if (resp != null) {
//        val unserializedResp = unserialize(resp)
//        if (unserializedResp != null) {
//            println(unserializedResp.data)
//            println(unserializedResp.attachments)
//        }
//    }

    // testFun(TestClass().javaClass)

}

//open class TestOne() {
//    open fun print() {
//        println("test1")
//    }
//}
//class TestClass() : TestOne() {
//    override fun print() {
//        println("test2")
//    }
//}
//
//fun testFun(OverrideClass: Class<TestOne>) {
//    val testI = OverrideClass.getDeclaredConstructor().newInstance()
//
//    testI.print()
//}