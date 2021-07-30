package io.egm.aqdv.model

sealed class ApplicationException {
    data class ContextBrokerException(val message: String): ApplicationException()
    data class AqdvException(val message: String): ApplicationException()
}
