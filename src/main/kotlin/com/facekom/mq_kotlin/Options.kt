package com.facekom.mq_kotlin

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.rabbitmq.client.BasicProperties
import com.rabbitmq.client.Delivery

typealias QueueHandler = (
    data: JsonElement?,
    props: BasicProperties,
    request: QueueMessage,
    delivery: Delivery
) -> Unit

typealias RpcHandler = suspend (
    data: JsonElement?,
    request: QueueMessage,
    response: QueueResponse,
    delivery: Delivery
) -> JsonElement?

interface JsonSerializable {
    fun toJSON(): JsonElement
}

abstract class QueueAction<T>(val action: String) : JsonSerializable {
    abstract val data: T

    abstract fun getData(): JsonElement

    override fun toJSON(): JsonElement {
        val obj = JsonObject()
        obj.addProperty("action", action)
        obj.add("data", getData())
        return obj
    }
}

class SimpleStringAction(
    action: String,
    override val data: String
) : QueueAction<String>(action) {

    override fun getData(): JsonElement {
        return JsonPrimitive(data)
    }
}

open class ConnectionOptions {
    var maxRetry: Int = 1
    var timeOutMs: Int = 10000
    var prefetchCount: Int? = 1
}

class ConnectionPoolConfig {
    var defaultConnectionName: String = "default"
}

open class AssertExchangeOptions {
    var assert: Boolean = true
    var durable: Boolean = true
    var autoDelete: Boolean = false
    var arguments: Map<String, Any> = mutableMapOf()
}

open class AssertQueueOptions {
    var assert: Boolean = true
    var durable: Boolean = true
    var exclusive: Boolean = false
    var autoDelete: Boolean = false
}

open class RpcClientOptions {
    var queueMaxSize: Int = 100
    var timeOutMs: Int = 10000
    var prefetchCount: Int = 1
    var replyQueueName: String = ""
    var replyQueue: AssertQueueOptions = AssertQueueOptions()

    init {
        replyQueue.exclusive = true
    }
}

open class RpcServerOptions {
    var timeOutMs: Int = 10000
    var prefetchCount: Int = 1
    val queue: AssertQueueOptions = AssertQueueOptions()
}

open class PublisherOptions {
    val exchange: AssertExchangeOptions = AssertExchangeOptions()
}

open class SubscriberOptions {
    val connection: ConnectionOptions = ConnectionOptions()
    val exchange: AssertExchangeOptions = AssertExchangeOptions()
    val queue: AssertQueueOptions = AssertQueueOptions()

    init {
        queue.exclusive = true
    }
}

open class QueueClientOptions {
    val queue: AssertQueueOptions = AssertQueueOptions()
}

open class QueueServerOptions {
    var connection: ConnectionOptions = ConnectionOptions()
    val queue: AssertQueueOptions = AssertQueueOptions()
}
