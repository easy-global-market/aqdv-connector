package io.egm.aqdv.config

import io.smallrye.config.ConfigMapping
import java.net.URI

@ConfigMapping(prefix = "application")
interface ApplicationProperties {

    fun aqdv(): Aqdv

    fun contextBroker(): ContextBroker

    fun authServer(): AuthServer

    interface Aqdv {
        fun url(): String
    }

    interface ContextBroker {
        fun context(): String

        fun url(): String

        fun entityId(): URI
    }

    interface AuthServer {
        fun url(): String

        fun clientId(): String

        fun clientSecret(): String

        fun grantType(): String
    }
}
