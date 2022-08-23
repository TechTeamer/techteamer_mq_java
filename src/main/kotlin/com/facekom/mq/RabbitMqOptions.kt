package com.facekom.mq

import java.io.FileInputStream
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager


class RabbitMqOptions {
    var key: String? = null
    var trust: String? = null
    var trustPwd: String? = null
    var keyPwd: String? = null
    var userName: String? = null
    var password: String? = null
    var vhost: String? = null
    var allowTlsWithoutTrustStore: Boolean = false

    fun key(value: String): RabbitMqOptions {
        key = value
        return this
    }

    fun trust(value: String): RabbitMqOptions {
        trust = value
        return this
    }

    fun trustPwd(value: String): RabbitMqOptions {
        trustPwd = value
        return this
    }

    fun keyPwd(value: String): RabbitMqOptions {
        keyPwd = value
        return this
    }

    fun userName(value: String): RabbitMqOptions {
        userName = value
        return this
    }

    fun password(value: String): RabbitMqOptions {
        password = value
        return this
    }
    fun vhost(value: String): RabbitMqOptions {
        vhost = value
        return this
    }

    fun isValid(): Boolean {
        if (key != null && key == "" || keyPwd != "") {
            return false
        }
        if (trust != null && trust == "" || trustPwd != "") {
            return false
        }
        return true
    }

    fun allowTlsWithoutTrustStore(value: Boolean): RabbitMqOptions {
        allowTlsWithoutTrustStore = value
        return this
    }

    fun getSSLContext(): SSLContext? {
        val context = SSLContext.getInstance("TLSv1.3")
        var keyMngF: KeyManagerFactory? = null
        var tmf: TrustManagerFactory? = null

        if (key != null && keyPwd != null) {
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore!!.load(FileInputStream(key!!), keyPwd!!.toCharArray())

            keyMngF = KeyManagerFactory.getInstance("SunX509")
            keyMngF!!.init(keyStore, keyPwd!!.toCharArray())
        }

        val trustStore = KeyStore.getInstance("JKS")

        if (trust != null && trustPwd != null) {
            trustStore.load(FileInputStream(trust!!), trustPwd!!.toCharArray())
            tmf = TrustManagerFactory.getInstance("SunX509");
            tmf!!.init(trustStore)
        }

        val keyManagersOrNull = keyMngF?.keyManagers
        val trustManagersOrTrustAll =
            tmf?.trustManagers ?: if (allowTlsWithoutTrustStore) {
                arrayOf(TrustAllX509TrustManager.INSTANCE)
            } else {
                throw Exception("Trustmanager is not provided and allowTlsWithoutTrustStore is set to false")
            }

        context.init(keyManagersOrNull, trustManagersOrTrustAll, null)

        return context
    }
}

class TrustAllX509TrustManager : X509TrustManager {
    @Throws(CertificateException::class)
    override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {
        // trust all
    }

    @Throws(CertificateException::class)
    override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {
        // trust all
    }

    override fun getAcceptedIssuers(): Array<X509Certificate?> {
        return arrayOfNulls<X509Certificate>(0)
    }

    companion object {
        val INSTANCE: X509TrustManager = TrustAllX509TrustManager()
    }
}