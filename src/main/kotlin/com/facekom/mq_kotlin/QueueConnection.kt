package com.facekom.mq_kotlin

import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class QueueConnection(val config: QueueConfig) {

    private var factory = ConnectionFactory()

    private lateinit var connection: Connection
    var myChannel: Channel? = null
    var logger = config.logger


    fun getChannel(): Channel {
        if (myChannel != null) return myChannel as Channel
        myChannel = connection.createChannel()
        return myChannel as Channel
    }

    fun connect() {
        if (config.options != null) {
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(FileInputStream(config.options!!.key), config.options!!.keyPwd)

            val keyMngF = KeyManagerFactory.getInstance("SunX509")
            keyMngF.init(keyStore, config.options!!.keyPwd)

            val trustStore = KeyStore.getInstance("JKS")
            trustStore.load(FileInputStream(config.options!!.trust), config.options!!.trustPwd)

            val tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(trustStore)

            val context = SSLContext.getInstance("TLSv1.2")
            context.init(keyMngF.keyManagers, tmf.trustManagers, null);

            factory.useSslProtocol(context)
        }

        factory.setUri(config.url)
        connection = factory.newConnection(config.url)
        getChannel()
    }

}
