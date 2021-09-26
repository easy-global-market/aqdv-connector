package io.egm.aqdv.config

import io.smallrye.config.ConfigMapping

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

        fun knownTimeseries(): Set<String>

        fun targetProperty(): String
    }

    interface Cron {
        fun timeSeriesData(): String
    }

    interface ContextBroker {
        fun context(): String

        fun url(): String
    }

    interface AuthServer {
        fun url(): String

        fun clientId(): String

        fun clientSecret(): String

        fun grantType(): String
    }
}
