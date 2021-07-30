package io.egm.aqdv.utils

fun String.aqdvNameToNgsiLdProperty(): String =
    this
        .replace("-", "")
        .replace("'", "")
        .split(" ")
        .joinToString("", transform = String::capitalize)
        .decapitalize()
