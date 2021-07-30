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

        // even if not used from this config interface (used by the scheduler config)
        // the app won't start if they are not defined
        fun cron(): Cron
    }

    interface Cron {
        fun timeSeries(): String

        fun timeSeriesData(): String
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
