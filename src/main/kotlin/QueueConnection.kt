import com.rabbitmq.client.Channel
import com.rabbitmq.client.Connection
import com.rabbitmq.client.ConnectionFactory
import com.rabbitmq.client.SslContextFactory
import java.io.FileInputStream
import java.security.KeyStore
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory

class QueueConnection constructor(config: QueueConfig) {

    private var factory = ConnectionFactory()

    private var connection: Connection
    open var myChannel: Channel? = null
    var logger = config.logger

    init {
        if (config.options != null) {
            val keyStore = KeyStore.getInstance("PKCS12")
            keyStore.load(FileInputStream(config.options!!.key), null)

            val keyMngF = KeyManagerFactory.getInstance("SunX509")
            keyMngF.init(keyStore, null)

            val trustStore = KeyStore.getInstance("JKS")
            trustStore.load(FileInputStream(config.options!!.trust), null)

            val tmf = TrustManagerFactory.getInstance("SunX509");
            tmf.init(trustStore)

            val context = SSLContext.getInstance("TLSv1.2")
            context.init(keyMngF.keyManagers, tmf.trustManagers, null);

            factory.useSslProtocol(context)
        }

        connection = factory.newConnection(config.url)
    }

    fun getChannel(): Channel {
        if (myChannel != null) return myChannel as Channel
        myChannel = connection.createChannel()
        return myChannel as Channel
    }

}