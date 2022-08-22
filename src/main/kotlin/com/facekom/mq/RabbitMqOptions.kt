package com.facekom.mq

import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class RabbitMqOptions {
    var key: String? = null
    var trust: String? = null
    var trustPwd: String? = null
    var keyPwd: String? = null
    var userName: String? = "nyilvantarto"
    var password: String? = "nyilvantarto"
    var vhost: String? = "nyilvantarto"

    fun key (value: String): RabbitMqOptions {
        key = value
        return this
    }
    fun trust (value: String): RabbitMqOptions {
        trust = value
        return this
    }
    fun trustPwd (value: String): RabbitMqOptions {
        trustPwd = value
        return this
    }
    fun keyPwd (value: String): RabbitMqOptions {
        keyPwd = value
        return this
    }

    fun isValid () : Boolean {
        if (key != null && key == "" || keyPwd != "") {
            return false
        }
        if (trust != null && trust == "" || trustPwd != "") {
            return false
        }
        return true
    }

    fun getSSLContext () : SSLContext? {
        if (key == null || keyPwd == null || trust == null || trustPwd == null) {
            return null
        }

        val keyStore = KeyStore.getInstance("PKCS12")
        keyStore.load(FileInputStream(key!!), keyPwd!!.toCharArray())

        val keyMngF = KeyManagerFactory.getInstance("SunX509")
        keyMngF.init(keyStore, keyPwd!!.toCharArray())

        val trustStore = KeyStore.getInstance("JKS")
        trustStore.load(FileInputStream(trust!!), trustPwd!!.toCharArray())

        val tmf = TrustManagerFactory.getInstance("SunX509");
        tmf.init(trustStore)

        val context = SSLContext.getInstance("TLSv1.2")
        context.init(keyMngF.keyManagers, tmf.trustManagers, null)

        return context
    }
}
